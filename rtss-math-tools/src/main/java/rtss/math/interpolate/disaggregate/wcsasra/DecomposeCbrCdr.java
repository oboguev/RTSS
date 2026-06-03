package rtss.math.interpolate.disaggregate.wcsasra;

import java.util.Arrays;

import rtss.data.bin.Bin;

/*
 * Weighted CSASRA decomposition for paired CBR/CDR series.
 *
 * Input:
 *   cbrs[k].avg = aggregate period CBR for bin k, per 1000
 *   cdrs[k].avg = aggregate period CDR for bin k, per 1000
 *
 * Output:
 *   cbr[t], cdr[t] = restored annual CBR/CDR values, per 1000
 *
 * The weights are not supplied externally.  They are recomputed from the
 * restored annual natural increase:
 *
 *   n[t] = (cbr[t] - cdr[t]) / 1000
 *
 * assuming negligible migration and arbitrary initial population P0 = 1.
 * 
 * **********************************************************************
 * 
 * Для пятилеток брать sigma в диапазоне 0.3 - 0.5 - 1.25 - 1.5.
 * 
 */
public class DecomposeCbrCdr
{
    public double[] cbr;
    public double[] cdr;

    public int maxOuterIterations = 200;
    public int maxInnerIterations = 2000;
    public int maxFinalProjectionIterations = 50;

    public double smoothingSigma = 1.25;

    public double minRate = 1e-12;

    public double maxAbsConvergenceDifference = 1e-10;
    public double maxRelConvergenceDifference = 1e-10;

    public double maxConstraintRelativeError = 1e-10;

    public boolean throwOnNonConvergence = true;

    public DecomposeCbrCdr()
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

        double[] ycbr = reconstructWithFixedWeights(cbrAggregated, widths, weights, null);
        double[] ycdr = reconstructWithFixedWeights(cdrAggregated, widths, weights, null);

        boolean converged = false;

        for (int iteration = 0; iteration < maxOuterIterations; iteration++)
        {
            weights = exposureWeights(ycbr, ycdr);

            double[] ncbr = reconstructWithFixedWeights(cbrAggregated, widths, weights, ycbr);
            double[] ncdr = reconstructWithFixedWeights(cdrAggregated, widths, weights, ycdr);

            double[] checkWeights = exposureWeights(ncbr, ncdr);
            double err = Math.max(
                                  maxConstraintRelativeError(ncbr, cbrAggregated, widths, checkWeights),
                                  maxConstraintRelativeError(ncdr, cdrAggregated, widths, checkWeights));

            boolean valuesConverged = converged(ncbr, ycbr) &&
                                      converged(ncdr, ycdr);

            ycbr = ncbr;
            ycdr = ncdr;

            if (valuesConverged && err <= maxConstraintRelativeError)
            {
                converged = true;
                break;
            }
        }

        /*
         * Final projection without additional smoothing.
         *
         * This brings the final arrays as close as possible to exact
         * self-consistency with the weights implied by the final CBR/CDR.
         */
        boolean projected = projectToSelfConsistentWeights(
                                                           ycbr, ycdr,
                                                           cbrAggregated, cdrAggregated,
                                                           widths);

        if (!converged && !projected && throwOnNonConvergence)
            throw new IllegalStateException("Weighted CBR/CDR decomposition failed to converge");

