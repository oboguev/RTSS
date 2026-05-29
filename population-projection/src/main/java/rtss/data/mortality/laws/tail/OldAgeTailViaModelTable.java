package rtss.data.mortality.laws.tail;

import rtss.data.bin.Bin;

public class OldAgeTailViaModelTable
{
    private static final double OLD_AGE_EPS = 1e-12;

    /*
     * beta = 0 gives a flat tail.
     * beta = 1 uses the standard table's old-age qx shape as-is.
     * beta > 1 steepens the standard table shape.
     */
    public static final double STANDARD_TAIL_BETA_MIN = 0.0;
    public static final double STANDARD_TAIL_BETA_MAX = 3.0;

    /*
     * Backward-compatible alias for older call sites that supplied a single beta.
     * In the new continuous-tail implementation this value is interpreted as
     * the upper allowed beta, not as a fixed beta.
     */
    public static final double STANDARD_TAIL_BETA = STANDARD_TAIL_BETA_MAX;

    /*
     * Prevent one noisy preceding point from generating an absurd inherited slope.
     * 0.12 means about +12.8% per year.
     */
    public static final double MAX_INHERITED_LOG_SLOPE = 0.12;

    private static final int INHERITED_SLOPE_POINTS = 5;

    public static double[] applyStandardQxTailToLastBin(
            double[] curve,
            Bin[] bins,
            double[] exposure,
            double[] standardQx) throws Exception
    {
        return applyStandardQxTailToBin(curve,
                                        bins,
                                        exposure,
                                        standardQx,
                                        bins.length - 1);
    }

    public static double[] applyStandardQxTailToBin(
            double[] curve,
            Bin[] bins,
            double[] exposure,
            double[] standardQx,
            int tailBinIndex) throws Exception
    {
        return applyStandardQxTailToBin(
                                        curve,
                                        bins,
                                        exposure,
                                        standardQx,
                                        tailBinIndex,
                                        STANDARD_TAIL_BETA_MIN,
                                        STANDARD_TAIL_BETA_MAX);
    }

    /*
     * Compatibility overload for the previous version of this class.
     *
     * The old implementation used beta as a fixed exponent:
     *
     *     tail[age] = scale * standardRatio[age]^beta
     *
     * That preserved the bin average but could create a discontinuity at the
     * start of the old-age tail. The new implementation instead tries to
     * preserve continuity at the first age of the tail and solves beta from
     * the bin-average constraint. Therefore the supplied beta is now treated
     * as the maximum allowed beta.
     */
    public static double[] applyStandardQxTailToBin(
            double[] curve,
            Bin[] bins,
            double[] exposure,
            double[] standardQx,
            int tailBinIndex,
            double betaMax) throws Exception
    {
        if (!(betaMax >= 0.0) || !Double.isFinite(betaMax))
            betaMax = STANDARD_TAIL_BETA_MAX;

        return applyStandardQxTailToBin(curve,
                                        bins,
                                        exposure,
                                        standardQx,
                                        tailBinIndex,
                                        STANDARD_TAIL_BETA_MIN,
                                        betaMax);
    }

