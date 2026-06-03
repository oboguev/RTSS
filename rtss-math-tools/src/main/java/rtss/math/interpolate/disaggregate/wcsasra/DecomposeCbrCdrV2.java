package rtss.math.interpolate.disaggregate.wcsasra;

import java.util.Arrays;

import rtss.data.bin.Bin;

/**
 * Decompose aggregate weighted-period CBR/CDR values into annual values.
 *
 * This version does not use Gaussian smoothing.  For fixed exposure weights it
 * solves the constrained least-squares problem:
 *
 *     minimize    sum_t (x[t] - 2*x[t+1] + x[t+2])^2
 *
 *     subject to  weightedMean(x over each input bin) = bin.avg
 *
 * CBR and CDR are solved separately, but with the same exposure weights.
 *
 * The exposure weights are not supplied externally.  They are reconstructed
 * endogenously from the current annual CBR/CDR path under the assumption of
 * negligible migration:
 *
 *     n[t] = (CBR[t] - CDR[t]) / 1000
 *     P[t+1] = P[t] * exp(n[t])
 *
 * Since only relative weights matter, P[0] is arbitrary.
 */
public class DecomposeCbrCdrV2
{
    public double[] cbr;
    public double[] cdr;

    public int maxOuterIterations = 500;

    public double maxAbsConvergenceDifference = 1e-10;
    public double maxRelConvergenceDifference = 1e-10;

    public double maxConstraintRelativeError = 1e-10;

    /*
     * Tiny ridge term used only to make the KKT system numerically safer.
     *
     * The solved objective is effectively:
     *
     *     0.5 * sum secondDifference^2
     *   + 0.5 * ridge * sum (x - anchor)^2
     *
     * The constraints are still exact up to floating point accuracy.
     */
    public double ridge = 1e-10;

    /*
     * If the unconstrained-smoothest solution produces negative annual rates,
     * this usually indicates either very irregular input data or that a known
     * shock/breakpoint should be handled explicitly.
     */
    public boolean failOnNegativeRates = true;
    public double negativeRateTolerance = 1e-8;

    public boolean throwOnNonConvergence = true;

    public DecomposeCbrCdrV2()
    {
    }

    public void decompose(Bin[] cbrs, Bin[] cdrs)
    {
        validateInput(cbrs, cdrs);

        int[] widths = widths(cbrs);
        double[] cbrAggregated = averages(cbrs);
        double[] cdrAggregated = averages(cdrs);

        int totalYears = total(widths);

        double[] weights = new double[totalYears];
        Arrays.fill(weights, 1.0);

        double[] ycbr = solveSmoothestConstrained(cbrAggregated,
                                                  widths,
                                                  weights,
                                                  expandAggregated(cbrAggregated, widths));

        double[] ycdr = solveSmoothestConstrained(cdrAggregated,
                                                  widths,
                                                  weights,
                                                  expandAggregated(cdrAggregated, widths));

        checkRates(ycbr, "CBR");
        checkRates(ycdr, "CDR");

        boolean converged = false;

        for (int iteration = 0; iteration < maxOuterIterations; iteration++)
        {
            double[] prevCbr = ycbr.clone();
            double[] prevCdr = ycdr.clone();

            weights = exposureWeights(ycbr, ycdr);

            ycbr = solveSmoothestConstrained(cbrAggregated, widths, weights, prevCbr);
            ycdr = solveSmoothestConstrained(cdrAggregated, widths, weights, prevCdr);

            checkRates(ycbr, "CBR");
            checkRates(ycdr, "CDR");

            /*
             * The solve above preserved weighted means for the weights computed
             * from the previous CBR/CDR.  For self-consistency, check again with
             * the weights implied by the newly solved CBR/CDR.
             */
            double[] checkWeights = exposureWeights(ycbr, ycdr);

            double cbrConstraintError = maxConstraintRelativeError(ycbr, cbrAggregated, widths, checkWeights);

            double cdrConstraintError = maxConstraintRelativeError(ycdr, cdrAggregated, widths, checkWeights);

            boolean valuesConverged = converged(ycbr, prevCbr) &&
                                      converged(ycdr, prevCdr);

            boolean constraintsConverged = cbrConstraintError <= maxConstraintRelativeError &&
                                           cdrConstraintError <= maxConstraintRelativeError;

            if (valuesConverged && constraintsConverged)
            {
                converged = true;
                break;
            }
        }

        if (!converged && throwOnNonConvergence)
            throw new IllegalStateException("DecomposeCbrCdrV2 failed to converge");

        this.cbr = ycbr;
        this.cdr = ycdr;
    }

