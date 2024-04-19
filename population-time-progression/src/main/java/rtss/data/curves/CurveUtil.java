package rtss.data.curves;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
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
}
