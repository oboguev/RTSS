package rtss.math.interpolate.disaggregate.wcsasra;

import java.util.Arrays;

import rtss.data.bin.Bin;

/**
 * Decompose aggregate weighted-period CBR/CDR values into annual values.
 *
 * V2M = V2 modified with "tension".
 *
 * For fixed exposure weights this solves:
 *
 *     minimize
 *
 *         secondDifferencePenalty * sum_t (x[t] - 2*x[t+1] + x[t+2])^2
 *       + firstDifferencePenalty  * sum_t (x[t+1] - x[t])^2
 *       + anchorPenalty           * sum_t (x[t] - anchor[t])^2
 *
 *     subject to:
 *
 *         weightedMean(x over each input bin) = bin.avg
 *
 * CBR and CDR are solved separately, but with common exposure weights.
 *
 * Exposure weights are reconstructed from current annual CBR/CDR under:
 *
 *     n[t] = (CBR[t] - CDR[t]) / 1000
 *     P[t+1] = P[t] * exp(n[t])
 *
 * Migration is assumed negligible.  Since only relative weights matter,
 * P[0] is arbitrary.
 */
public class DecomposeCbrCdrV2M
{
    public double[] cbr;
    public double[] cdr;

    public int maxOuterIterations = 500;

    public double maxAbsConvergenceDifference = 1e-10;
    public double maxRelConvergenceDifference = 1e-10;

    public double maxConstraintRelativeError = 1e-10;

    /*
     * Main smoothness penalty.
     *
     * Penalizes changes in slope.
     */
    public double secondDifferencePenalty = 1.0;

    /*
     * Tension penalty.
     *
     * Penalizes large year-to-year movements.  This is what suppresses
     * the wide overshooting waves that pure minimum-curvature V2 can produce.
     *
     * Suggested values to try:
     *
     *     0.05  weak tension
     *     0.10  default
     *     0.20  stronger
     *     0.50  quite strong
     */
    public double firstDifferencePenalty = 0.1;

    /*
     * Optional pull toward a fixed baseline curve.
     *
     * Default is zero.  If enabled, the anchor is a simple piecewise-linear
     * interpolation through bin-center aggregate values.
     */
    public double anchorPenalty = 0.0;

    /*
     * Tiny numerical ridge.  This is not intended as a substantive smoothing
     * parameter.  It only makes borderline KKT systems safer.
     */
    public double numericalRidge = 1e-12;

    public boolean failOnNegativeRates = true;
    public double negativeRateTolerance = 1e-8;

    public boolean throwOnNonConvergence = true;

    public DecomposeCbrCdrV2M()
    {
    }

