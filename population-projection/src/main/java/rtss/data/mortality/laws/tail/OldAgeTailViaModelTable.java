package rtss.data.mortality.laws.tail;

import rtss.data.bin.Bin;
import rtss.data.mortality.SingleMortalityTable;

public class OldAgeTailViaModelTable
{
    private static final double OLD_AGE_EPS = 1e-12;

    /*
     * beta = 1.0: use standard table shape as-is.
     * beta < 1.0: flatten the within-bin shape.
     * beta > 1.0: steepen the within-bin shape.
     */
    public static final double DEFAULT_BETA = 1.0;

    public static double[] apply(
            double[] curve,
            Bin[] bins,
            double[] exposure,
            SingleMortalityTable mt,
            int startAge) throws Exception
    {
        return apply(curve, bins, exposure, mt.qx(), startAge, DEFAULT_BETA);
    }

    public static double[] apply(
            double[] curve,
            Bin[] bins,
            double[] exposure,
            SingleMortalityTable mt,
            int startAge,
            double beta) throws Exception
    {
        return apply(curve, bins, exposure, mt.qx(), startAge, beta);
    }

    public static double[] apply(
            double[] curve,
            Bin[] bins,
            double[] exposure,
            double[] standardQx,
            int startAge) throws Exception
    {
        return apply(curve, bins, exposure, standardQx, startAge, DEFAULT_BETA);
    }

    /*
     * Replace all full bins whose age_x1 >= startAge.
     *
     * For every replaced bin i:
     *
     *     bin.avg =
     *         sum(exposure[x] * result[x]) / sum(exposure[x])
     *
     * is preserved exactly, up to floating-point roundoff.
     *
     * Important:
     * - startAge should normally be a bin boundary: 65, 70, 75, etc.
     * - curve and exposure are indexed relative to bins[0].age_x1.
     * - standardQx is indexed by actual age.
     */
    public static double[] apply(
            double[] curve,
            Bin[] bins,
            double[] exposure,
            double[] standardQx,
            int startAge,
            double beta) throws Exception
    {
        validateInputs(curve, bins, exposure, standardQx, beta);

        int firstAge = bins[0].age_x1;
        double[] out = curve.clone();

        for (Bin bin : bins)
        {
            if (bin.age_x1 < startAge)
                continue;

            replaceBinWithStandardShape(
                    out,
                    bin,
                    exposure,
                    standardQx,
                    firstAge,
                    beta);
        }

        return out;
    }

    private static void replaceBinWithStandardShape(
            double[] out,
            Bin bin,
            double[] exposure,
            double[] standardQx,
            int firstAge,
            double beta) throws Exception
    {
        int ix1 = bin.age_x1 - firstAge;
        int ix2 = bin.age_x2 - firstAge;

        if (ix1 < 0 || ix2 >= out.length || ix1 > ix2)
            throw new IllegalArgumentException("bin does not match curve range: " +
                                               bin.age_x1 + "-" + bin.age_x2);

        if (bin.age_x1 < 0 || bin.age_x2 >= standardQx.length)
            throw new IllegalArgumentException("standardQx does not cover bin ages: " +
                                               bin.age_x1 + "-" + bin.age_x2);

        double targetAvg = bin.avg;

        if (!(targetAvg > 0.0) || !Double.isFinite(targetAvg))
            return;

        double[] shape = standardShapeForBin(standardQx, bin.age_x1, bin.age_x2, beta);

        double weightedShapeAvg = exposureWeightedAverageOfShape(
                shape,
                exposure,
                firstAge,
                bin.age_x1);

        if (!(weightedShapeAvg > 0.0) || !Double.isFinite(weightedShapeAvg))
            throw new Exception("Unable to calculate weighted shape average for bin " +
                                bin.age_x1 + "-" + bin.age_x2);

        double scale = targetAvg / weightedShapeAvg;

        for (int age = bin.age_x1; age <= bin.age_x2; age++)
        {
            int ix = age - firstAge;
            int k = age - bin.age_x1;

            out[ix] = scale * shape[k];
        }

        double checkAvg = exposureWeightedAverage(out, exposure, firstAge, bin.age_x1, bin.age_x2);

        if (Math.abs(checkAvg - targetAvg) > Math.max(1e-9, Math.abs(targetAvg) * 1e-9))
        {
            throw new Exception(String.format(
                    "Bin average preservation failed for %d-%d: expected %.12g, got %.12g",
                    bin.age_x1,
                    bin.age_x2,
                    targetAvg,
                    checkAvg));
        }
    }

    /*
     * Shape inside one bin.
     *
     * We use only ratios, so standardQx may be in 0..1 probability units while
     * curve/bin.avg may be in per-1000 units.
     */
    private static double[] standardShapeForBin(
            double[] standardQx,
            int age1,
            int age2,
            double beta)
    {
        double[] shape = new double[age2 - age1 + 1];

        double q0 = Math.max(standardQx[age1], OLD_AGE_EPS);
        double previousRatio = 1.0;

        for (int age = age1; age <= age2; age++)
        {
            double q = Math.max(standardQx[age], OLD_AGE_EPS);
            double ratio = q / q0;

            /*
             * Avoid importing small accidental downward wiggles from
             * the standard table into the old-age tail.
             */
            ratio = Math.max(ratio, previousRatio);
            previousRatio = ratio;

            shape[age - age1] = Math.pow(ratio, beta);
        }

        return shape;
    }

    private static double exposureWeightedAverageOfShape(
            double[] shape,
            double[] exposure,
            int firstAge,
            int age1)
    {
        double num = 0.0;
        double den = 0.0;

        for (int k = 0; k < shape.length; k++)
        {
            int age = age1 + k;
            int ix = age - firstAge;

            double w = weight(exposure, ix);

            if (w == 0.0)
                continue;

            num += w * shape[k];
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

    private static void validateInputs(
            double[] curve,
            Bin[] bins,
            double[] exposure,
            double[] standardQx,
            double beta)
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

        if (!(beta >= 0.0) || !Double.isFinite(beta))
            throw new IllegalArgumentException("invalid beta: " + beta);
    }
}