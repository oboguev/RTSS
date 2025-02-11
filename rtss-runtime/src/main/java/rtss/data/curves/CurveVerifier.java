package rtss.data.curves;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.util.Util;

public class CurveVerifier
{
    /*
     * Verify that the curve preserves mean values as indicated by the bins
     */
    public static void validate_means(Bin[] bins, double[] yy) throws Exception
    {
        validate_means(yy, bins);
    }
    
    public static void validate_means(double[] yy, Bin[] bins) throws Exception
    {
        int ppy = CurveUtil.ppy(yy, bins);
        
        for (Bin bin : bins)
        {
            double[] y = Util.splice(yy, bin.x1(ppy), bin.x2(ppy));
            if (Util.differ(Util.average(y), bin.avg, 0.001))
                throw new Exception("Curve does not preserve mean values of the bins");
        }
    }
    
    /*
     * Verify that the bins data is U-shaped
     */
    public static boolean verifyUShape(Bin[] bins, boolean strict, String title, boolean doThrow) throws Exception
    {
        boolean was_down = false;
        boolean was_up = false;
        
        for (Bin bin : bins)
        {
            Bin prev = bin.prev;
            if (prev != null)
            {
                if (bin.avg == prev.avg)
                {
                    if (strict)
                        return error(title, doThrow, "Mortality bins are not U-shaped at " + bin);
                }
                else if (bin.avg < prev.avg)
                {
                    if (was_up)
                        return error(title, doThrow, "Mortality bins are not U-shaped at " + bin);
                    was_down = true;
                }
                else // if (bin.avg > prev.avg)
                {
                    if (!was_down)
                        return error(title, doThrow, "Mortality bins are not U-shaped at " + bin);
                    was_up = true;
                }
            }
        }
        
        return true;
    }
    
    public static boolean verifyUShape(Bin[] bins, String title, boolean doThrow) throws Exception
    {
        if (Bins.flips(bins) > 1)
            return error(title, doThrow, "Mortality curve has multiple local minumums and maximums");

        Bin firstBin = Bins.firstBin(bins);
        Bin lastBin = Bins.lastBin(bins);

        Bin maxBin = Bins.findMaxBin(bins);
        if (maxBin != firstBin && maxBin != lastBin)
            return error(title, doThrow, "Mortality curve has an unexpected maximum in the middle");

        // minimum value bin
        Bin minBin = Bins.findMinBin(bins);
        if (minBin == firstBin || minBin == lastBin)
            return error(title, doThrow, "Mortality minimum is not in the middle");
        
        return true;
    }

    static boolean error(String title, boolean doThrow, String msg) throws Exception
    {
        msg += ": " + title;
        if (doThrow)
        {
            throw new Exception(msg);
        }
        else
        {
            Util.err(msg);
            return false;
        }
    }
    
    /*
     * Verify that curve values are positive
     */
    public static boolean positive(double[] curve, Bin[] bins, String title, boolean doThrow) throws Exception
    {
        final int ppy = CurveUtil.ppy(curve, bins);
        StringBuilder sb = new StringBuilder();

        Bin recent = null;
        
        for (int x = 0; x < curve.length; x++)
        {
            if (curve[x] <= 0)
            {
                Bin bin = CurveUtil.x2bin(x, ppy, bins);
                if (bin != recent)
                {
                    recent = bin;
                    if (sb.length() != 0)
                        sb.append(" ");
                    if (bin.age_x1 == bin.age_x2)
                        sb.append(String.format("%d", bin.age_x1));
                    else
                        sb.append(String.format("%d-%d", bin.age_x1, bin.age_x2));
                }
            }
        }
        
        if (recent != null)
        {
            String msg = String.format("Nevative or zero-value segments in %s at ages %s", title, sb.toString());
            if (doThrow)
                throw new Exception(msg);
            Util.err(msg);
            return false;
        }

        return true;
    }

    /*
     * Verify that the curve is U-shaped
     */
    public static boolean verifyUShape(double[] curve, Bin[] bins, String title, boolean doThrow) throws Exception
    {
        return verifyUShape(curve, bins, true, title, doThrow);
    }
    
