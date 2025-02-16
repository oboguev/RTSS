package rtss.data.curves;

import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.curves.ensure.EnsureNonNegativeCurve;
import rtss.data.curves.refine.RefineYearlyPopulation;
import rtss.data.population.struct.Population;
import rtss.data.selectors.Gender;
import rtss.math.interpolate.ConstrainedCubicSplineInterpolator;
import rtss.math.interpolate.SteffenSplineInterpolator;
import rtss.math.interpolate.TargetPrecision;
import rtss.math.interpolate.disaggregate.DisaggregateConstantWidthSeries;
import rtss.math.interpolate.disaggregate.DisaggregateVariableWidthSeries;
import rtss.math.interpolate.mpspline.MeanPreservingIntegralSpline;
import rtss.math.interpolate.mpspline.MeanPreservingIterativeSpline;
import rtss.math.pclm.PCLM_Rizzi_2015;
import rtss.util.Util;
import rtss.util.plot.ChartXYSplineAdvanced;

/**
 * Interpolate aggregated bins to a smooth yearly curve, in a mean-preserving way.
 * Typically used to interpolate population from an aggregated multi-year data to a yearly resolution.
 * Or to a daily resolution, for use by PopulationContext.
 */
public class InterpolatePopulationAsMeanPreservingCurve
{
    public static final int MAX_AGE = Population.MAX_AGE;
    
    public static class CurveResult
    {
        public final double[] curve;
        public final double[] raw;
        
        public CurveResult(double[] curve)
        {
            this.curve = curve;
            this.raw = null;
        }

        public CurveResult(double[] curve, double[] raw)
        {
            this.curve = curve;
            this.raw = raw;
        }
    }

    public static double[] curve(Bin[] bins, String title, TargetResolution targetResolution, Integer yearHint, Gender gender) throws Exception
    {
        // curve_osier(bins, "method", "", title);
        // return curve_pclm(bins, title);

        Exception ex = null;
        CurveResult curve = null;

        try
        {
            /*
             * При интерполяции данных для TargetResolution.DAILY алгоритм CSASRA даёт гораздо
             * более гладкие данные, чем алгоритм сплайна.
             */
            if (curve == null && Util.True)
                curve = curve_csasra(bins, title, targetResolution, yearHint, gender);
        }
        catch (Exception e2)
        {
            Util.err("CSASRA disaggregation failed for " + title);
            ex = e2;
        }

        try
        {
            if (curve == null && Util.True)
                curve = curve_spline(bins, title, targetResolution, yearHint, gender);
        }
        catch (Exception e2)
        {
            Util.err("Spline disaggregation failed for " + title);
            if (ex == null)
                ex = e2;
        }

        if (curve == null)
        {
            if (ex != null)
                throw ex;
            else
                throw new Exception("Unable to build the curve");
        }

        if (Util.True)
        {
            int ppy = 1;
            double[] xxx = Bins.ppy_x(bins, ppy);
            ChartXYSplineAdvanced chart = new ChartXYSplineAdvanced(title, "x", "y").showSplinePane(false);
            
            chart.addSeries("final", xxx, curve.curve);

            if (curve.raw != null && Util.differ(curve.curve, curve.raw))
                chart.addSeries("raw", xxx, curve.raw);
            
            chart.addSeries("bins", xxx, Bins.ppy_y(bins, ppy));
            
            chart.display();
            Util.noop();
        }

        return curve.curve;
    }

    /* ================================================================================================ */

