package rtss.data.mortality.laws.tail;

import rtss.data.bin.Bin;

public class OldAgeTail
{
    private static final double OLD_AGE_EPS = 1e-12;

    /*
     * Replaces the curve inside one wide old-age bin by a model tail.
     *
     * The replacement preserves:
     *
     *     bin.avg = sum(exposure[x] * y[x]) / sum(exposure[x])
     *
     * over the selected bin.
     *
     * The code is intended for mx/rate-like quantities, not for qx unless qx
     * has deliberately been treated as a rate-like quantity in the surrounding code.
     */
    public static double[] applyOldAgeTailToBin(
            double[] curve,
            Bin[] bins,
            double[] exposure,
            int tailBinIndex,
            OldAgeTailModel model) throws Exception
    {
        if (curve == null)
            throw new IllegalArgumentException("curve is null");

        if (bins == null || bins.length == 0)
            throw new IllegalArgumentException("bins is null or empty");

        if (tailBinIndex < 0 || tailBinIndex >= bins.length)
            throw new IllegalArgumentException("tailBinIndex is out of range: " + tailBinIndex);

        if (exposure != null && exposure.length != curve.length)
            throw new IllegalArgumentException("exposure length does not match curve length: " +
                                               exposure.length + " vs " + curve.length);

        Bin tailBin = bins[tailBinIndex];

        int firstAge = bins[0].age_x1;
        int x1 = tailBin.age_x1 - firstAge;
        int x2 = tailBin.age_x2 - firstAge;

        if (x1 < 0 || x2 >= curve.length || x1 > x2)
            throw new IllegalArgumentException("tail bin does not match curve range");

        int width = x2 - x1 + 1;

        if (width <= 1)
            return curve;

        double targetAvg = tailBin.avg;

        if (!(targetAvg > 0) || !Double.isFinite(targetAvg))
            return curve;

        double[] out = curve.clone();

        /*
         * Anchor the model at the first age of the last bin.
         * I deliberately estimate this from the pre-tail curve, not from values
         * inside the wide last bin, because those are exactly the values we distrust.
         */
        double startValue = estimateTailStartValue(curve, x1, targetAvg);

        /*
         * Previous log-slope is used only by the Coale-Kisker-like model.
         */
        double previousLogSlope = estimatePreviousLogSlope(curve, x1, 5);

        switch (model)
        {
        case GOMPERTZ:
            fillGompertzTail(out, exposure, x1, x2, startValue, targetAvg);
            break;

        case KANNISTO:
            fillKannistoTail(out, exposure, x1, x2, startValue, targetAvg);
            break;

        case COALE_KISKER_LIKE:
            fillCoaleKiskerLikeTail(out, exposure, x1, x2, startValue, previousLogSlope, targetAvg);
            break;

        default:
            throw new IllegalArgumentException("Unsupported tail model: " + model);
        }

        return out;
    }

    /* ========================================== Gompertz tail =========================================== */

    private static void fillGompertzTail(
            double[] y,
            double[] exposure,
            int x1,
            int x2,
            double startValue,
            double targetAvg) throws Exception
    {
        /*
         * y[x] = startValue * exp(b * t), where t = x - x1.
         *
         * b is chosen so that exposure-weighted average over [x1, x2]
         * equals targetAvg.
         */
        double b = solveMonotoneParameter(
                                          p -> weightedAverageGompertz(exposure, x1, x2, startValue, p),
                                          targetAvg);

        for (int x = x1; x <= x2; x++)
        {
            int t = x - x1;
            y[x] = startValue * safeExp(b * t);
        }
    }

    private static double weightedAverageGompertz(
            double[] exposure,
            int x1,
            int x2,
            double startValue,
            double b)
    {
        double num = 0.0;
        double den = 0.0;

        for (int x = x1; x <= x2; x++)
        {
            int t = x - x1;
            double w = weight(exposure, x);
            double v = startValue * safeExp(b * t);

            num += w * v;
            den += w;
        }

        return num / den;
    }

    /* ========================================== Kannisto tail =========================================== */

    private static void fillKannistoTail(
            double[] y,
            double[] exposure,
            int x1,
            int x2,
            double startValue,
            double targetAvg) throws Exception
    {
        /*
         * Kannisto/logistic form:
         *
         *     y = K * odds / (1 + odds)
         *     odds = odds0 * exp(b * t)
         *
         * For qx, K should normally be 1.
         * For mx, K = 1 is still often acceptable up to age 100,
         * but if your mx can exceed 1, raise K.
         */
        double K = Math.max(1.0, 2.0 * Math.max(startValue, targetAvg));

        if (startValue >= 0.95 * K)
        {
            /*
             * Too close to asymptote; logistic model becomes numerically awkward.
             * Fall back to Gompertz.
             */
            fillGompertzTail(y, exposure, x1, x2, startValue, targetAvg);
            return;
        }

        double b = solveMonotoneParameter(
                                          p -> weightedAverageKannisto(exposure, x1, x2, startValue, K, p),
                                          targetAvg);

        double odds0 = startValue / (K - startValue);

        for (int x = x1; x <= x2; x++)
        {
            int t = x - x1;
            double odds = odds0 * safeExp(b * t);
            y[x] = K * odds / (1.0 + odds);
        }
    }

    private static double weightedAverageKannisto(
            double[] exposure,
            int x1,
            int x2,
            double startValue,
            double K,
            double b)
    {
        double odds0 = startValue / (K - startValue);

        double num = 0.0;
        double den = 0.0;

        for (int x = x1; x <= x2; x++)
        {
            int t = x - x1;
            double odds = odds0 * safeExp(b * t);
            double v = K * odds / (1.0 + odds);
            double w = weight(exposure, x);

            num += w * v;
            den += w;
        }

        return num / den;
    }

