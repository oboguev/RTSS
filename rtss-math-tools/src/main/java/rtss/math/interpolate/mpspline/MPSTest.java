package rtss.math.interpolate.mpspline;

import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.selectors.Area;
import rtss.math.interpolate.ConstrainedCubicSplineInterpolator;
import rtss.math.interpolate.SteffenSplineInterpolator;
import rtss.math.interpolate.TargetPrecision;
import rtss.util.Util;

public class MPSTest
{
    public static void main(String[] args)
    {
        try
        {
            Bin[] bins = Bins.loadBinsYearly(String.format("ww2losses/%s_census_1959_data.txt", Area.RSFSR.name()));
            final int ppy = 10;
            double[] yy;

            MeanPreservingIterativeSpline.Options options = new MeanPreservingIterativeSpline.Options();
            TargetPrecision precision = new TargetPrecision().eachBinAbsoluteDifference(0.1);

            options.basicSplineType(SteffenSplineInterpolator.class);
            yy = MeanPreservingIterativeSpline.eval(bins, ppy, options, precision);

            options.basicSplineType(AkimaSplineInterpolator.class);
            yy = MeanPreservingIterativeSpline.eval(bins, ppy, options, precision);

            options.basicSplineType(ConstrainedCubicSplineInterpolator.class);
            yy = MeanPreservingIterativeSpline.eval(bins, ppy, options, precision);
            
            Util.unused(yy);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