    public static double[] applyStandardQxTailToBin(
            double[] curve,
            Bin[] bins,
            double[] exposure,
            double[] standardQx,
            int tailBinIndex,
            double betaMin,
            double betaMax) throws Exception
    {
        validateInputs(curve, bins, exposure, standardQx, tailBinIndex);

        if (!(betaMin >= 0.0) || !Double.isFinite(betaMin))
            betaMin = STANDARD_TAIL_BETA_MIN;

        if (!(betaMax >= betaMin) || !Double.isFinite(betaMax))
            betaMax = STANDARD_TAIL_BETA_MAX;

        Bin bin = bins[tailBinIndex];

        int firstAge = bins[0].age_x1;

        int ix1 = bin.age_x1 - firstAge;
        int ix2 = bin.age_x2 - firstAge;

        if (ix1 == ix2)
            return curve;

        double targetAvg = bin.avg;

        if (!(targetAvg > 0.0) || !Double.isFinite(targetAvg))
            return curve;

        double[] out = curve.clone();

        /*
         * standardRatio[0] == 1.0.
         *
         * Ratios are made non-decreasing to avoid accidental irregularities
         * in the standard table becoming visible as artificial dents in the tail.
         */
        double[] standardRatio = standardTailRatios(standardQx,
                                                    bin.age_x1,
                                                    bin.age_x2);

        /*
         * Desired first tail value: continuation of the preceding PCLM curve.
         */
        double desiredStart = estimateDesiredTailStart(curve, ix1, targetAvg);

        double beta;
        double start;

        /*
         * We want:
         *
         *     targetAvg = desiredStart * weightedAverage(standardRatio^beta)
         *
         * hence:
         *
         *     weightedAverage(standardRatio^beta) = targetAvg / desiredStart
         */
        double wantedRatio = targetAvg / desiredStart;

        double avgAtBetaMin = weightedAverageRatio(
                                                   standardRatio,
                                                   exposure,
                                                   firstAge,
                                                   bin.age_x1,
                                                   betaMin);

        double avgAtBetaMax = weightedAverageRatio(
                                                   standardRatio,
                                                   exposure,
                                                   firstAge,
                                                   bin.age_x1,
                                                   betaMax);

        if (!Double.isFinite(wantedRatio) || wantedRatio <= 0.0 ||
            !Double.isFinite(avgAtBetaMin) || !Double.isFinite(avgAtBetaMax))
        {
            beta = 1.0;
            start = targetAvg / weightedAverageRatio(
                                                     standardRatio,
                                                     exposure,
                                                     firstAge,
                                                     bin.age_x1,
                                                     beta);
        }
        else if (wantedRatio >= avgAtBetaMin && wantedRatio <= avgAtBetaMax)
        {
            beta = solveBetaForWeightedAverageRatio(
                                                    standardRatio,
                                                    exposure,
                                                    firstAge,
                                                    bin.age_x1,
                                                    wantedRatio,
                                                    betaMin,
                                                    betaMax);

            start = desiredStart;
        }
        else if (wantedRatio < avgAtBetaMin)
        {
            /*
             * The requested start is too high to preserve the bin average
             * with a nondecreasing tail. Use the flattest allowed shape and
             * adjust start minimally to preserve the aggregate.
             */
            beta = betaMin;
            start = targetAvg / avgAtBetaMin;
        }
        else
        {
            /*
             * The requested start is too low. Even the steepest allowed
             * standard-table deformation does not reach the required bin
             * average. Use the steepest allowed shape and adjust start to
             * preserve the aggregate.
             */
            beta = betaMax;
            start = targetAvg / avgAtBetaMax;
        }

        for (int age = bin.age_x1; age <= bin.age_x2; age++)
        {
            int k = age - bin.age_x1;
            int ix = age - firstAge;

            out[ix] = start * Math.pow(standardRatio[k], beta);
        }

        double checkAvg = exposureWeightedAverage(
                                                  out,
                                                  exposure,
                                                  firstAge,
                                                  bin.age_x1,
                                                  bin.age_x2);

        if (Math.abs(checkAvg - targetAvg) > Math.max(1e-9, Math.abs(targetAvg) * 1e-9))
        {
            throw new Exception(String.format(
                                              "Standard-table tail calibration failed: expected %.12g, got %.12g",
                                              targetAvg,
                                              checkAvg));
        }

        return out;
    }

    public static int findLastBinFullyInsideCurve(Bin[] bins, double[] curve)
    {
        if (bins == null || bins.length == 0 || curve == null)
            return -1;

        int firstAge = bins[0].age_x1;
        int maxCurveAge = firstAge + curve.length - 1;

        int best = -1;

        for (int i = 0; i < bins.length; i++)
        {
            Bin b = bins[i];

            if (b.age_x1 < firstAge)
                continue;

            if (b.age_x2 > maxCurveAge)
                continue;

            if (b.age_x1 > b.age_x2)
                continue;

            best = i;
        }

        return best;
    }

    private static void validateInputs(
            double[] curve,
            Bin[] bins,
            double[] exposure,
            double[] standardQx,
            int tailBinIndex)
    {
        if (curve == null)
            throw new IllegalArgumentException("curve is null");

        if (bins == null || bins.length == 0)
            throw new IllegalArgumentException("bins is null or empty");

        if (standardQx == null)
            throw new IllegalArgumentException("standardQx is null");

        if (exposure != null && exposure.length != curve.length)
        {
            throw new IllegalArgumentException(
                                               "exposure length does not match curve length: " +
                                               exposure.length + " vs " + curve.length);
        }

        if (tailBinIndex < 0 || tailBinIndex >= bins.length)
            throw new IllegalArgumentException("tailBinIndex is out of range: " + tailBinIndex);

        Bin bin = bins[tailBinIndex];
        int firstAge = bins[0].age_x1;

        int ix1 = bin.age_x1 - firstAge;
        int ix2 = bin.age_x2 - firstAge;

        if (ix1 < 0 || ix2 >= curve.length || ix1 > ix2)
            throw new IllegalArgumentException("tail bin does not match curve range");

        if (bin.age_x1 < 0 || bin.age_x2 >= standardQx.length)
            throw new IllegalArgumentException("standardQx does not cover tail bin ages");
    }