    /* ========================================== Coale-Kisker-like tail =========================================== */

    private static void fillCoaleKiskerLikeTail(
            double[] y,
            double[] exposure,
            int x1,
            int x2,
            double startValue,
            double initialLogSlope,
            double targetAvg) throws Exception
    {
        /*
         * Coale-Kisker-like shape:
         *
         *     log y[t] = log(startValue)
         *                + h * t
         *                + 0.5 * c * t * (t - 1)
         *
         * h is inherited from the previous ages.
         * c is chosen so that the exposure-weighted average over the last bin
         * equals targetAvg.
         *
         * If c < 0, the log-slope decelerates with age.
         * If c > 0, it accelerates.
         */
        double h = initialLogSlope;

        double c = solveMonotoneParameter(
                                          p -> weightedAverageCoaleKiskerLike(exposure, x1, x2, startValue, h, p),
                                          targetAvg);

        double logStart = Math.log(Math.max(startValue, OLD_AGE_EPS));

        for (int x = x1; x <= x2; x++)
        {
            int t = x - x1;
            double logv = logStart + h * t + 0.5 * c * t * (t - 1);
            y[x] = safeExp(logv);
        }
    }

    private static double weightedAverageCoaleKiskerLike(
            double[] exposure,
            int x1,
            int x2,
            double startValue,
            double h,
            double c)
    {
        double logStart = Math.log(Math.max(startValue, OLD_AGE_EPS));

        double num = 0.0;
        double den = 0.0;

        for (int x = x1; x <= x2; x++)
        {
            int t = x - x1;
            double logv = logStart + h * t + 0.5 * c * t * (t - 1);
            double v = safeExp(logv);
            double w = weight(exposure, x);

            num += w * v;
            den += w;
        }

        return num / den;
    }

    /* ============================================== SHARED HELPERS ======================================= */

    @FunctionalInterface
    private interface TailAverageFunction
    {
        double value(double p);
    }

    private static double solveMonotoneParameter(TailAverageFunction f, double target) throws Exception
    {
        /*
         * Finds p such that f(p) = target.
         * f is expected to be monotone increasing.
         */
        double lo = -1.0;
        double hi = 1.0;

        double flo = f.value(lo) - target;
        double fhi = f.value(hi) - target;

        int expand = 0;

        while (flo > 0.0 && expand++ < 60)
        {
            hi = lo;
            fhi = flo;
            lo *= 2.0;
            flo = f.value(lo) - target;
        }

        expand = 0;

        while (fhi < 0.0 && expand++ < 60)
        {
            lo = hi;
            flo = fhi;
            hi *= 2.0;
            fhi = f.value(hi) - target;
        }

        if (flo > 0.0 || fhi < 0.0)
            throw new Exception("Unable to bracket old-age tail parameter");

        for (int it = 0; it < 100; it++)
        {
            double mid = 0.5 * (lo + hi);
            double fmid = f.value(mid) - target;

            if (Math.abs(fmid) <= Math.max(1e-12, Math.abs(target) * 1e-12))
                return mid;

            if (fmid < 0.0)
                lo = mid;
            else
                hi = mid;
        }

        return 0.5 * (lo + hi);
    }

    private static double estimateTailStartValue(double[] y, int x1, double targetAvg)
    {
        if (x1 <= 0)
            return Math.max(targetAvg, OLD_AGE_EPS);

        double prev = Math.max(y[x1 - 1], OLD_AGE_EPS);
        double h = estimatePreviousLogSlope(y, x1, 5);

        /*
         * Do not allow a negative inherited old-age slope.
         */
        h = Math.max(0.0, h);

        /*
         * Avoid letting one noisy preceding point create an absurd jump.
         */
        h = Math.min(h, 0.25);

        double start = prev * safeExp(h);

        /*
         * Usually the first point of the last bin should not already exceed
         * the bin average by a lot. If it does, the aggregate constraint would
         * force a declining old-age tail. Leave the value alone, but keep it finite.
         */
        if (!Double.isFinite(start) || start <= 0.0)
            start = prev;

        return Math.max(start, OLD_AGE_EPS);
    }

    private static double estimatePreviousLogSlope(double[] y, int x1, int n)
    {
        /*
         * Estimate slope of log(y) over the n points immediately before x1.
         * Returns per-age log slope.
         */
        int right = x1 - 1;
        int left = Math.max(0, right - n + 1);

        if (right <= left)
            return 0.0;

        double sx = 0.0;
        double sy = 0.0;
        double sxx = 0.0;
        double sxy = 0.0;
        int count = 0;

        for (int x = left; x <= right; x++)
        {
            double v = y[x];

            if (!(v > 0.0) || !Double.isFinite(v))
                continue;

            double xx = x;
            double yy = Math.log(v);

            sx += xx;
            sy += yy;
            sxx += xx * xx;
            sxy += xx * yy;
            count++;
        }

        if (count < 2)
            return 0.0;

        double den = count * sxx - sx * sx;

        if (Math.abs(den) < 1e-30)
            return 0.0;

        return (count * sxy - sx * sy) / den;
    }

    private static double weight(double[] exposure, int x)
    {
        if (exposure == null)
            return 1.0;

        double w = exposure[x];

        if (!(w > 0.0) || !Double.isFinite(w))
            return 0.0;

        return w;
    }

    private static double safeExp(double z)
    {
        if (z > 700.0)
            return Math.exp(700.0);

        if (z < -700.0)
            return Math.exp(-700.0);

        return Math.exp(z);
    }
}