    private double[] solveSmoothestConstrained(
            double[] targets,
            int[] widths,
            double[] weights,
            double[] anchor)
    {
        int n = total(widths);
        int m = targets.length;

        if (weights.length != n)
            throw new IllegalArgumentException("Invalid weights length");

        if (anchor == null)
            anchor = expandAggregated(targets, widths);

        if (anchor.length != n)
            throw new IllegalArgumentException("Invalid anchor length");

        int dim = n + m;

        double[][] a = new double[dim][dim];
        double[] rhs = new double[dim];

        /*
         * Objective matrix for:
         *
         *     sum_i (x[i] - 2*x[i+1] + x[i+2])^2
         *
         * This adds D' * D to the upper-left block.
         */
        for (int i = 0; i <= n - 3; i++)
        {
            int i0 = i;
            int i1 = i + 1;
            int i2 = i + 2;

            addOuterProduct(a, new int[] { i0, i1, i2 }, new double[] { 1.0, -2.0, 1.0 });
        }

        /*
         * Tiny anchoring ridge:
         *
         *     0.5 * ridge * sum_i (x[i] - anchor[i])^2
         *
         * This is mainly for numerical stability.  With the default ridge it
         * should not visibly affect normal demographic series.
         */
        if (ridge > 0.0)
        {
            for (int i = 0; i < n; i++)
            {
                a[i][i] += ridge;
                rhs[i] += ridge * anchor[i];
            }
        }

        /*
         * Constraints:
         *
         *     sum_i normalizedWeight[i] * x[i] = target[k]
         *
         * for each bin k.
         *
         * We use normalized weights inside each bin, so the right-hand side is
         * simply the target weighted mean.
         */
        int ix = 0;

        for (int k = 0; k < m; k++)
        {
            int start = ix;
            int end = ix + widths[k];

            double sw = 0.0;
            for (int i = start; i < end; i++)
                sw += weights[i];

            if (!(sw > 0.0) || Double.isNaN(sw) || Double.isInfinite(sw))
                throw new IllegalStateException("Invalid weight sum in bin " + k);

            int constraintRow = n + k;

            for (int i = start; i < end; i++)
            {
                double aw = weights[i] / sw;

                a[i][constraintRow] = aw;
                a[constraintRow][i] = aw;
            }

            rhs[constraintRow] = targets[k];

            ix = end;
        }

        double[] solution = solveLinearSystem(a, rhs);

        double[] x = new double[n];
        System.arraycopy(solution, 0, x, 0, n);

        double err = maxConstraintRelativeError(x, targets, widths, weights);
        if (err > Math.max(1e-8, 1000.0 * maxConstraintRelativeError))
            throw new IllegalStateException("Constraint solve error is too large: " + err);

        return x;
    }

    private void addOuterProduct(double[][] a, int[] indexes, double[] coeffs)
    {
        for (int p = 0; p < indexes.length; p++)
        {
            int ip = indexes[p];
            double cp = coeffs[p];

            for (int q = 0; q < indexes.length; q++)
            {
                int iq = indexes[q];
                double cq = coeffs[q];

                a[ip][iq] += cp * cq;
            }
        }
    }

    /*
     * Dense Gaussian elimination with partial pivoting.
     *
     * The matrix here is a KKT matrix, symmetric but indefinite.  Plain partial
     * pivoting is adequate for the small systems expected here: usually tens or
     * hundreds of annual points, not tens of thousands.
     */
    private double[] solveLinearSystem(double[][] a, double[] b)
    {
        int n = b.length;

        if (a.length != n)
            throw new IllegalArgumentException("Matrix/vector size mismatch");

        for (int i = 0; i < n; i++)
        {
            if (a[i].length != n)
                throw new IllegalArgumentException("Matrix is not square");
        }

        double[][] m = new double[n][n];
        double[] rhs = b.clone();

        for (int i = 0; i < n; i++)
            m[i] = a[i].clone();

        for (int col = 0; col < n; col++)
        {
            int pivotRow = col;
            double pivotAbs = Math.abs(m[col][col]);

            for (int row = col + 1; row < n; row++)
            {
                double v = Math.abs(m[row][col]);
                if (v > pivotAbs)
                {
                    pivotAbs = v;
                    pivotRow = row;
                }
            }

            if (!(pivotAbs > 0.0) || Double.isNaN(pivotAbs) || pivotAbs < 1e-18)
                throw new IllegalStateException("Singular or ill-conditioned linear system");

            if (pivotRow != col)
            {
                double[] tmpRow = m[col];
                m[col] = m[pivotRow];
                m[pivotRow] = tmpRow;

                double tmp = rhs[col];
                rhs[col] = rhs[pivotRow];
                rhs[pivotRow] = tmp;
            }

            double pivot = m[col][col];

            for (int row = col + 1; row < n; row++)
            {
                double factor = m[row][col] / pivot;

                if (factor == 0.0)
                    continue;

                m[row][col] = 0.0;

                for (int j = col + 1; j < n; j++)
                    m[row][j] -= factor * m[col][j];

                rhs[row] -= factor * rhs[col];
            }
        }

        double[] x = new double[n];

        for (int row = n - 1; row >= 0; row--)
        {
            double s = rhs[row];

            for (int j = row + 1; j < n; j++)
                s -= m[row][j] * x[j];

            x[row] = s / m[row][row];
        }

        return x;
    }

