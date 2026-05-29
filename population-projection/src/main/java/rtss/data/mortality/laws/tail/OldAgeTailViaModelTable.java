package rtss.data.mortality.laws.tail;

import rtss.data.bin.Bin;

public class OldAgeTailViaModelTable
{
    private static final double OLD_AGE_EPS = 1e-12;

    public static final double STANDARD_TAIL_BETA = 1.0;

    /*
     * beta = 1.0 means: use the standard table's old-age qx shape as-is.
     * beta < 1.0 flattens the tail.
     * beta > 1.0 steepens the tail.
     *
     * For first experiments I would keep beta = 1.0.
     */
    public static double[] applyStandardQxTailToBin(
            double[] curve,
            Bin[] bins,
            double[] exposure,
            double[] standardQx,
            int tailBinIndex,
            double beta) throws Exception
    {
        if (curve == null)
            throw new IllegalArgumentException("curve is null");

        if (bins == null || bins.length == 0)
            throw new IllegalArgumentException("bins is null or empty");

        if (standardQx == null)
            throw new IllegalArgumentException("standardQx is null");

        if (exposure != null && exposure.length != curve.length)
            throw new IllegalArgumentException("exposure length does not match curve length: " +
                                               exposure.length + " vs " + curve.length);

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

        if (ix1 == ix2)
            return curve;

        double targetAvg = bin.avg;

        if (!(targetAvg > 0.0) || !Double.isFinite(targetAvg))
            return curve;

        double[] out = curve.clone();

        /*
         * Standard table is used only as shape.
         *
         * shape(age) = (standardQx[age] / standardQx[firstTailAge]) ^ beta
         *
         * Then the whole shape is scaled so that:
         *
         *     targetAvg =
         *         sum(exposure[age] * out[age]) / sum(exposure[age])
         *
         * over the tail bin.
         *
         * Because only ratios are used, it does not matter that standardQx
         * is in probability units 0..1 while curve/bin.avg may be in per-1000 units.
         */
        double q0 = Math.max(standardQx[bin.age_x1], OLD_AGE_EPS);

        double weightedShapeSum = 0.0;
        double exposureSum = 0.0;

        for (int age = bin.age_x1; age <= bin.age_x2; age++)
        {
            int ix = age - firstAge;

            double w = weight(exposure, ix);

            if (w == 0.0)
                continue;

            double q = Math.max(standardQx[age], OLD_AGE_EPS);
            double shape = Math.pow(q / q0, beta);

            weightedShapeSum += w * shape;
            exposureSum += w;
        }

        if (!(weightedShapeSum > 0.0) || !(exposureSum > 0.0))
            throw new Exception("Unable to calculate standard-table tail scale");

        double currentShapeAvg = weightedShapeSum / exposureSum;
        double scale = targetAvg / currentShapeAvg;

        for (int age = bin.age_x1; age <= bin.age_x2; age++)
        {
            int ix = age - firstAge;

            double q = Math.max(standardQx[age], OLD_AGE_EPS);
            double shape = Math.pow(q / q0, beta);

            out[ix] = scale * shape;
        }

        /*
         * Optional exact numerical check.
         */
        double checkAvg = exposureWeightedAverage(out, exposure, firstAge, bin.age_x1, bin.age_x2);

        if (Math.abs(checkAvg - targetAvg) > Math.max(1e-9, Math.abs(targetAvg) * 1e-9))
        {
            throw new Exception(String.format(
                                              "Standard-table tail calibration failed: expected %.12g, got %.12g",
                                              targetAvg,
                                              checkAvg));
        }

        return out;
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
