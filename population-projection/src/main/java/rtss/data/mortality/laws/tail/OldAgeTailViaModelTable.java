package rtss.data.mortality.laws.tail;

import java.util.ArrayList;
import java.util.List;

import rtss.data.bin.Bin;
import rtss.data.mortality.SingleMortalityTable;

public final class OldAgeTailViaModelTable
{
    private OldAgeTailViaModelTable()
    {
    }

    private static final double OLD_AGE_EPS = 1e-12;
    private static final double CHECK_TOLERANCE = 1e-9;

    public static final double DEFAULT_BETA = 1.0;
    public static final int DEFAULT_ITERATIONS = 250;
    public static final int DEFAULT_SMOOTHING_RADIUS = 2;
    public static final double DEFAULT_SMOOTHING_STRENGTH = 0.35;

    public static double[] apply(
            double[] curve,
            Bin[] bins,
            double[] exposure,
            SingleMortalityTable mt,
            int startAge) throws Exception
    {
        return apply(curve, bins, exposure, mt.qx(), startAge);
    }

    public static double[] apply(
            double[] curve,
            Bin[] bins,
            double[] exposure,
            SingleMortalityTable mt,
            int startAge,
            int endAge) throws Exception
    {
        return apply(curve, bins, exposure, mt.qx(), startAge, endAge);
    }

    public static double[] apply(
            double[] curve,
            Bin[] bins,
            double[] exposure,
            SingleMortalityTable mt,
            int startAge,
            int endAge,
            double beta) throws Exception
    {
        return apply(curve, bins, exposure, mt.qx(), startAge, endAge, beta);
    }

    public static double[] apply(
            double[] curve,
            Bin[] bins,
            double[] exposure,
            double[] standardQx,
            int startAge) throws Exception
    {
        int firstAge = firstAge(curve, bins);
        int endAge = firstAge + curve.length - 1;

        return apply(curve, bins, exposure, standardQx, startAge, endAge);
    }

    public static double[] apply(
            double[] curve,
            Bin[] bins,
            double[] exposure,
            double[] standardQx,
            int startAge,
            int endAge) throws Exception
    {
        return apply(curve, bins, exposure, standardQx, startAge, endAge, DEFAULT_BETA);
    }

    public static double[] apply(
            double[] curve,
            Bin[] bins,
            double[] exposure,
            double[] standardQx,
            int startAge,
            int endAge,
            double beta) throws Exception
    {
        return apply(
                curve,
                bins,
                exposure,
                standardQx,
                startAge,
                endAge,
                beta,
                DEFAULT_ITERATIONS,
                DEFAULT_SMOOTHING_RADIUS,
                DEFAULT_SMOOTHING_STRENGTH);
    }

