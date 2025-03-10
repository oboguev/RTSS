package rtss.data.curves;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.util.Clipboard;
import rtss.util.Util;

public class CurveUtil
{
    /*
     * Estimate ppy for the curve
     */
    public static int ppy(double[] curve, Bin[] bins) throws Exception
    {
        Bin last = Bins.lastBin(bins);
        int ppy = curve.length / (last.age_x2 + 1);
        if (curve.length != ppy * (last.age_x2 + 1))
            throw new IllegalArgumentException();
        return ppy;
    }

    /*
     * Get bin corresponding to x-point
     */
    public static Bin x2bin(int x, int ppy, Bin[] bins) throws Exception
    {
        int year = (x / ppy) + Bins.firstBin(bins).age_x1;
        return Bins.binForAge(year, bins);
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
     * Distort the curve to match the sum
     */
    public static double[] distort_matchsum(final double[] y, double ymin, double ymax, double targetSum) throws Exception
    {
        double[] v = y.clone();
        double a1, a2;

        if (Util.sum(y) < targetSum)
        {
            // keep decreasing a (and increasing sum)
            double a = 1.0;

            while (Util.sum(v) < targetSum)
            {
                a *= 0.7;
                if (a < 1.0 / 200)
                    throw new Exception("Unable distort the curve: a is out of range");
                v = distort(y, ymin, ymax, a);
            }
            
            a1 = a;
            a2 = 1.0;
        }
        else if (Util.sum(y) > targetSum)
        {
            // keep increasing a (and decreasing sum)
            double a = 1.0;

            while (Util.sum(v) > targetSum)
            {
                a *= 1.5;
                if (a >= 200)
                    throw new Exception("Unable distort the curve: a is out of range");
                v = distort(y, ymin, ymax, a);
            }
            
            a1 = 1.0;
            a2 = a;
        }
        else
        {
            return y.clone();
        }
        
        for (;;)
        {
            double a = (a1 + a2) / 2;
            
            v = distort(y, ymin, ymax, a);
            double vsum = Util.sum(v);
            
            if (Util.same(vsum, targetSum) && Math.abs(vsum - targetSum) < 0.5)
                return v;
            
            if (Util.same(a1, a2))
                throw new Exception("Unable distort the curve: out of cycles");
            
            if (vsum < targetSum)
            {
                // keep decreasing a (and increasing sum)
                a2 = a;
            }
            else if (vsum > targetSum)
            {
                // keep increasing a (and decreasing sum)
                a1 = a;
            }
            else
            {
                return v;
            }
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
        Util.assertion(ymax >= ymin);

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

    public static double[] fill_linear(int x1, double y1, int x2, double y2) throws Exception
    {
        Util.assertion(x2 > x1 || x2 == x1 && y2 == y1);
        double[] v = new double[x2 - x1 + 1];

        for (int k = 0; k < v.length; k++)
            v[k] = linear_interpol(x1 + k, x1, y1, x2, y2);

        return v;
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

    /*
     * If bins are not U-shaped, throw an exception.
     * If bins are U-shaped, return trends for all segments.
     */
    public static CurveSegmentTrend[] getUShapeSegmentTrends(Bin[] bins, String title) throws Exception
    {
        CurveVerifier.verifyUShape(bins, false, title, true);

        Bin minBin1 = null;
        Bin minBin2 = null;

        for (Bin bin : bins)
        {
            if (minBin1 == null)
            {
                minBin1 = bin;
            }
            else if (bin.avg < minBin1.avg)
            {
                minBin1 = bin;
                minBin2 = null;
            }
            else if (bin.avg == minBin1.avg)
            {
                if (bin != minBin1.next)
                    throw new Exception("Long chain of mimimum bins at " + title);
                minBin2 = bin;
            }
        }

        CurveSegmentTrend[] trends = new CurveSegmentTrend[bins.length];
        for (Bin bin : bins)
        {
            if (bin.index < minBin1.index)
                trends[bin.index] = CurveSegmentTrend.DOWN;
            else if (bin == minBin1 && minBin2 == null)
                trends[bin.index] = CurveSegmentTrend.MIN;
            else if (bin == minBin1 && minBin2 != null)
                trends[bin.index] = CurveSegmentTrend.MIN1;
            else if (bin == minBin2)
                trends[bin.index] = CurveSegmentTrend.MIN2;
            else
                trends[bin.index] = CurveSegmentTrend.UP;
        }

        return trends;
    }
}
