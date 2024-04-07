package rtss.data.mortality.synthetic;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.math.interpolate.ConstrainedCubicSplineInterpolator;
import rtss.math.interpolate.SteffenSplineInterpolator;
import rtss.math.interpolate.TargetPrecision;
import rtss.math.interpolate.mpspline.MeanPreservingIterativeSpline;
import rtss.util.Util;
import rtss.util.plot.ChartXYSplineAdvanced;

/**
 * Interpolate monotone curve from yearly x-values to daily x-values in a mean-preserving way.
 * Typically used to interpolate the "lx" curve.  
 */
public class YearlyToDailyMonotoneCurve
{
    public static double[] yearly2daily(final double[] y) throws Exception
    {
        final int DAYS_PER_YEAR = 365; 

        List<Bin> list = new ArrayList<>();
        for (int year = 0; year  < y.length; year++)
            list.add(new Bin(year, year, y[year]));

        Bin[] bins = Bins.bins(list);

        TargetPrecision precision = new TargetPrecision().eachBinRelativeDifference(0.001);
        MeanPreservingIterativeSpline.Options options = new MeanPreservingIterativeSpline.Options()
                .checkPositive(false);

        double[] xxx = Bins.ppy_x(bins, DAYS_PER_YEAR);
        double[] yyy1 = null;
        double[] yyy2 = null;
        double[] yyy3 = null;

        if (Util.False)
        {
            options.basicSplineType(SteffenSplineInterpolator.class);
            yyy1 = MeanPreservingIterativeSpline.eval(bins, DAYS_PER_YEAR, options, precision);
        }

        if (Util.False)
        {
            options.basicSplineType(AkimaSplineInterpolator.class);
            yyy2 = MeanPreservingIterativeSpline.eval(bins, DAYS_PER_YEAR, options, precision);
        }

        if (Util.True)
        {
            options.basicSplineType(ConstrainedCubicSplineInterpolator.class);
            yyy3 = MeanPreservingIterativeSpline.eval(bins, DAYS_PER_YEAR, options, precision);
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
            chart.addSeries("bins", xxx, Bins.ppy_y(bins, DAYS_PER_YEAR));
            chart.display();
        }
        
        double[] yyy = yyy1;
        if (yyy == null)
            yyy = yyy2;
        if (yyy == null)
            yyy = yyy3;
        if (!Util.isPositive(yyy))
            throw new Exception("Error calculating curve (negative or zero value)");
        
        double[] yy = Bins.ppy2yearly(yyy, DAYS_PER_YEAR);

        MakeCurve.validate_means(yy, bins);

        return yy;
    }
}