    /*
     * Builds an old-age segment using:
     *
     *     y[x] = base[x] * exp(z[x])
     *
     * where:
     *
     *     base[x] = standardQx[x]^beta, normalized only as shape;
     *     z[x]    = smooth multiplicative correction.
     *
     * The method preserves, for every selected source bin:
     *
     *     bin.avg = sum(exposure[x] * y[x]) / sum(exposure[x])
     *
     * exactly, up to floating-point roundoff.
     *
     * Only bins fully contained in [startAge, endAge] are replaced.
     * If a bin partially overlaps the range, this is treated as an error,
     * because preserving a partially replaced bin would be ambiguous.
     *
     * Typical call:
     *
     *     y = OldAgeTailViaModelTable.apply(
     *             y, bins, exposure, standardMt, 70, 100, 1.0);
     *
     * Pass endAge = 100 if bins contain an artificial fake bin after 100
     * and you do not want this fake bin to participate in the tail shaping.
     */
    public static double[] apply(
            double[] curve,
            Bin[] bins,
            double[] exposure,
            double[] standardQx,
            int startAge,
            int endAge,
            double beta,
            int iterations,
            int smoothingRadius,
            double smoothingStrength) throws Exception
    {
        validateInputs(
                curve,
                bins,
                exposure,
                standardQx,
                startAge,
                endAge,
                beta,
                iterations,
                smoothingRadius,
                smoothingStrength);

        int firstAge = bins[0].age_x1;

        List<Bin> selectedBins = selectedBins(bins, startAge, endAge);

        if (selectedBins.isEmpty())
            return curve.clone();

        double[] out = curve.clone();

        /*
         * Shape from the model table, only for the selected age range.
         * The absolute scale of standardQx is irrelevant.
         */
        double[] base = makeBaseShape(curve.length, firstAge, startAge, endAge, standardQx, beta);

        /*
         * Initial tail: model-table shape, then exact raking to source bins.
         */
        for (int age = startAge; age <= endAge; age++)
        {
            int ix = age - firstAge;
            out[ix] = base[ix];
        }

        projectSelectedBins(out, selectedBins, exposure, firstAge);

        /*
         * Iterate:
         *
         * 1. Convert current old-age segment to log multiplier over standard shape.
         * 2. Smooth the log multiplier.
         * 3. Rebuild y = base * exp(smoothed multiplier).
         * 4. Project each source bin back to its exact average.
         *
         * This keeps the correction smooth, instead of independently normalizing
         * every bin and producing saw-tooth jumps at bin boundaries.
         */
        double[] z = new double[curve.length];

        for (int it = 0; it < iterations; it++)
        {
            for (int age = startAge; age <= endAge; age++)
            {
                int ix = age - firstAge;
                z[ix] = Math.log(Math.max(out[ix], OLD_AGE_EPS) / Math.max(base[ix], OLD_AGE_EPS));
            }

            smoothInPlace(z, startAge - firstAge, endAge - firstAge, smoothingRadius, smoothingStrength);

            for (int age = startAge; age <= endAge; age++)
            {
                int ix = age - firstAge;
                out[ix] = base[ix] * safeExp(z[ix]);
            }

            projectSelectedBins(out, selectedBins, exposure, firstAge);
        }

        /*
         * Final exact projection and verification.
         */
        projectSelectedBins(out, selectedBins, exposure, firstAge);
        verifySelectedBins(out, selectedBins, exposure, firstAge);

        return out;
    }

    private static List<Bin> selectedBins(
            Bin[] bins,
            int startAge,
            int endAge)
    {
        List<Bin> selected = new ArrayList<>();

        for (Bin bin : bins)
        {
            if (bin.age_x2 < startAge)
                continue;

            if (bin.age_x1 > endAge)
                continue;

            if (bin.age_x1 < startAge || bin.age_x2 > endAge)
            {
                throw new IllegalArgumentException(
                        "bin partially overlaps replacement range: " +
                        bin.age_x1 + "-" + bin.age_x2 +
                        ", range " + startAge + "-" + endAge);
            }

            selected.add(bin);
        }

        return selected;
    }

    private static double[] makeBaseShape(
            int length,
            int firstAge,
            int startAge,
            int endAge,
            double[] standardQx,
            double beta)
    {
        double[] base = new double[length];

        double q0 = Math.max(standardQx[startAge], OLD_AGE_EPS);
        double previousRatio = 1.0;

        for (int age = startAge; age <= endAge; age++)
        {
            int ix = age - firstAge;

            double q = Math.max(standardQx[age], OLD_AGE_EPS);
            double ratio = q / q0;

            /*
             * Do not import small accidental downward wiggles from the model table.
             */
            ratio = Math.max(ratio, previousRatio);
            previousRatio = ratio;

            base[ix] = Math.pow(ratio, beta);
        }

        return base;
    }

    private static void projectSelectedBins(
            double[] y,
            List<Bin> selectedBins,
            double[] exposure,
            int firstAge) throws Exception
    {
        for (Bin bin : selectedBins)
            projectBin(y, bin, exposure, firstAge);
    }

    private static void projectBin(
            double[] y,
            Bin bin,
            double[] exposure,
            int firstAge) throws Exception
    {
        double currentAvg = exposureWeightedAverage(y, exposure, firstAge, bin.age_x1, bin.age_x2);

        if (!(currentAvg > 0.0) || !Double.isFinite(currentAvg))
        {
            throw new Exception(
                    "Unable to project bin because current average is invalid for " +
                    bin.age_x1 + "-" + bin.age_x2);
        }

        double targetAvg = bin.avg;

        if (!(targetAvg > 0.0) || !Double.isFinite(targetAvg))
            return;

        double scale = targetAvg / currentAvg;

        for (int age = bin.age_x1; age <= bin.age_x2; age++)
        {
            int ix = age - firstAge;
            y[ix] *= scale;
        }
    }

