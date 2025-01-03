package rtss.data.curves;

import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.population.Population;
import rtss.math.interpolate.ConstrainedCubicSplineInterpolator;
import rtss.math.interpolate.SteffenSplineInterpolator;
import rtss.math.interpolate.TargetPrecision;
import rtss.math.interpolate.mpspline.MeanPreservingIntegralSpline;
import rtss.math.interpolate.mpspline.MeanPreservingIterativeSpline;
import rtss.math.pclm.PCLM_Rizzi_2015;
import rtss.util.Util;
import rtss.util.plot.ChartXYSplineAdvanced;

/**
 * Interpolate aggregated bins to a smooth yearly curve, in a mean-preserving way.
 * Typically used to interpolate population from an aggregated multi-year data to a yearly resolution.
 */
public class InterpolatePopulationAsMeanPreservingCurve
{
    public static final int MAX_AGE = Population.MAX_AGE;

    public static double[] curve(Bin[] bins, String title) throws Exception
    {
        // curve_osier(bins, "method", "", title);
        // return curve_pclm(bins, title);
        return curve_spline(bins, title);
    }
    
    /*
     * Spline implementation
     */
    private static double[] curve_spline(Bin[] bins, String title) throws Exception
    {
        TargetPrecision precision = new TargetPrecision().eachBinRelativeDifference(0.001);
        MeanPreservingIterativeSpline.Options options = new MeanPreservingIterativeSpline.Options()
                .checkPositive(false);

        if (Util.True)
        {
            /*
             * Helps to avoid the last segment of the curve dive down to much
             */
            options = options.placeLastBinKnotAtRightmostPoint();
        }

        int ppy = 12;
        double[] xxx = Bins.ppy_x(bins, ppy);
        double[] yyy1 = null;
        double[] yyy2 = null;
        double[] yyy3 = null;
        double[] yyy4 = null;
        double[] yyy5 = null;

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
        
        if (Util.False)
        {
            MeanPreservingIntegralSpline.Options xoptions = new MeanPreservingIntegralSpline.Options();
            xoptions = xoptions.ppy(ppy).debug_title(title).basicSplineType(ConstrainedCubicSplineInterpolator.class);
            xoptions = xoptions.splineParams("title", title);
            // do not use f2.trends since it over-determines the spline and makes value of s' discontinuous between segments 
            // options = options.splineParams("f2.trends", trends);
            yyy4 = MeanPreservingIntegralSpline.eval(bins, xoptions);
        }

        if (Util.False)
        {
            final double lambda = 0.0001;
            yyy5 = PCLM_Rizzi_2015.pclm(bins, lambda, ppy);
        }

        double[] yyy = yyy1;
        if (yyy == null)
            yyy = yyy2;
        if (yyy == null)
            yyy = yyy3;
        if (yyy == null)
            yyy = yyy4;
        if (yyy == null)
            yyy = yyy5;
        
        yyy = EnsureNonNegativeCurve.ensureNonNegative(yyy, bins);
        
        if (Util.False)
        {
            ChartXYSplineAdvanced chart = new ChartXYSplineAdvanced(title, "x", "y").showSplinePane(false);
            if (yyy1 != null)
                chart.addSeries("1", xxx, yyy1);
            if (yyy2 != null)
                chart.addSeries("2", xxx, yyy2);
            if (yyy3 != null)
                chart.addSeries("3", xxx, yyy3);
            if (yyy4 != null)
                chart.addSeries("4", xxx, yyy4);
            if (yyy5 != null)
                chart.addSeries("5", xxx, yyy5);
            chart.addSeries("bins", xxx, Bins.ppy_y(bins, ppy));
            chart.display();
        }

        if (!Util.isNonNegative(yyy))
            throw new Exception("Error calculating curve (negative value)");

        double[] yy = Bins.ppy2yearly(yyy, ppy);

        if (!Util.isNonNegative(yy))
            throw new Exception("Error calculating curve (negative value)");

        validate_means(yy, bins);

        return yy;
    }

    /*
     * PCLM implementation
     */
    @SuppressWarnings("unused")
    private static double[] curve_pclm(Bin[] bins, String title) throws Exception
    {
        int ppy = 12;
        double[] xxx = Bins.ppy_x(bins, ppy);

        final double lambda = 0.0001;
        double[] yyy = PCLM_Rizzi_2015.pclm(bins, lambda, ppy);

        if (Util.True)
        {
            ViewCurve.view(title, bins, "pclm", yyy);
        }

        if (!Util.isNonNegative(yyy))
            throw new Exception("Error calculating curve (negative value)");

        double[] yy = Bins.ppy2yearly(yyy, ppy);

        if (!Util.isNonNegative(yy))
            throw new Exception("Error calculating curve (negative value)");

        validate_means(yy, bins);

        return yy;
    }
    
    @SuppressWarnings("unused")
    private static double[] curve_osier(Bin[] bins, String method, String params, String title) throws Exception
    {
        int ppy = 1;
        if (params != null && params.length() != 0)
            method += ":\"" + params + "\"";
        double[] yy = OsierTask.population(bins, "XXX", method, ppy);
        if (Util.True)
        {
            String chartTitle = "Osier curve (" + method + ") "+ title;
            ViewCurve.view(chartTitle, bins, yy);
        }
        double[] y = Bins.ppy2yearly(yy, ppy);
        // will fail here
        // CurveVerifier.validate_means(y, bins);
        return y;
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