        this.cbr = ycbr;
        this.cdr = ycdr;
    }

    private double[] reconstructWithFixedWeights(
            double[] aggregated,
            int[] widths,
            double[] weights,
            double[] initial)
    {
        int totalYears = total(widths);

        double[] y;

        if (initial != null)
        {
            if (initial.length != totalYears)
                throw new IllegalArgumentException("Invalid initial array length");
            y = initial.clone();
        }
        else
        {
            y = expandAggregated(aggregated, widths);
        }

        enforceWeightedMeans(y, aggregated, widths, weights);

        double[] kernel = gaussianKernel(smoothingSigma);
        double[] prev = y.clone();

        boolean converged = false;

        for (int iteration = 0; iteration < maxInnerIterations; iteration++)
        {
            double[] smoothed = gaussianFilter(y, kernel);

            enforceMinimum(smoothed);
            enforceWeightedMeans(smoothed, aggregated, widths, weights);

            y = smoothed;

            if (iteration > 0 && converged(y, prev))
            {
                converged = true;
                break;
            }

            prev = y.clone();
        }

        enforceWeightedMeans(y, aggregated, widths, weights);

        if (!converged && throwOnNonConvergence)
            throw new IllegalStateException("Weighted CSASRA inner iteration failed to converge");

        return y;
    }

    private boolean projectToSelfConsistentWeights(
            double[] ycbr,
            double[] ycdr,
            double[] cbrAggregated,
            double[] cdrAggregated,
            int[] widths)
    {
        boolean converged = false;

        for (int iteration = 0; iteration < maxFinalProjectionIterations; iteration++)
        {
            double[] prevCbr = ycbr.clone();
            double[] prevCdr = ycdr.clone();

            double[] weights = exposureWeights(ycbr, ycdr);

            enforceWeightedMeans(ycbr, cbrAggregated, widths, weights);
            enforceWeightedMeans(ycdr, cdrAggregated, widths, weights);

            double[] checkWeights = exposureWeights(ycbr, ycdr);

            double err = Math.max(
                                  maxConstraintRelativeError(ycbr, cbrAggregated, widths, checkWeights),
                                  maxConstraintRelativeError(ycdr, cdrAggregated, widths, checkWeights));

            if (converged(ycbr, prevCbr) &&
                converged(ycdr, prevCdr) &&
                err <= maxConstraintRelativeError)
            {
                converged = true;
                break;
            }
        }

        return converged;
    }

    /*
     * Construct relative annual exposure weights.
     *
     * P[0] is arbitrary and set to 1.
     *
     * For annual natural increase n:
     *
     *   P[t+1] = P[t] * exp(n)
     *
     * and exposure during the year is:
     *
     *   w[t] = integral from 0 to 1 of P[t] * exp(n * u) du
     *        = P[t] * (exp(n) - 1) / n
     *
     * with the limiting value w[t] = P[t] for n = 0.
     */
    private double[] exposureWeights(double[] cbr, double[] cdr)
    {
        if (cbr.length != cdr.length)
            throw new IllegalArgumentException("CBR/CDR length mismatch");

        double[] w = new double[cbr.length];

        double p = 1.0;

        for (int t = 0; t < cbr.length; t++)
        {
            double n = (cbr[t] - cdr[t]) / 1000.0;

            double exposureFactor;
            if (Math.abs(n) < 1e-8)
            {
                /*
                 * expm1(n) / n =
                 *   1 + n/2 + n^2/6 + n^3/24 + ...
                 */
                exposureFactor = 1.0 + n / 2.0 + n * n / 6.0 + n * n * n / 24.0;
            }
            else
            {
                exposureFactor = Math.expm1(n) / n;
            }

            w[t] = p * exposureFactor;
            p *= Math.exp(n);
        }

        normalizeMeanToOne(w);
        return w;
    }

    private void enforceWeightedMeans(
            double[] y,
            double[] aggregated,
            int[] widths,
            double[] weights)
    {
        int ix = 0;

        for (int k = 0; k < aggregated.length; k++)
        {
            int start = ix;
            int end = ix + widths[k];

            double target = aggregated[k];

            if (target == 0.0)
            {
                Arrays.fill(y, start, end, 0.0);
                ix = end;
                continue;
            }

            double current = weightedMean(y, weights, start, end);

            if (!(current > 0.0) || Double.isNaN(current) || Double.isInfinite(current))
                throw new IllegalStateException("Invalid weighted mean while enforcing constraints");

            double factor = target / current;

            for (int i = start; i < end; i++)
                y[i] *= factor;

            ix = end;
        }
    }

    private double weightedMean(double[] y, double[] weights, int start, int end)
    {
        double sw = 0.0;
        double sy = 0.0;

        for (int i = start; i < end; i++)
        {
            sw += weights[i];
            sy += weights[i] * y[i];
        }

        if (!(sw > 0.0))
            throw new IllegalStateException("Non-positive weight sum");

        return sy / sw;
    }

    private double maxConstraintRelativeError(
            double[] y,
            double[] aggregated,
            int[] widths,
            double[] weights)
    {
        double max = 0.0;
        int ix = 0;

        for (int k = 0; k < aggregated.length; k++)
        {
            int start = ix;
            int end = ix + widths[k];

            double target = aggregated[k];
            double actual = weightedMean(y, weights, start, end);

            double base = Math.max(Math.abs(target), 1e-12);
            double err = Math.abs(actual - target) / base;

            if (err > max)
                max = err;

            ix = end;
        }

        return max;
    }

    private double[] gaussianFilter(double[] data, double[] kernel)
    {
        if (kernel.length == 1)
            return data.clone();

        int n = data.length;
        int radius = kernel.length / 2;

        double[] smoothed = new double[n];

        for (int i = 0; i < n; i++)
        {
            double sum = 0.0;
            double weightSum = 0.0;

            for (int j = -radius; j <= radius; j++)
            {
                int ix = i + j;

                if (ix >= 0 && ix < n)
                {
                    double kw = kernel[j + radius];
                    sum += data[ix] * kw;
                    weightSum += kw;
                }
            }

            smoothed[i] = sum / weightSum;
        }

        return smoothed;
    }

    private double[] gaussianKernel(double sigma)
    {
        if (!(sigma > 0.0))
            return new double[] { 1.0 };

        int size = (int) Math.ceil(6.0 * sigma);

        if (size < 3)
            size = 3;

        if (size % 2 == 0)
            size++;

        double[] kernel = new double[size];

        int radius = size / 2;
        double sum = 0.0;

        for (int i = -radius; i <= radius; i++)
        {
            double v = Math.exp(-(i * i) / (2.0 * sigma * sigma));
            kernel[i + radius] = v;
            sum += v;
        }

        for (int i = 0; i < kernel.length; i++)
            kernel[i] /= sum;

        return kernel;
    }

    private void enforceMinimum(double[] y)
    {
        for (int i = 0; i < y.length; i++)
        {
            if (y[i] < minRate)
                y[i] = minRate;
        }
    }

    private boolean converged(double[] a, double[] b)
    {
        return maxAbsDifferenceIsLess(a, b, maxAbsConvergenceDifference) ||
               maxRelDifferenceIsLess(a, b, maxRelConvergenceDifference);
    }

    private boolean maxAbsDifferenceIsLess(double[] a, double[] b, double limit)
    {
        if (a.length != b.length)
            throw new IllegalArgumentException("Array length mismatch");

        for (int i = 0; i < a.length; i++)
        {
            if (Math.abs(a[i] - b[i]) > limit)
                return false;
        }

        return true;
    }

    private boolean maxRelDifferenceIsLess(double[] a, double[] b, double limit)
    {
        if (a.length != b.length)
            throw new IllegalArgumentException("Array length mismatch");

        final double precisionThreshold = 1e-12;

        for (int i = 0; i < a.length; i++)
        {
            double diff = Math.abs(a[i] - b[i]);
            double base = Math.max(Math.abs(a[i]), Math.abs(b[i]));

            if (base < precisionThreshold)
            {
                if (diff > precisionThreshold)
                    return false;
            }
            else
            {
                if (diff / base > limit)
                    return false;
            }
        }

        return true;
    }

    private double[] expandAggregated(double[] aggregated, int[] widths)
    {
        double[] y = new double[total(widths)];

        int ix = 0;

        for (int k = 0; k < aggregated.length; k++)
        {
            Arrays.fill(y, ix, ix + widths[k], Math.max(aggregated[k], minRate));
            ix += widths[k];
        }

        return y;
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
            w[i] = bins[i].age_x2 - bins[i].age_x1 + 1;

        return w;
    }

    private int total(int[] widths)
    {
        int n = 0;

        for (int w : widths)
            n += w;

        return n;
    }

    private void normalizeMeanToOne(double[] w)
    {
        double sum = 0.0;

        for (double v : w)
            sum += v;

        double mean = sum / w.length;

        if (!(mean > 0.0) || Double.isNaN(mean) || Double.isInfinite(mean))
            throw new IllegalStateException("Invalid mean weight");

        for (int i = 0; i < w.length; i++)
            w[i] /= mean;
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
                throw new IllegalArgumentException("Null bin");

            if (b.age_x1 != d.age_x1 || b.age_x2 != d.age_x2)
                throw new IllegalArgumentException("CBR/CDR bin layout mismatch at index " + i);

            if (b.age_x2 < b.age_x1)
                throw new IllegalArgumentException("Invalid bin width at index " + i);

            if (i > 0)
            {
                if (cbrs[i - 1].age_x2 + 1 != b.age_x1)
                    throw new IllegalArgumentException("CBR bins are not continuous at index " + i);

                if (cdrs[i - 1].age_x2 + 1 != d.age_x1)
                    throw new IllegalArgumentException("CDR bins are not continuous at index " + i);
            }

            if (!(b.avg >= 0.0) || Double.isNaN(b.avg) || Double.isInfinite(b.avg))
                throw new IllegalArgumentException("Invalid CBR value at index " + i);

            if (!(d.avg >= 0.0) || Double.isNaN(d.avg) || Double.isInfinite(d.avg))
                throw new IllegalArgumentException("Invalid CDR value at index " + i);
        }
    }
}