    private static double[] standardTailRatios(
            double[] standardQx,
            int age1,
            int age2)
    {
        double[] r = new double[age2 - age1 + 1];

        double q0 = Math.max(standardQx[age1], OLD_AGE_EPS);
        double previous = 1.0;

        for (int age = age1; age <= age2; age++)
        {
            double q = Math.max(standardQx[age], OLD_AGE_EPS);
            double ratio = q / q0;

            ratio = Math.max(ratio, previous);

            r[age - age1] = ratio;
            previous = ratio;
        }

        return r;
    }

    private static double estimateDesiredTailStart(
            double[] y,
            int ix1,
            double targetAvg)
    {
        if (ix1 <= 0)
            return targetAvg;

        double prev = y[ix1 - 1];

        if (!(prev > 0.0) || !Double.isFinite(prev))
            return targetAvg;

        double h = estimatePreviousLogSlope(y, ix1, INHERITED_SLOPE_POINTS);

        h = Math.max(0.0, h);
        h = Math.min(h, MAX_INHERITED_LOG_SLOPE);

        double start = prev * Math.exp(h);

        if (!(start > 0.0) || !Double.isFinite(start))
            return targetAvg;

        return start;
    }

    private static double estimatePreviousLogSlope(
            double[] y,
            int ix1,
            int n)
    {
        int right = ix1 - 1;
        int left = Math.max(0, right - n + 1);

        if (right <= left)
            return 0.0;

        double sx = 0.0;
        double sy = 0.0;
        double sxx = 0.0;
        double sxy = 0.0;
        int count = 0;

        for (int ix = left; ix <= right; ix++)
        {
            double v = y[ix];

            if (!(v > 0.0) || !Double.isFinite(v))
                continue;

            double x = ix;
            double ly = Math.log(v);

            sx += x;
            sy += ly;
            sxx += x * x;
            sxy += x * ly;
            count++;
        }

        if (count < 2)
            return 0.0;

        double den = count * sxx - sx * sx;

        if (Math.abs(den) < 1e-30)
            return 0.0;

        return (count * sxy - sx * sy) / den;
    }

    private static double solveBetaForWeightedAverageRatio(
            double[] standardRatio,
            double[] exposure,
            int firstAge,
            int age1,
            double targetRatio,
            double betaMin,
            double betaMax)
    {
        double lo = betaMin;
        double hi = betaMax;

        for (int it = 0; it < 80; it++)
        {
            double mid = 0.5 * (lo + hi);

            double avg = weightedAverageRatio(
                                              standardRatio,
                                              exposure,
                                              firstAge,
                                              age1,
                                              mid);

            if (avg < targetRatio)
                lo = mid;
            else
                hi = mid;
        }

        return 0.5 * (lo + hi);
    }

    private static double weightedAverageRatio(
            double[] standardRatio,
            double[] exposure,
            int firstAge,
            int age1,
            double beta)
    {
        double num = 0.0;
        double den = 0.0;

        for (int k = 0; k < standardRatio.length; k++)
        {
            int age = age1 + k;
            int ix = age - firstAge;

            double w = weight(exposure, ix);

            if (w == 0.0)
                continue;

            double v = Math.pow(standardRatio[k], beta);

            num += w * v;
            den += w;
        }

        if (den == 0.0)
            return Double.NaN;

        return num / den;
    }

    private static double exposureWeightedAverage(
            double[] y,
            double[] exposure,
            int firstAge,
            int age1,
            int age2)
    {
        double num = 0.0;
        double den = 0.0;

        for (int age = age1; age <= age2; age++)
        {
            int ix = age - firstAge;

            double w = weight(exposure, ix);

            if (w == 0.0)
                continue;

            num += w * y[ix];
            den += w;
        }

        if (den == 0.0)
            return Double.NaN;

        return num / den;
    }

    private static double weight(double[] exposure, int ix)
    {
        if (exposure == null)
            return 1.0;

        if (ix < 0 || ix >= exposure.length)
            return 0.0;

        double w = exposure[ix];

        if (!(w > 0.0) || !Double.isFinite(w))
            return 0.0;

        return w;
    }
}
