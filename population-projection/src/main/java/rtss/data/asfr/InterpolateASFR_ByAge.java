package rtss.data.asfr;

import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.curves.CurveVerifier;
import rtss.data.curves.TargetResolution;
import rtss.data.curves.ensure.EnsureNonNegativeCurve;
import rtss.math.interpolate.ConstrainedCubicSplineInterpolator;
import rtss.math.interpolate.SteffenSplineInterpolator;
import rtss.math.interpolate.TargetPrecision;
import rtss.math.interpolate.disaggregate.csasra.DisaggregateVariableWidthSeries;
import rtss.math.interpolate.mpspline.MeanPreservingIntegralSpline;
import rtss.math.interpolate.mpspline.MeanPreservingIterativeSpline;
import rtss.math.pclm.PCLM_Rizzi_2015;
import rtss.util.Util;
import rtss.util.plot.ChartXYSplineAdvanced;

/*
 * Возрастная интерполяция возрастных коэффициентов плодовитости 
 *   - из агрегированного по возрасту вида  
 *   - в годовое по возрасту разрешение.
 *    
 * Интерполяция производится в предположении равномерного распределения числа женщин по возрасту
 * внутри агрегированной возрастной группы.
 */
public class InterpolateASFR_ByAge
{
    public static AgeSpecificFertilityRates interpolate(AgeSpecificFertilityRates asfr) throws Exception
    {
        String title = "ASFR";
        Bin[] bins = asfr.binsReadonly();
        CurveResult curve = null;
        CurveResult curve_csasra = null;
        CurveResult curve_spline = null;

        if (Util.True)
            curve_csasra = curve = curve_csasra(bins);

        if (Util.False)
            curve_spline = curve = curve_spline(bins);

        if (Util.False)
        {
            int ppy = 1;
            double[] xxx = Bins.ppy_x(bins, ppy);
            ChartXYSplineAdvanced chart = new ChartXYSplineAdvanced(title, "x", "y").showSplinePane(false);
            if (curve_csasra != null)
                chart.addSeries("CSASRA", xxx, curve_csasra.curve);
            if (curve_spline != null)
                chart.addSeries("SPLINE", xxx, curve_spline.curve);
            chart.addSeries("bins", xxx, Bins.ppy_y(bins, ppy));
            chart.display();
        }
        
        Bin[] xbins = Bins.fromYearlyValues(bins[0].age_x1, curve.curve);
        
        return new AgeSpecificFertilityRates(xbins);
    }

    private static class CurveResult
    {
        @SuppressWarnings("unused")
        public final String method;

        @SuppressWarnings("unused")
        public final double[] curve;

        @SuppressWarnings("unused")
        public final double[] raw;

        public CurveResult(String method, double[] curve)
        {
            this.method = method;
            this.curve = curve;
            this.raw = null;
        }

        @SuppressWarnings("unused")
        public CurveResult(String method, double[] curve, double[] raw)
        {
            this.method = method;
            this.curve = curve;
            this.raw = raw;
        }
    }

    private static CurveResult curve_csasra(Bin[] bins) throws Exception
    {
        String title = "ASFR";
        final int ppy = 1;
        double[] averages = Bins.midpoint_y(bins);
        double[] xxx = Bins.ppy_x(bins, ppy);

        int[] intervalWidths = Bins.widths(bins);
        int maxIterations = 5000;
        double positivityThreshold = 1e-6;
        double maxConvergenceDifference = 1e-3;

        double smoothingSigma = 1.0;

        double[] yyy = DisaggregateVariableWidthSeries.disaggregate(averages,
                                                                    intervalWidths,
                                                                    maxIterations,
                                                                    smoothingSigma,
                                                                    positivityThreshold,
                                                                    maxConvergenceDifference,
                                                                    false);

        if (!Util.isNonNegative(yyy))
            throw new Exception("Error calculating curve (negative value)");

        double[] yy = Bins.ppy2yearly(yyy, ppy);
        if (!Util.isNonNegative(yy))
            throw new Exception("Error calculating curve (negative value)");

        CurveVerifier.validate_means(yy, bins, 0.001);

        if (Util.False)
        {
            ChartXYSplineAdvanced chart = new ChartXYSplineAdvanced(title, "x", "y").showSplinePane(false);
            if (yyy != null)
                chart.addSeries("1", xxx, yyy);
            chart.addSeries("bins", xxx, Bins.ppy_y(bins, ppy));
            chart.display();
        }

        return new CurveResult("csasra", yy);
    }

    private static CurveResult curve_spline(Bin[] bins)
            throws Exception
    {
        String title = "ASFR";
        TargetPrecision precision = new TargetPrecision().eachBinRelativeDifference(0.001);
        MeanPreservingIterativeSpline.Options splineOptions = new MeanPreservingIterativeSpline.Options()
                .checkPositive(false);

        if (Util.True)
        {
            /*
             * Helps to avoid the last segment of the curve dive down too much
             */
            splineOptions = splineOptions.placeLastBinKnotAtRightmostPoint();
        }

        int ppy = 1;

        double[] xxx = Bins.ppy_x(bins, ppy);
        double[] yyy1 = null;
        double[] yyy2 = null;
        double[] yyy3 = null;
        double[] yyy4 = null;
        double[] yyy5 = null;

        if (Util.False)
        {
            splineOptions.basicSplineType(SteffenSplineInterpolator.class);
            yyy1 = MeanPreservingIterativeSpline.eval(bins, ppy, splineOptions, precision);
        }

        if (Util.False)
        {
            splineOptions.basicSplineType(AkimaSplineInterpolator.class);
            yyy2 = MeanPreservingIterativeSpline.eval(bins, ppy, splineOptions, precision);
        }

        if (Util.False)
        {
            splineOptions.basicSplineType(ConstrainedCubicSplineInterpolator.class);
            yyy3 = MeanPreservingIterativeSpline.eval(bins, ppy, splineOptions, precision);
        }

        if (Util.True)
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

        yyy = EnsureNonNegativeCurve.ensureNonNegative(yyy, bins, title, TargetResolution.YEARLY);

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

        return new CurveResult("spline", yy);
    }
}