    private static void verifySelectedBins(
            double[] y,
            List<Bin> selectedBins,
            double[] exposure,
            int firstAge) throws Exception
    {
        for (Bin bin : selectedBins)
        {
            double got = exposureWeightedAverage(y, exposure, firstAge, bin.age_x1, bin.age_x2);
            double expected = bin.avg;

            if (Math.abs(got - expected) > Math.max(CHECK_TOLERANCE, Math.abs(expected) * CHECK_TOLERANCE))
            {
                throw new Exception(String.format(
                        "Bin average preservation failed for %d-%d: expected %.12g, got %.12g",
                        bin.age_x1,
                        bin.age_x2,
                        expected,
                        got));
            }
        }
    }

    private static void smoothInPlace(
            double[] z,
            int ix1,
            int ix2,
            int radius,
            double strength)
    {
        if (radius <= 0 || strength <= 0.0)
            return;

        double[] smoothed = z.clone();

        for (int ix = ix1; ix <= ix2; ix++)
        {
            double num = 0.0;
            double den = 0.0;

            for (int jx = Math.max(ix1, ix - radius); jx <= Math.min(ix2, ix + radius); jx++)
            {
                int d = Math.abs(jx - ix);

                /*
                 * Simple triangular kernel.
                 */
                double w = radius + 1 - d;

                num += w * z[jx];
                den += w;
            }

            double local = num / den;

            smoothed[ix] = (1.0 - strength) * z[ix] + strength * local;
        }

        for (int ix = ix1; ix <= ix2; ix++)
            z[ix] = smoothed[ix];
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

    private static int firstAge(double[] curve, Bin[] bins)
    {
        validateBasicInputs(curve, bins);
        return bins[0].age_x1;
    }

    private static void validateInputs(
            double[] curve,
            Bin[] bins,
            double[] exposure,
            double[] standardQx,
            int startAge,
            int endAge,
            double beta,
            int iterations,
            int smoothingRadius,
            double smoothingStrength)
    {
        validateBasicInputs(curve, bins);

        if (standardQx == null)
            throw new IllegalArgumentException("standardQx is null");

        if (exposure != null && exposure.length != curve.length)
        {
            throw new IllegalArgumentException(
                    "exposure length does not match curve length: " +
                    exposure.length + " vs " + curve.length);
        }

        int firstAge = bins[0].age_x1;
        int lastAge = firstAge + curve.length - 1;

        if (startAge < firstAge || startAge > lastAge)
        {
            throw new IllegalArgumentException(
                    "startAge is outside curve range: " +
                    startAge + ", range " + firstAge + "-" + lastAge);
        }

        if (endAge < startAge || endAge > lastAge)
        {
            throw new IllegalArgumentException(
                    "endAge is outside curve range or before startAge: " +
                    endAge + ", allowed " + startAge + "-" + lastAge);
        }

        if (startAge < 0 || endAge >= standardQx.length)
        {
            throw new IllegalArgumentException(
                    "standardQx does not cover age range " +
                    startAge + "-" + endAge);
        }

        if (!(beta >= 0.0) || !Double.isFinite(beta))
            throw new IllegalArgumentException("invalid beta: " + beta);

        if (iterations < 0)
            throw new IllegalArgumentException("iterations must be non-negative: " + iterations);

        if (smoothingRadius < 0)
            throw new IllegalArgumentException("smoothingRadius must be non-negative: " + smoothingRadius);

        if (!(smoothingStrength >= 0.0 && smoothingStrength <= 1.0) || !Double.isFinite(smoothingStrength))
            throw new IllegalArgumentException("invalid smoothingStrength: " + smoothingStrength);
    }

    private static void validateBasicInputs(
            double[] curve,
            Bin[] bins)
    {
        if (curve == null)
            throw new IllegalArgumentException("curve is null");

        if (curve.length == 0)
            throw new IllegalArgumentException("curve is empty");

        if (bins == null || bins.length == 0)
            throw new IllegalArgumentException("bins is null or empty");
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