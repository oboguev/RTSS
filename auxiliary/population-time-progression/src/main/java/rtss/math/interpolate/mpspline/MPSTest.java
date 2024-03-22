package rtss.math.interpolate.mpspline;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.selectors.Area;
import rtss.util.Util;

public class MPSTest
{
    public static void main(String[] args)
    {
        try
        {
            Bin[] bins = Bins.loadBinsYearly(String.format("ww2losses/%s_census_1959_data.txt", Area.USSR.name()));
            final int ppy = 10;
            double[] yy = MeanPreservingIterativeSpline.eval(bins, ppy);
            Util.noop();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