    /*
     * Construct relative annual exposure weights from current CBR/CDR.
     *
     * CBR/CDR are per 1000.  Natural increase n is therefore:
     *
     *     n = (CBR - CDR) / 1000
     *
     * With continuous exponential growth during the year:
     *
     *     P(u) = P0 * exp(n*u), 0 <= u <= 1
     *
     * exposure is:
     *
     *     integral_0^1 P0 * exp(n*u) du
     *       = P0 * (exp(n) - 1) / n
     *
     * Only relative weights matter.  The final weights are normalized to mean 1.
     */
    private double[] exposureWeights(double[] cbr, double[] cdr)
    {
        if (cbr.length != cdr.length)
            throw new IllegalArgumentException("CBR/CDR length mismatch");

        int nYears = cbr.length;

        double[] logWeights = new double[nYears];

        double logP = 0.0;
        double maxLogWeight = Double.NEGATIVE_INFINITY;

        for (int t = 0; t < nYears; t++)
        {
            double naturalIncrease = (cbr[t] - cdr[t]) / 1000.0;

            double logExposureFactor = logExposureFactor(naturalIncrease);

            logWeights[t] = logP + logExposureFactor;

            if (logWeights[t] > maxLogWeight)
                maxLogWeight = logWeights[t];

            logP += naturalIncrease;
        }

        double[] weights = new double[nYears];

        for (int t = 0; t < nYears; t++)
            weights[t] = Math.exp(logWeights[t] - maxLogWeight);

        normalizeMeanToOne(weights);

        return weights;
    }

    private double logExposureFactor(double n)
    {
        if (Math.abs(n) < 1e-7)
        {
            /*
             * expm1(n)/n = 1 + n/2 + n^2/6 + n^3/24 + ...
             */
            double f = 1.0 +
                       n / 2.0 +
                       n * n / 6.0 +
                       n * n * n / 24.0 +
                       n * n * n * n / 120.0;

            return Math.log(f);
        }

        double f = Math.expm1(n) / n;

        if (!(f > 0.0) || Double.isNaN(f) || Double.isInfinite(f))
            throw new IllegalStateException("Invalid exposure factor");

        return Math.log(f);
    }

    private double maxConstraintRelativeError(
            double[] x,
            double[] targets,
            int[] widths,
            double[] weights)
    {
        double max = 0.0;

        int ix = 0;

        for (int k = 0; k < targets.length; k++)
        {
            int start = ix;
            int end = ix + widths[k];

            double actual = weightedMean(x, weights, start, end);
            double target = targets[k];

            double base = Math.max(Math.abs(target), 1e-12);
            double err = Math.abs(actual - target) / base;

            if (err > max)
                max = err;

            ix = end;
        }

        return max;
    }

    private double weightedMean(double[] x, double[] weights, int start, int end)
    {
        double sx = 0.0;
        double sw = 0.0;

        for (int i = start; i < end; i++)
        {
            sx += weights[i] * x[i];
            sw += weights[i];
        }

        if (!(sw > 0.0) || Double.isNaN(sw) || Double.isInfinite(sw))
            throw new IllegalStateException("Invalid weight sum");

        return sx / sw;
    }

    private boolean converged(double[] a, double[] b)
    {
        if (a.length != b.length)
            throw new IllegalArgumentException("Array length mismatch");

        return maxAbsDifference(a, b) <= maxAbsConvergenceDifference ||
               maxRelDifference(a, b) <= maxRelConvergenceDifference;
    }

    private double maxAbsDifference(double[] a, double[] b)
    {
        double max = 0.0;

        for (int i = 0; i < a.length; i++)
        {
            double d = Math.abs(a[i] - b[i]);
            if (d > max)
                max = d;
        }

        return max;
    }