    private static CurveResult curve_csasra(Bin[] bins, String title, TargetResolution targetResolution, Integer yearHint, Gender gender)
            throws Exception
    {
        final int ppy = 1;
        double[] xxx = Bins.ppy_x(bins, ppy);
        double[] averages = Bins.midpoint_y(bins);

        int[] intervalWidths = Bins.widths(bins);
        int maxIterations = 5000;
        double positivityThreshold = 1e-6;
        double maxConvergenceDifference = 1e-3;

        /*
         * For yearly data (targetResolution == YEARLY) use sigma around 1.0 (0.5-1.5).
         * Difference of results within this range is typically miniscule.
         * 
         * For daily data (targetResolution == DAILY) use sigma around 10.0.
         */
        double smoothingSigma;
        switch (targetResolution)
        {
        case YEARLY:
            smoothingSigma = 1.0;
            break;

        case DAILY:
            smoothingSigma = 10.0;
            break;

        default:
            throw new IllegalArgumentException();
        }

        double[] yyy = DisaggregateVariableWidthSeries.disaggregate(averages,
                                                                    intervalWidths,
                                                                    maxIterations,
                                                                    smoothingSigma,
                                                                    positivityThreshold,
                                                                    maxConvergenceDifference);

        if (Util.False)
        {
            ChartXYSplineAdvanced chart = new ChartXYSplineAdvanced(title, "x", "y").showSplinePane(false);
            chart.addSeries("csasra", xxx, yyy);
            chart.addSeries("bins", xxx, Bins.ppy_y(bins, ppy));
            chart.display();
            Util.noop();
        }

        if (!Util.isNonNegative(yyy))
            throw new Exception("Error calculating curve (negative value)");

        double[] yy = Bins.ppy2yearly(yyy, ppy);

        if (!Util.isNonNegative(yy))
            throw new Exception("Error calculating curve (negative value)");

        CurveVerifier.validate_means(yy, bins);
        
        if (targetResolution == TargetResolution.YEARLY)
        {
            /* уточнить разбивку на возраста 0-9 */
            double[] raw = Util.dup(yy);
            yy = RefineYearlyPopulation.refine(bins, title, yy, yearHint, gender);
            CurveVerifier.validate_means(yy, bins);
            return new CurveResult(yy, raw);
        }
        else
        {
            return new CurveResult(yy);
        }
    }

    @SuppressWarnings("unused")
    private static double[] curve_csasra_fixed(Bin[] bins, String title, TargetResolution targetResolution) throws Exception
    {
        if (!Bins.isEqualWidths(bins))
            throw new IllegalArgumentException();

        final int ppy = 1;
        double[] xxx = Bins.ppy_x(bins, ppy);
        double[] sss = Bins.midpoint_y(bins);

        int restoredPoints = Bins.lastBin(bins).age_x2 + 1;
        int samplinglSize = Bins.firstBin(bins).widths_in_years;
        int numIterations = 2000;
        double smoothingSigma = 50.0;
        double positivityThreshold = 1e-6;

        double[] yyy = DisaggregateConstantWidthSeries.disaggregate(sss, restoredPoints, samplinglSize, numIterations, smoothingSigma,
                                                                    positivityThreshold);

        if (Util.False)
        {
            ChartXYSplineAdvanced chart = new ChartXYSplineAdvanced(title, "x", "y").showSplinePane(false);
            chart.addSeries("1", xxx, yyy);
            chart.addSeries("bins", xxx, Bins.ppy_y(bins, ppy));
            chart.display();
            Util.noop();
        }

        return yyy;
    }

    /* ================================================================================================ */

    /*
     * Spline implementation
     */
    private static CurveResult curve_spline(Bin[] bins, String title, TargetResolution targetResolution, Integer yearHint, Gender gender)
            throws Exception
    {
        TargetPrecision precision = new TargetPrecision().eachBinRelativeDifference(0.001);
        MeanPreservingIterativeSpline.Options options = new MeanPreservingIterativeSpline.Options()
                .checkPositive(false);

        if (Util.True)
        {
            /*
             * Helps to avoid the last segment of the curve dive down too much
             */
            options = options.placeLastBinKnotAtRightmostPoint();
        }

        int ppy = 12;

        if (targetResolution == TargetResolution.DAILY)
            ppy = 1;

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

        yyy = EnsureNonNegativeCurve.ensureNonNegative(yyy, bins, title, targetResolution);

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

        CurveVerifier.validate_means(yy, bins);

        if (targetResolution == TargetResolution.YEARLY)
        {
            double[] raw = Util.dup(yy);
            /* уточнить разбивку на возраста 0-9 */
            yy = RefineYearlyPopulation.refine(bins, title, yy, yearHint, gender);
            CurveVerifier.validate_means(yy, bins);
            return new CurveResult(yy, raw);
        }
        else
        {
            return new CurveResult(yy);
        }
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

        CurveVerifier.validate_means(yy, bins);

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
            String chartTitle = "Osier curve (" + method + ") " + title;
            ViewCurve.view(chartTitle, bins, yy);
        }
        double[] y = Bins.ppy2yearly(yy, ppy);
        // will fail here
        // CurveVerifier.validate_means(y, bins);
        return y;
    }
}
