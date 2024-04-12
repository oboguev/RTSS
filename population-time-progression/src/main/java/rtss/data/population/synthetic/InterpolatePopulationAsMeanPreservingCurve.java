package rtss.data.population.synthetic;

import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.population.Population;
import rtss.math.interpolate.ConstrainedCubicSplineInterpolator;
import rtss.math.interpolate.SteffenSplineInterpolator;
import rtss.math.interpolate.TargetPrecision;
import rtss.math.interpolate.mpspline.MeanPreservingIterativeSpline;
import rtss.util.Util;
import rtss.util.plot.ChartXYSplineAdvanced;

/**
 * Interpolate aggregated bins to a smooth yearly curve, in a mean-preserving way.
 * Typically used to interpolate population from an aggregated multi-year data to a yearly resolution.
 */
public class InterpolatePopulationAsMeanPreservingCurve
{
    public static final int MAX_AGE = Population.MAX_AGE;

    public static double[] curve(Bin... bins) throws Exception
    {
        TargetPrecision precision = new TargetPrecision().eachBinRelativeDifference(0.001);
        MeanPreservingIterativeSpline.Options options = new MeanPreservingIterativeSpline.Options()
                .checkPositive(false);

        int ppy = 12;
        double[] xxx = Bins.ppy_x(bins, ppy);
        double[] yyy1 = null;
        double[] yyy2 = null;
        double[] yyy3 = null;

        if (Util.False)
        {
            options.basicSplineType(SteffenSplineInterpolator.class);
            yyy1 = MeanPreservingIterativeSpline.eval(bins, ppy, options, precision);
        }

        if (Util.False)
        {
            options.basicSplineType(AkimaSplineInterpolator.class);
            yyy2 = MeanPreservingIterativeSpline.eval(bins, ppy, options, precision);
        }

        if (Util.True)
        {
            options.basicSplineType(ConstrainedCubicSplineInterpolator.class);
            yyy3 = MeanPreservingIterativeSpline.eval(bins, ppy, options, precision);
        }
        
        if (Util.True)
        {
            ChartXYSplineAdvanced chart = new ChartXYSplineAdvanced("Make curve", "x", "y");
            if (yyy1 != null)
                chart.addSeries("1", xxx, yyy1);
            if (yyy2 != null)
                chart.addSeries("2", xxx, yyy2);
            if (yyy3 != null)
                chart.addSeries("3", xxx, yyy3);
            chart.addSeries("bins", xxx, Bins.ppy_y(bins, ppy));
            chart.display();
        }
        
        double[] yyy = yyy1;
        if (yyy == null)
            yyy = yyy2;
        if (yyy == null)
            yyy = yyy3;
        if (!Util.isPositive(yyy))
            throw new Exception("Error calculating curve (negative or zero value)");
        
        double[] yy = Bins.ppy2yearly(yyy, ppy);

        validate_means(yy, bins);

        return yy;
    }
    
    /*
     * Verify that the curve preserves mean values as indicated by the bins
     */
    static void validate_means(double[] yy, Bin... bins) throws Exception
    {
        for (Bin bin : bins)
        {
            double[] y = Util.splice(yy, bin.age_x1, bin.age_x2);
            if (Util.differ(Util.average(y), bin.avg, 0.001))
                throw new Exception("Curve does not preserve mean values of the bins");
        }
    }
}