    private double maxRelDifference(double[] a, double[] b)
    {
        double max = 0.0;

        for (int i = 0; i < a.length; i++)
        {
            double d = Math.abs(a[i] - b[i]);
            double base = Math.max(Math.abs(a[i]), Math.abs(b[i]));

            double rel;

            if (base < 1e-12)
                rel = d;
            else
                rel = d / base;

            if (rel > max)
                max = rel;
        }

        return max;
    }

    private void checkRates(double[] x, String name)
    {
        if (!failOnNegativeRates)
            return;

        for (int i = 0; i < x.length; i++)
        {
            if (x[i] < -negativeRateTolerance)
                throw new IllegalStateException(name + " has negative value at annual index " + i + ": " + x[i]);
        }
    }

    private double[] expandAggregated(double[] aggregated, int[] widths)
    {
        double[] x = new double[total(widths)];

        int ix = 0;

        for (int k = 0; k < aggregated.length; k++)
        {
            Arrays.fill(x, ix, ix + widths[k], aggregated[k]);
            ix += widths[k];
        }

        return x;
    }

    private double[] averages(Bin[] bins)
    {
        double[] a = new double[bins.length];

        for (int i = 0; i < bins.length; i++)
            a[i] = bins[i].avg;

        return a;
    }

    private int[] widths(Bin[] bins)
    {
        int[] w = new int[bins.length];

        for (int i = 0; i < bins.length; i++)
        {
            if (bins[i].widths_in_years > 0)
                w[i] = bins[i].widths_in_years;
            else
                w[i] = bins[i].age_x2 - bins[i].age_x1 + 1;
        }

        return w;
    }

    private int total(int[] widths)
    {
        int n = 0;

        for (int w : widths)
            n += w;

        return n;
    }

    private void normalizeMeanToOne(double[] weights)
    {
        double sum = 0.0;

        for (double w : weights)
            sum += w;

        double mean = sum / weights.length;

        if (!(mean > 0.0) || Double.isNaN(mean) || Double.isInfinite(mean))
            throw new IllegalStateException("Invalid mean weight");

        for (int i = 0; i < weights.length; i++)
            weights[i] /= mean;
    }

    private void validateInput(Bin[] cbrs, Bin[] cdrs)
    {
        if (cbrs == null || cdrs == null)
            throw new IllegalArgumentException("Null input");

        if (cbrs.length == 0)
            throw new IllegalArgumentException("Empty input");

        if (cbrs.length != cdrs.length)
            throw new IllegalArgumentException("CBR/CDR bin count mismatch");

        for (int i = 0; i < cbrs.length; i++)
        {
            Bin b = cbrs[i];
            Bin d = cdrs[i];

            if (b == null || d == null)
                throw new IllegalArgumentException("Null bin at index " + i);

            if (b.age_x1 != d.age_x1 || b.age_x2 != d.age_x2)
                throw new IllegalArgumentException("CBR/CDR bin layout mismatch at index " + i);

            int bw = b.age_x2 - b.age_x1 + 1;
            int dw = d.age_x2 - d.age_x1 + 1;

            if (bw <= 0 || dw <= 0)
                throw new IllegalArgumentException("Invalid bin width at index " + i);

            if (b.widths_in_years > 0 && b.widths_in_years != bw)
                throw new IllegalArgumentException("Inconsistent CBR bin width at index " + i);

            if (d.widths_in_years > 0 && d.widths_in_years != dw)
                throw new IllegalArgumentException("Inconsistent CDR bin width at index " + i);

            if (i > 0)
            {
                if (cbrs[i - 1].age_x2 + 1 != b.age_x1)
                    throw new IllegalArgumentException("CBR bins are not continuous at index " + i);

                if (cdrs[i - 1].age_x2 + 1 != d.age_x1)
                    throw new IllegalArgumentException("CDR bins are not continuous at index " + i);
            }

            if (Double.isNaN(b.avg) || Double.isInfinite(b.avg))
                throw new IllegalArgumentException("Invalid CBR value at index " + i);

            if (Double.isNaN(d.avg) || Double.isInfinite(d.avg))
                throw new IllegalArgumentException("Invalid CDR value at index " + i);

            if (b.avg < 0.0)
                throw new IllegalArgumentException("Negative aggregate CBR at index " + i);

            if (d.avg < 0.0)
                throw new IllegalArgumentException("Negative aggregate CDR at index " + i);
        }
    }
}