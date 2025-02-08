package rtss.data.curves;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.util.Util;

public class EnsureNonNegativeCurve
{
    /*
     * Modify the curve @yyy interpolating bin set @bins to ensure the curve has only non-negative values.
     * 
     * Currently this is only a partial implementation for specific practical case we encounter with our data sets,
     * but can be generalized if needed utilizing the same general approach of controlled (and iterative)
     * elastic distortion of y-scale within the bin. If segment endpoints are to be kept at their present values, 
     * the application of this distortion can further be controlled by a bell-shaped curve coming down to zero
     * at both ends of the segment.
     */
    public static double[] ensureNonNegative(double[] yyy, Bin[] bins, String title, TargetResolution targetResolution) throws Exception
    {
        Bin first = Bins.firstBin(bins);
        Bin last = Bins.lastBin(bins);
        int ppy = yyy.length / (last.age_x2 + 1);
        if (yyy.length != ppy * (last.age_x2 + 1))
            throw new IllegalArgumentException();

        Bin bin = null;

        for (int x = 0; x < yyy.length; x++)
        {
            if (yyy[x] < 0)
            {
                int age = first.age_x1 + x / ppy;
                bin = Bins.binForAge(age, bins);
                if (bin == null)
                    throw new Exception("Internal error");
                yyy = reshape(bins, bin, yyy, title, targetResolution);
                x = bin.age_x2;
            }
        }

        if (!Util.isNonNegative(yyy))
            throw new Exception("Error calculating curve (negative value)");

        return yyy;
    }

    private static double[] reshape(Bin[] bins, Bin bin, double[] yyy, String title, TargetResolution targetResolution) throws Exception
    {
        Bin first = Bins.firstBin(bins);
        Bin last = Bins.lastBin(bins);
        int ppy = yyy.length / (last.age_x2 + 1);

        int x1 = ppy * (bin.age_x1 - first.age_x1);
        int x2 = ppy * (bin.age_x2 + 1 - first.age_x1) - 1;
        double[] seg = Util.splice(yyy, x1, x2);

        if (bin == last)
        {
            /*
             * A correction for the case of the very last (rightmost) curve segment, 
             * corresponding to the last bin, going negative.
             * It is assumed that:
             * - the leftmost point of the segment is positive and is a maximum within the segment. 
             *  
             * The change is performed into two steps: 
             *  
             *  1. Remap the segment y-value range from [min...max] to [0...max].
             *     This will eliminate negatives, but will also increase the mean value of the segment. 
             *  
             *  2. Distort y-scale (y-values) within the segment to preserve mean value for the segment,
             *     so it still matches the bin value.
             *     Distortion is performed by y -> ymax * (y / ymax) ^ a, where a is > 1.
             *     This effectively pulls down the curve in an elastic way.
             *     Distortion is performed iteratively until a matching value of "a" is found.
             * 
             */
            seg = reshape(seg, bin.avg * bin.widths_in_years * ppy, true, title);
        }
        else if (targetResolution == TargetResolution.YEARLY)
        {
            // untested
            throw new Exception("Unimplemented: can only fix negative values for the last bin, but not for intermediate bins");
        }
        else
        {
            double age_years = bin.age_x1 / ppy;
            if (targetResolution == TargetResolution.DAILY)
                age_years /= 365;
            if (age_years < 90)
                throw new Exception("Unimplemented: can only fix negative values for high-age bin, but not for lower age bins");

            seg = reshape(seg, bin.avg * bin.widths_in_years * ppy, false, title);
        }

        yyy = Util.dup(yyy);
        Util.insert(yyy, seg, x1);
        return yyy;
    }

    /* ============================================================================================== */

    private static double[] reshape(double[] seg, double target_sum, boolean last, String title) throws Exception
    {
        seg = Util.dup(seg);

        /*
         * Remap the segment y-value range from [min...max] to [new_min...max].
         */
        double min = Util.min(seg);
        double max = Util.max(seg);

        if (max <= 0)
            throw new Exception("Negative values in the curve, the fix is not implemented");
        if (last && seg[0] != max)
            throw new Exception("Negative values in the curve, the fix is not implemented");

        double new_min = 0.01 * max;
        for (int x = 0; x < seg.length; x++)
            seg[x] = new_min + (max - new_min) * (seg[x] - min) / (max - min);

        /*
         * Find a1 and a2 as the range for binary search
         */
        double a1 = 1.0;
        double[] xseg = distort(seg, a1);
        double sum1 = Util.sum(xseg);
        if (sum1 < target_sum)
            throw new Exception("Internal error");

        double a2 = 1.0;
        double sum2;

        for (;;)
        {
            xseg = distort(seg, a2);
            sum2 = Util.sum(xseg);
            if (sum2 < target_sum)
                break;
            a2 += 1.0;
        }

        /*
         * Do binary search
         */
        for (;;)
        {
            double a = (a1 + a2) / 2;
            xseg = distort(seg, a);
            double sum = Util.sum(xseg);

            if (!Util.differ(sum, target_sum))
                break;

            if (sum > target_sum)
            {
                // increase a
                a1 = a;
            }
            else
            {
                // decrease a
                a2 = a;
            }
        }

        return xseg;
    }

    private static double[] distort(double[] seg, double a)
    {
        double max = Util.max(seg);
        double[] xseg = Util.dup(seg);

        for (int x = 0; x < seg.length; x++)
            xseg[x] = max * Math.pow(seg[x] / max, a);

        return xseg;
    }
}
