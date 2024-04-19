package rtss.data.curves;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.util.Util;

public class EnsurePositiveCurve
{
    public static double[] ensurePositive(double[] yyy, Bin[] bins) throws Exception
    {
        return new EnsurePositiveCurve().do_ensurePositive(yyy, bins); 
    }
    
    private double[] yyy;
    private Bin[] bins;
    private int ppy;
    private Bin first;
    private Bin last;
    
    private double[] do_ensurePositive(double[] yyy, Bin[] bins) throws Exception
    {
        this.yyy = yyy = yyy.clone();
        this.bins = bins;

        if (Util.isPositive(yyy))
            return yyy;
        
        for (Bin bin : bins)
        {
            if (bin.avg < 0)
                throw new Exception("Unexpected: negative bin");
            else if (bin.avg == 0)
                throw new Exception("Unsupported: bin with zero value");
        }
        
        first = Bins.firstBin(bins);
        last = Bins.lastBin(bins);
        ppy = yyy.length / (last.age_x2 + 1);
        if (yyy.length != ppy * (last.age_x2 + 1))
            throw new IllegalArgumentException();
        
        /* ===================================================================== */
        
        for (Bin bin = first; bin != null; bin = bin.next)
        {
            Bin next = bin.next;
            double[] seg = seg(bin);
            if (Util.isPositive(seg))
                continue;
            
            double sv1 = seg[0];
            double sv2 = seg[seg.length - 1];
            if (sv1 > 0 && sv2 <= 0 && next == null ||
                sv1 > 0 && sv2 > 0 ||
                sv1 <= 0 && sv2 > 0)
            {
                fixOne(bin, seg);
            }
            else if (sv1 > 0 && sv2 <= 0 && next != null)
            {
                double[] nseg = seg(next);
                if (Util.max(nseg) <= 0)
                    throw new Exception("The curve is not fixable");
                double nsv1 = nseg[0];
                double nsv2 = nseg[nseg.length - 1];
                if (nsv1 > 0)
                    fixOne(bin, seg);
                else if (nsv2 <= 0)
                    throw new Exception("Unimplemented, the curve is not fixable now");
                else
                    fixTwo(bin, seg, next, nseg);
            }
            else
            {
                throw new Exception("Unexpected");
            }
        }
        
        if (!Util.isPositive(yyy))
            throw new Exception("Failed to make the curve positive");
        
        return yyy;
    }
    
    /*
     * Extract the segment for the bin from @yyy
     */
    private double[] seg(Bin bin) throws Exception
    {
        return CurveUtil.seg(yyy, bin, bins, ppy);
    }
    
    /*
     * Fix one bin
     */
    private void fixOne(Bin bin, double[] seg) throws Exception
    {
        int x1 = ppy * (bin.age_x1 - first.age_x1);

        double min = Util.min(seg);
        double max = Util.max(seg);
        double new_min = 0.01 * max;

        if (max <= 0)
            throw new Exception("Negative segment in the curve, the fix is not implemented");
        compress_range(seg, min, max, new_min);
        double[] xseg = distort_range(seg, bin.avg * bin.widths_in_years * ppy);
        Util.insert(yyy, xseg, x1);
    }

    /*
     * Fix two consecutive bins [+-] [-+]
     */
    private void fixTwo(Bin bin1, double[] seg1, Bin bin2, double[] seg2) throws Exception
    {
        double min1 = Util.min(seg1);
        double max1 = Util.max(seg1);
        double min2 = Util.min(seg2);
        double max2 = Util.max(seg2);

        double min3 = Math.min(min1, min2);
        double new_min3 = 0.01 * Math.min(max1, max2);

        compress_range(seg1, min3, max1, new_min3);
        compress_range(seg2, min3, max2, new_min3);

        double[] xseg1 = distort_range(seg1, bin1.avg * bin1.widths_in_years * ppy);
        double[] xseg2 = distort_range(seg2, bin2.avg * bin2.widths_in_years * ppy);
        
        int x11 = ppy * (bin1.age_x1 - first.age_x1);
        int x21 = ppy * (bin2.age_x1 - first.age_x1);

        Util.insert(yyy, xseg1, x11);
        Util.insert(yyy, xseg2, x21);
    }
    
    /*
     * Compress the segment y-value range from [min...max] to [new_min...max].
     */
    private void compress_range(double[] seg, double min, double max, double new_min)
    {
        for (int x = 0; x < seg.length; x++)
            seg[x] = new_min + (max - new_min) * (seg[x] - min) / (max - min);
    }
    
    /*
     * Non-linear distortion of segment y-values to pull their range downward,
     * so that the sum is still target_sum
     */
    private double[] distort_range(double[] seg, double target_sum) throws Exception
    {
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
            double a = (a1 + a2) /2;
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

    /*
     * Non-linear distortion of segment y-values to pull their range downward
     */
    private double[] distort(double[] seg, double a)
    {
        double max = Util.max(seg);
        double[] xseg = Util.dup(seg);

        for (int x = 0; x < seg.length; x++)
            xseg[x] = max * Math.pow(seg[x] / max, a);

        return xseg;
    }
}