    public static boolean verifyUShape(double[] curve, Bin[] bins, boolean strict, String title, boolean doThrow) throws Exception
    {
        final int ppy = CurveUtil.ppy(curve, bins);
        StringBuilder sb = new StringBuilder();
        
        Bin minBin1 = null;
        Bin minBin2 = null;
        Bin lastMinBin = null;

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
                    return error(title, doThrow, "Long chain of mimimum bins");
                minBin2 = bin;
            }
        }
        
        lastMinBin = minBin1;
        if (minBin2 != null)
            lastMinBin = minBin2;
        
        boolean inflected = false;
        Bin recentErrorBin = null;
        double tolerance = 1.0;
        if (!strict)
            tolerance = 1.0;
        
        for (int x = 1; x < curve.length; x++)
        {
            Bin bin = CurveUtil.x2bin(x, ppy, bins);
            boolean error = false;
            
            if (bin.index < minBin1.index)
            {
                // must be trending down
                if (curve[x] > tolerance * curve[x - 1])
                    error = true;
            }
            else if (bin.index > lastMinBin.index)
            {
                // must be trending up
                if (tolerance * curve[x] < curve[x - 1])
                    error = true;
            }
            else if (bin == minBin1 || bin == minBin2)
            {
                // must be trending down before the inflection point,
                // then trending up
                if (!inflected)
                {
                    if (curve[x] > tolerance * curve[x - 1])
                        inflected = true;
                }
                else
                {
                    // must be trending up
                    if (tolerance * curve[x] < curve[x - 1])
                        error = true;
                }
            }
            
            if (error && bin != recentErrorBin)
            {
                recentErrorBin = bin;
                if (sb.length() != 0)
                    sb.append(" ");
                sb.append(binRange(bin));
            }
        }
        
        if (recentErrorBin != null)
        {
            List<List<Integer>> xlist = locateContinuousNonMonotonicPoints(curve, bins, title, tolerance, null);
            String desc = describeContinuousNonMonotonicPoints(xlist);
            
            String minsegs = binRange(minBin1);
            if (minBin2 != null)
                minsegs += " " + binRange(minBin2);
            
            String msg = String.format("Non-monotonic segments in %s at ranges %s and ages %s, minimum segments: %s", 
                                       title, sb.toString(), desc, minsegs);
            if (doThrow)
                throw new Exception(msg);
            Util.err(msg);
            return false;
        }
        
        return true;
    }
    
    private static String binRange(Bin bin)
    {
        if (bin.age_x1 == bin.age_x2)
            return String.format("%d", bin.age_x1);
        else
            return String.format("%d-%d", bin.age_x1, bin.age_x2);
    }

    /*
     * @inflection returned as the minimum point before raise and can be the last point *before* the minimum bin
     */
    public static List<Integer> locateNonMonotonicPoints(double[] curve, Bin[] bins, String title, double tolerance, MutableInt inflection) throws Exception
    {
        final int ppy = CurveUtil.ppy(curve, bins);
        List<Integer> list = new ArrayList<>();
        
        Bin minBin1 = null;
        Bin minBin2 = null;
        Bin lastMinBin = null;

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
                    error(title, true, "Long chain of mimimum bins");
                minBin2 = bin;
            }
        }
        
        lastMinBin = minBin1;
        if (minBin2 != null)
            lastMinBin = minBin2;
        
        boolean inflected = false;
        double lastv = curve[0];
        
        for (int x = 1; x < curve.length; x++)
        {
            Bin bin = CurveUtil.x2bin(x, ppy, bins);
            
            if (bin.index < minBin1.index)
            {
                // must be trending down
                if (curve[x] > tolerance * lastv)
                    list.add(x);
                else 
                    lastv = curve[x];
            }
            else if (bin.index > lastMinBin.index)
            {
                // must be trending up
                if (tolerance * curve[x] < lastv)
                    list.add(x);
                else 
                    lastv = curve[x];
            }
            else if (bin == minBin1 || bin == minBin2)
            {
                // must be trending down before the inflection point,
                // then trending up
                if (!inflected)
                {
                    if (curve[x] > tolerance * curve[x - 1])
                    {
                        inflected = true;
                        if (inflection != null)
                            inflection.setValue(x - 1);
                    }
                    lastv = curve[x];
                }
                else
                {
                    // must be trending up
                    if (tolerance * curve[x] < lastv)
                        list.add(x);
                    else
                        lastv = curve[x];
                }
            }
        }
        
        return list;
    }
    
    public static List<List<Integer>> locateContinuousNonMonotonicPoints(double[] curve, Bin[] bins, String title, double tolerance, MutableInt inflection) throws Exception
    {
        List<List<Integer>> xlist = new ArrayList<>();
        
        Integer last_x = null;
        List<Integer> list = null;
        
        for (int x : locateNonMonotonicPoints(curve, bins, title, tolerance, inflection)) 
        {
            if (last_x != null && x == last_x + 1)
            {
                list.add(x);
            }
            else
            {
                list = new ArrayList<Integer>();
                list.add(x);
                xlist.add(list);
            }
            last_x = x;
        }
        
        return xlist;
    }

    public static String describeContinuousNonMonotonicPoints(List<List<Integer>> xlist)
    {
        StringBuilder sb = new StringBuilder();
        for (List<Integer> list : xlist)
        {
            String s = "" + list.get(0);
            if (list.size() != 1)
                s += "-" + list.get(list.size() - 1);
            
            if (sb.length() != 0)
                sb.append(" ");
            sb.append(s);
        }
        return sb.toString();
    }
}
