package rtss.data.curves;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.util.Util;

public class CurveVerifier
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

    private static boolean error(String title, boolean doThrow, String msg) throws Exception
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
}
