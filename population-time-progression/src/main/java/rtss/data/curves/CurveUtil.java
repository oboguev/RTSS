package rtss.data.curves;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.util.Clipboard;
import rtss.util.Util;

public class CurveUtil
{
    /*
     * Verify that the curve preserves mean values as indicated by the bins
     */
    public static void validate_means(double[] yy, Bin[] bins) throws Exception
    {
        for (Bin bin : bins)
        {
            double[] y = Util.splice(yy, bin.age_x1, bin.age_x2);
            if (Util.differ(Util.average(y), bin.avg, 0.001))
                throw new Exception("Curve does not preserve mean values of the bins");
        }
    }

    public static void verifyUShape(Bin[] bins, String title, boolean doThrow) throws Exception
    {
        if (Bins.flips(bins) > 1)
            error(title, doThrow, "Mortality curve has multiple local minumums and maximums");

        Bin firstBin = Bins.firstBin(bins);
        Bin lastBin = Bins.lastBin(bins);

        Bin maxBin = Bins.findMaxBin(bins);
        if (maxBin != firstBin && maxBin != lastBin)
            error(title, doThrow, "Mortality curve has an unexpected maximum in the middle");

        // minimum value bin
        Bin minBin = Bins.findMinBin(bins);
        if (minBin == firstBin || minBin == lastBin)
            error(title, doThrow, "Mortality minimum is not in the middle");

    }

    private static void error(String title, boolean doThrow, String msg) throws Exception
    {
        msg += ": " + title;
        if (doThrow)
            throw new Exception(msg);
        else
            Util.err(msg);
    }

    public static int ppy(double[] curve, Bin[] bins) throws Exception
    {
        Bin last = Bins.lastBin(bins);
        int ppy = curve.length / (last.age_x2 + 1);
        if (curve.length != ppy * (last.age_x2 + 1))
            throw new IllegalArgumentException();
        return ppy;
    }

    /*
     * Extract the segment for the bin from @curve
     */
    public static double[] seg(double[] curve, Bin bin, Bin[] bins, int ppy) throws Exception
    {
        Bin first = Bins.firstBin(bins);

        int x1 = ppy * (bin.age_x1 - first.age_x1);
        int x2 = ppy * (bin.age_x2 + 1 - first.age_x1) - 1;

        return Util.splice(curve, x1, x2);
    }

    /*
     * Insert the segment for the bin into @curve
     */
    public static void insert(double[] curve, Bin bin, Bin[] bins, double[] seg) throws Exception
    {
        int ppy = ppy(curve, bins);

        Bin first = Bins.firstBin(bins);

        int x1 = ppy * (bin.age_x1 - first.age_x1);
        int x2 = ppy * (bin.age_x2 + 1 - first.age_x1) - 1;

        if (seg.length != x2 - x1 + 1)
            throw new IllegalArgumentException();

        Util.insert(curve, seg, x1);
    }

    public static boolean isMonotonicallyDescending(double[] y)
    {
        for (int k = 1; k < y.length; k++)
        {
            if (y[k] > y[k - 1])
                return false;
        }

        return true;
    }

    /*
     * Fill in straigth line of values into @seg[x1...x2], interpolated frim @v1 to @v2.
     */
    public static void straight_line(double[] seg, int x1, double v1, int x2, double v2)
    {
        if (x2 == x1)
        {
            if (v1 != v2)
                throw new IllegalArgumentException();
            seg[x1] = v1;
        }
        else
        {
            for (int x = x1; x <= x2; x++)
                seg[x] = v1 + (x - x1) * (v2 - v1) / (x2 - x1);
        }
    }

    /*
     * Distort array values: y -> ymin + (ymax - ymin) * F((y - ymin)/(ymax - ymin), a).
     * Original array is unchanged, a modified copy is returned.
     * 
     * Values of a > 1 decrease sum and average.
     * Values of a < 1 increase sum and average.
     */
    public static double[] distort(final double[] y, double ymin, double ymax, double a) throws Exception
    {
        if (ymin == ymax)
        {
            return y.clone();
        }
        else
        {
            double[] z = new double[y.length];
            for (int x = 0; x < y.length; x++)
            {
                /*
                 * map_01_basic can cause abrupt twists at the end of the segment because x^a
                 * may have high derivative near 0 or 1. Use map_01_beveled instead, which is more relaxed. 
                 */
                z[x] = ymin + (ymax - ymin) * map_01_beveled((y[x] - ymin) / (ymax - ymin), a);
                z[x] = Util.validate(z[x]);
            }
            return z;
        }
    }

    @SuppressWarnings("unused")
    private static double map_01_basic(double x, double a) throws Exception
    {
        if (!(x >= 0 && x <= 1))
            throw new IllegalArgumentException();
        return Math.pow(x, a);
    }

    @SuppressWarnings("unused")
    private static double map_01_beveled(double x, double a) throws Exception
    {
        if (!(x >= 0 && x <= 1))
            throw new IllegalArgumentException();

        final double margin = 0.2;

        if (x < margin)
        {
            return linear_interpol(x, 0, 0, margin, Math.pow(margin, a));
        }
        else if (x > 1 - margin)
        {
            return linear_interpol(x, 1 - margin, Math.pow(1 - margin, a), 1, 1);
        }
        else
        {
            return Math.pow(x, a);
        }
    }

    private static double linear_interpol(double x, double x1, double y1, double x2, double y2) throws Exception
    {
        if (x1 == x2)
        {
            if (y1 == y2 && x == x1)
                return y1;
            else
                throw new IllegalArgumentException();
        }

        return y1 + (x - x1) * (y2 - y1) / (x2 - x1);
    }

    public static void exportCurveSegmentToClipboard(final double[] curve, int year1, int year2, int ppy) throws Exception
    {
        int x1 = year1 * ppy;
        int x2 = (year2 + 1) * ppy - 1;
        double year = year1;

        StringBuilder sb = new StringBuilder();

        for (int x = x1; x <= x2; x++)
        {
            sb.append(String.format("%.5f %.3f\n", year, curve[x]));
            year += 1.0 / ppy;
        }

        Clipboard.put(sb.toString());
    }
}