    public void decompose(Bin[] cbrs, Bin[] cdrs)
    {
        validateInput(cbrs, cdrs);
        validateParameters();

        int[] widths = widths(cbrs);

        double[] cbrAggregated = averages(cbrs);
        double[] cdrAggregated = averages(cdrs);

        int totalYears = total(widths);

        double[] cbrAnchor = piecewiseLinearAnchor(cbrAggregated, widths);
        double[] cdrAnchor = piecewiseLinearAnchor(cdrAggregated, widths);

        double[] weights = new double[totalYears];
        Arrays.fill(weights, 1.0);

        double[] ycbr = solveSmoothestConstrained(cbrAggregated, widths, weights, cbrAnchor);
        double[] ycdr = solveSmoothestConstrained(cdrAggregated, widths, weights, cdrAnchor);

        checkRates(ycbr, "CBR");
        checkRates(ycdr, "CDR");

        boolean converged = false;

        for (int iteration = 0; iteration < maxOuterIterations; iteration++)
        {
            double[] prevCbr = ycbr.clone();
            double[] prevCdr = ycdr.clone();

            weights = exposureWeights(ycbr, ycdr);

            ycbr = solveSmoothestConstrained(cbrAggregated, widths, weights, cbrAnchor);
            ycdr = solveSmoothestConstrained(cdrAggregated, widths, weights, cdrAnchor);

            checkRates(ycbr, "CBR");
            checkRates(ycdr, "CDR");

            /*
             * The solve preserved constraints for weights computed from the
             * previous CBR/CDR.  Check constraints again with weights implied
             * by the newly solved paths.
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
            throw new IllegalStateException("DecomposeCbrCdrV2M failed to converge");

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
            anchor = piecewiseLinearAnchor(targets, widths);

        if (anchor.length != n)
            throw new IllegalArgumentException("Invalid anchor length");

        int dim = n + m;

        double[][] a = new double[dim][dim];
        double[] rhs = new double[dim];

        /*
         * Penalty for second differences:
         *
         *     x[i] - 2*x[i+1] + x[i+2]
         */
        if (secondDifferencePenalty != 0.0)
        {
            for (int i = 0; i <= n - 3; i++)
            {
                addOuterProduct(a,
                                new int[] { i, i + 1, i + 2 },
                                new double[] { 1.0, -2.0, 1.0 },
                                secondDifferencePenalty);
            }
        }

        /*
         * Tension penalty for first differences:
         *
         *     x[i+1] - x[i]
         */
        if (firstDifferencePenalty != 0.0)
        {
            for (int i = 0; i <= n - 2; i++)
            {
                addOuterProduct(a,
                                new int[] { i, i + 1 },
                                new double[] { -1.0, 1.0 },
                                firstDifferencePenalty);
            }
        }

        /*
         * Optional fixed-anchor penalty plus tiny numerical ridge.
         */
        double diagonalAnchorPenalty = anchorPenalty + numericalRidge;

        if (diagonalAnchorPenalty != 0.0)
        {
            for (int i = 0; i < n; i++)
            {
                a[i][i] += diagonalAnchorPenalty;
                rhs[i] += diagonalAnchorPenalty * anchor[i];
            }
        }

        /*
         * Equality constraints:
         *
         *     sum_i normalizedWeight[i] * x[i] = target[k]
         *
         * for each bin k.
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

    private void addOuterProduct(double[][] a, int[] indexes, double[] coeffs, double scale)
    {
        if (scale == 0.0)
            return;

        for (int p = 0; p < indexes.length; p++)
        {
            int ip = indexes[p];
            double cp = coeffs[p];

            for (int q = 0; q < indexes.length; q++)
            {
                int iq = indexes[q];
                double cq = coeffs[q];

                a[ip][iq] += scale * cp * cq;
            }
        }
    }

    /*
     * Dense Gaussian elimination with partial pivoting.
     *
     * The KKT matrix is symmetric but indefinite, so Cholesky is not suitable.
     * Plain partial pivoting is adequate for the small systems expected here.
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

            if (!(pivotAbs > 0.0) || Double.isNaN(pivotAbs) || pivotAbs < 1e-20)
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
             * expm1(n)/n =
             *     1 + n/2 + n^2/6 + n^3/24 + n^4/120 + ...
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

    /*
     * Fixed baseline curve used only when anchorPenalty or numericalRidge
     * is non-zero.
     *
     * The anchor passes through bin-center aggregate values.  It is not
     * required to satisfy bin-mean constraints; constraints are imposed exactly
     * by the KKT system.
     */
    private double[] piecewiseLinearAnchor(double[] aggregated, int[] widths)
    {
        int n = total(widths);
        int m = aggregated.length;

        double[] anchor = new double[n];

        if (m == 1)
        {
            Arrays.fill(anchor, aggregated[0]);
            return anchor;
        }

        double[] centers = new double[m];

        int ix = 0;

        for (int k = 0; k < m; k++)
        {
            centers[k] = ix + (widths[k] - 1) / 2.0;
            ix += widths[k];
        }

        for (int t = 0; t < n; t++)
        {
            int k;

            if (t <= centers[0])
            {
                k = 0;
            }
            else if (t >= centers[m - 1])
            {
                k = m - 2;
            }
            else
            {
                k = 0;

                while (!(centers[k] <= t && t <= centers[k + 1]))
                    k++;
            }

            double x0 = centers[k];
            double x1 = centers[k + 1];

            double y0 = aggregated[k];
            double y1 = aggregated[k + 1];

            double f = (t - x0) / (x1 - x0);

            anchor[t] = y0 + f * (y1 - y0);
        }

        return anchor;
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

    private void validateParameters()
    {
        if (maxOuterIterations <= 0)
            throw new IllegalArgumentException("maxOuterIterations must be positive");

        if (Double.isNaN(secondDifferencePenalty) || Double.isInfinite(secondDifferencePenalty) ||
            secondDifferencePenalty < 0.0)
            throw new IllegalArgumentException("Invalid secondDifferencePenalty");

        if (Double.isNaN(firstDifferencePenalty) || Double.isInfinite(firstDifferencePenalty) ||
            firstDifferencePenalty < 0.0)
            throw new IllegalArgumentException("Invalid firstDifferencePenalty");

        if (Double.isNaN(anchorPenalty) || Double.isInfinite(anchorPenalty) ||
            anchorPenalty < 0.0)
            throw new IllegalArgumentException("Invalid anchorPenalty");

        if (Double.isNaN(numericalRidge) || Double.isInfinite(numericalRidge) ||
            numericalRidge < 0.0)
            throw new IllegalArgumentException("Invalid numericalRidge");

        if (secondDifferencePenalty == 0.0 &&
            firstDifferencePenalty == 0.0 &&
            anchorPenalty == 0.0 &&
            numericalRidge == 0.0)
            throw new IllegalArgumentException("At least one penalty must be positive");

        if (!(maxAbsConvergenceDifference > 0.0) ||
            Double.isNaN(maxAbsConvergenceDifference) ||
            Double.isInfinite(maxAbsConvergenceDifference))
            throw new IllegalArgumentException("Invalid maxAbsConvergenceDifference");

        if (!(maxRelConvergenceDifference > 0.0) ||
            Double.isNaN(maxRelConvergenceDifference) ||
            Double.isInfinite(maxRelConvergenceDifference))
            throw new IllegalArgumentException("Invalid maxRelConvergenceDifference");

        if (!(maxConstraintRelativeError > 0.0) ||
            Double.isNaN(maxConstraintRelativeError) ||
            Double.isInfinite(maxConstraintRelativeError))
            throw new IllegalArgumentException("Invalid maxConstraintRelativeError");
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