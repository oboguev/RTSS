package rtss.data.mortality.synthetic;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator;

import rtss.math.interpolate.ConstrainedCubicSplineInterpolator;
import rtss.math.interpolate.SteffenSplineInterpolator;
import rtss.util.Util;
import rtss.util.plot.ChartXYSplineAdvanced;

/**
 * Interpolate monotone curve from yearly x-values to daily x-values 
 * such that the curve smoothly passes through the knots.
 * Values in @y refer to the start of the interval.
 * 
 * Interpolation happens to the start of the last year, but not beyond it. 
 *  
 * Typically used to interpolate the "lx" curve.  
 */
public class InterpolateYearlyToDailyAsValuePreservingMonotoneCurve
{
    final static int DAYS_PER_YEAR = 365; 
    
    public static double[] yearly2daily(final double[] y) throws Exception
    {
        final double[] x = new double[y.length];
        for (int k = 0; k < y.length; k++)
            x[k] = k;
        
        double[] yyy1 = null;
        double[] yyy2 = null;
        double[] yyy3 = null;
        double[] xxx = xxx(x);

        if (Util.False)
        {
            UnivariateFunction sp = makeSpline(x, y, SteffenSplineInterpolator.class);
            yyy1 = interpol(sp, x);
        }

        if (Util.False)
        {
            UnivariateFunction sp = makeSpline(x, y, AkimaSplineInterpolator.class);
            yyy2 = interpol(sp, x);
        }

        if (Util.True)
        {
            UnivariateFunction sp = makeSpline(x, y, ConstrainedCubicSplineInterpolator.class);
            yyy3 = interpol(sp, x);
        }
        
        if (Util.False)
        {
            ChartXYSplineAdvanced chart = new ChartXYSplineAdvanced("Make curve", "x", "y");
            if (yyy1 != null)
                chart.addSeries("1", xxx, yyy1);
            if (yyy2 != null)
                chart.addSeries("2", xxx, yyy2);
            if (yyy3 != null)
                chart.addSeries("3", xxx, yyy3);
            chart.addSeriesAsDots("original", x, y);
            chart.display();
        }
        
        double[] yyy = yyy1;
        if (yyy == null)
            yyy = yyy2;
        if (yyy == null)
            yyy = yyy3;
        if (!Util.isPositive(yyy))
            throw new Exception("Error calculating curve (negative or zero value)");
        
        return yyy;
    }
    
    private static double[] interpol(UnivariateFunction sp, double[] x)
    {
        int ndays = (x.length - 1) * DAYS_PER_YEAR;
        double[] yy = new double[ndays];
        for (int day = 0; day < ndays; day++)
            yy[day] = sp.value(day * 1.0 / DAYS_PER_YEAR);
        return yy;
    }

    private static UnivariateFunction makeSpline(final double[] cp_x, final double[] cp_y, Class<?> basicSplineType) throws Exception
    {
        if (basicSplineType == AkimaSplineInterpolator.class)
        {
            return new AkimaSplineInterpolator().interpolate(cp_x, cp_y);
        }
        else if (basicSplineType == SteffenSplineInterpolator.class)
        {
            return new SteffenSplineInterpolator().interpolate(cp_x, cp_y);
        }
        else if (basicSplineType == ConstrainedCubicSplineInterpolator.class)
        {
            return new ConstrainedCubicSplineInterpolator().interpolate(cp_x, cp_y);
        }
        else
        {
            throw new IllegalArgumentException();
        }
    }

    private static double[] xxx(double[] x)
    {
        int ndays = (x.length - 1) * DAYS_PER_YEAR;
        double[] xxx = new double[ndays];
        for (int day = 0; day < ndays; day++)
            xxx[day] = day * 1.0 / DAYS_PER_YEAR;
        return xxx;
    }
}
