package rtss.data.curves;

import java.util.HashSet;
import java.util.Set;

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
import rtss.math.interpolate.disaggregate.csasra.DisaggregateConstantWidthSeries;
import rtss.math.interpolate.disaggregate.csasra.DisaggregateVariableWidthSeries;
import rtss.math.interpolate.mpspline.MeanPreservingIntegralSpline;
import rtss.math.interpolate.mpspline.MeanPreservingIterativeSpline;
import rtss.math.pclm.PCLM_Rizzi_2015;
import rtss.util.Util;
import rtss.util.plot.CaptureImages;
import rtss.util.plot.ChartXYSPlineBasic;
import rtss.util.plot.ChartXYSplineAdvanced;

/**
 * Interpolate population in aggregated bins to a smooth disaggregated curve, in a mean-preserving way.

 * Интерпролировать население агрегированное в корзины плавной кривой, сохраняющей значение корзин.
 * 
 * Применяется в двух случая:
 * 
 *   1. targetResolution = YEARLY
 *      Дезагрегация многолетних корозин с годовые значения.
 *      Обычно это пятилетние группы (0-4 5-9 и т.д), иногда с 16-летней группой в конце (85-100).
 *      Используется PopulationADH (PopulationFromExcel), 
 *      а также конструктором Populaton из корзин (вызывается из AdjustPopulation1941). 
 *      
 *   2. targetResolution = DAILY 
 *      Дезагрегация погодовых значений в дневные.
 *      Используется PopulationContext. 
 *      
 * *************************************************************************************     
 * 
 * В случае DAILY используеся алгоритм CSASRA.
 * При интерполяции данных для TargetResolution.DAILY алгоритм CSASRA даёт гораздо более гладкие данные, 
 * чем алгоритм сплайна.
 * 
 * *************************************************************************************
 *      
 * В случае YEARLY обработка двухстепенчатая.
 * 
 * На первой ступени генерируется годовая кривая.
 * Алгоритм первой ступени -- универсальный алгоритм распаковки, не знающий демографической специфики.
 * 
 * На второй ступени ход кривой в начальные годы возраста (0-9) корректируется для лучшего соответствия
 * характеристикам раннедетской смертности в данную эпоху, а также провалам рождений в недавние годы.
 * Это алгоритм RefineYearlyPopulation.
 * 
 * Для первой ступени применимы два алгоритма: CSASRA и SPLINE.
 * Их результаты в большинстве случаев сходны.
 * Однако SPLINE даёт лучший (на начальном участке возрастов) результат для 1956-1959 гг.,
 * когда число рождений замедляется и кривая начального участка становится пологой.    
 */
public class InterpolatePopulationAsMeanPreservingCurve
{
    public static class InterpolationOptions
    {
        private boolean usePrimaryCSASRA = true;
        private boolean usePrimarySPLINE = true;
        private boolean useSecondaryRefineYearlyAges = true;
        private boolean debugSecondaryRefineYearlyAges = false;
        private Double secondaryRefineYearlyAgesSmoothness = null;
        private String subtitle = null;
        private boolean displayChart = false;
        private boolean allowChartMinorClipping = false;
        private Set<String> extras = new HashSet<>();

        public InterpolationOptions clone()
        {
            InterpolationOptions c = new InterpolationOptions();

            c.usePrimaryCSASRA = usePrimaryCSASRA;
            c.usePrimarySPLINE = usePrimarySPLINE;
            c.useSecondaryRefineYearlyAges = useSecondaryRefineYearlyAges;
            c.debugSecondaryRefineYearlyAges = debugSecondaryRefineYearlyAges;
            c.secondaryRefineYearlyAgesSmoothness = secondaryRefineYearlyAgesSmoothness;
            c.subtitle = subtitle;
            c.displayChart = displayChart;
            c.allowChartMinorClipping = allowChartMinorClipping;
            c.extras.addAll(extras);

            return c;
        }

        public InterpolationOptions usePrimaryCSASRA(boolean usePrimaryCSASRA)
        {
            this.usePrimaryCSASRA = usePrimaryCSASRA;
            return this;
        }

        public InterpolationOptions usePrimarySPLINE(boolean usePrimarySPLINE)
        {
            this.usePrimarySPLINE = usePrimarySPLINE;
            return this;
        }

        public InterpolationOptions useSecondaryRefineYearlyAges(boolean useSecondaryRefineYearlyAges)
        {
            this.useSecondaryRefineYearlyAges = useSecondaryRefineYearlyAges;
            return this;
        }

        public InterpolationOptions debugSecondaryRefineYearlyAges(boolean debugSecondaryRefineYearlyAges)
        {
            this.debugSecondaryRefineYearlyAges = debugSecondaryRefineYearlyAges;
            return this;
        }

        public InterpolationOptions secondaryRefineYearlyAgesSmoothness(double secondaryRefineYearlyAgesSmoothness)
        {
            this.secondaryRefineYearlyAgesSmoothness = secondaryRefineYearlyAgesSmoothness;
            return this;
        }

        public InterpolationOptions subtitle(String subtitle)
        {
            this.subtitle = subtitle;
            return this;
        }

        public InterpolationOptions displayChart(boolean displayChart)
        {
            this.displayChart = displayChart;
            return this;
        }

        public InterpolationOptions allowChartMinorClipping(boolean allowChartMinorClipping)
        {
            this.allowChartMinorClipping = allowChartMinorClipping;
            return this;
        }

        public InterpolationOptions extra(String extra)
        {
            this.extras.add(extra);
            return this;
        }

        /* --------------------------------------------------------------------- */

        public boolean usePrimaryCSASRA()
        {
            return usePrimaryCSASRA;
        }

        public boolean usePrimarySPLINE()
        {
            return usePrimarySPLINE;
        }

        public boolean useSecondaryRefineYearlyAges()
        {
            return useSecondaryRefineYearlyAges;
        }

        public boolean debugSecondaryRefineYearlyAges()
        {
            return debugSecondaryRefineYearlyAges;
        }

        public Double secondaryRefineYearlyAgesSmoothness()
        {
            return secondaryRefineYearlyAgesSmoothness;
        }

        public String subtitle()
        {
            return subtitle;
        }

        public boolean displayChart()
        {
            return displayChart;
        }

        public boolean allowChartMinorClipping()
        {
            return allowChartMinorClipping;
        }

        public boolean hasExtra(String extra)
        {
            return extras.contains(extra);
        }
    }

    public static class InterpolationOptionsByGender
    {
        private InterpolationOptions both;
        private InterpolationOptions male;
        private InterpolationOptions female;
        private boolean allowCache = false;

        public InterpolationOptionsByGender allowCache(boolean allowCache)
        {
            this.allowCache = allowCache;
            return this;
        }

        public boolean allowCache()
        {
            return allowCache;
        }

        public InterpolationOptions both()
        {
            if (both == null)
                both = new InterpolationOptions();
            return both;
        }

        public InterpolationOptions male()
        {
            if (male == null)
                male = new InterpolationOptions();
            return male;
        }

        public InterpolationOptions female()
        {
            if (female == null)
                female = new InterpolationOptions();
            return female;
        }

        public InterpolationOptions getForGender(Gender gender)
        {
            if (gender == Gender.MALE && male != null)
                return male;
            else if (gender == Gender.FEMALE && female != null)
                return female;
            else
                return both;
        }

        public InterpolationOptions createForGender(Gender gender)
        {
            if (gender == Gender.MALE)
            {
                if (male == null)
                    male = new InterpolationOptions();
                return male;
            }
            else if (gender == Gender.FEMALE)
            {
                if (female == null)
                    female = new InterpolationOptions();
                return female;
            }
            else
            {
                if (both == null)
                    both = new InterpolationOptions();
                return both;
            }
        }
    }

    private static class CurveResult
    {
        public final String method;
        public final double[] curve;
        public final double[] raw;

        public CurveResult(String method, double[] curve)
        {
            this.method = method;
            this.curve = curve;
            this.raw = null;
        }

        public CurveResult(String method, double[] curve, double[] raw)
        {
            this.method = method;
            this.curve = curve;
            this.raw = raw;
        }
    }

    public static final int MAX_AGE = Population.MAX_AGE;

    public static enum FirstStageAlgorithm
    {
        CSASRA, SPLINE
    }

    /* =========================================================================================================== */

    public static double[] curve(
            Bin[] bins,
            String title,
            TargetResolution targetResolution,
            Integer yearHint,
            Gender gender,
            InterpolationOptions options)
            throws Exception
    {
        
        if (Bins.isAllZero(bins))
        {
            // special case: all bin.avg are zero, zero population
            int ppy = 12;
            if (targetResolution == TargetResolution.DAILY)
                ppy = 1;

            double[] xx = Bins.ppy_x(bins, ppy);
            double[] result = new double[xx.length];
            return result;
        }
        
        if (options == null)
            options = new InterpolationOptions();

        if (options.subtitle() != null)
            title = title + " " + options.subtitle();

        // curve_osier(bins, "method", "", title);
        // return curve_pclm(bins, title);

        Exception ex = null;
        CurveResult curve = null;
        
        FirstStageAlgorithm preferredAlgorithm = FirstStageAlgorithm.CSASRA;
        
        if (options.usePrimaryCSASRA() && !options.usePrimarySPLINE())
            preferredAlgorithm = FirstStageAlgorithm.CSASRA;
        else if (!options.usePrimaryCSASRA() && options.usePrimarySPLINE())
            preferredAlgorithm = FirstStageAlgorithm.SPLINE;
        else if (targetResolution == TargetResolution.YEARLY && yearHint >= 1956)
            preferredAlgorithm = FirstStageAlgorithm.SPLINE;
        else
            preferredAlgorithm = FirstStageAlgorithm.CSASRA;

        if (preferredAlgorithm == FirstStageAlgorithm.CSASRA)
        {
            /*
             * Try CSASRA first
             */
            try
            {
                if (curve == null && Util.True && options.usePrimaryCSASRA())
                    curve = curve_csasra(bins, title, targetResolution, yearHint, gender, options);
            }
            catch (Exception e2)
            {
                Util.err("CSASRA disaggregation failed for " + title + ", " + e2.getLocalizedMessage());
                ex = e2;
            }

            try
            {
                if (curve == null && Util.True && options.usePrimarySPLINE())
                    curve = curve_spline(bins, title, targetResolution, yearHint, gender, options);
            }
            catch (Exception e2)
            {
                Util.err("Spline disaggregation failed for " + title + ", " + e2.getLocalizedMessage());
                if (ex == null)
                    ex = e2;
            }
        }
        else
        {
            /*
             * Try SPLINE first
             */
            try
            {
                if (curve == null && Util.True && options.usePrimarySPLINE())
                    curve = curve_spline(bins, title, targetResolution, yearHint, gender, options);
            }
            catch (Exception e2)
            {
                Util.err("Spline disaggregation failed for " + title + ", " + e2.getLocalizedMessage());
                if (ex == null)
                    ex = e2;
            }
            
            try
            {
                if (curve == null && Util.True && options.usePrimaryCSASRA())
                    curve = curve_csasra(bins, title, targetResolution, yearHint, gender, options);
            }
            catch (Exception e2)
            {
                Util.err("CSASRA disaggregation failed for " + title + ", " + e2.getLocalizedMessage());
                ex = e2;
            }
        }
        
        if (curve == null)
        {
            if (ex != null)
                throw ex;
            else
                throw new Exception("Unable to build the curve");
        }

        /*
         * Export chart image
         */
        if (CaptureImages.get() != null)
        {
            int ppy = 1;
            double[] xxx = Bins.ppy_x(bins, ppy);

            ChartXYSPlineBasic chart = new ChartXYSPlineBasic(title, "x", "y");

            chart.addSeries("final " + curve.method, xxx, curve.curve);
            double maxY = Util.max(curve.curve);

            if (curve.raw != null && Util.differ(curve.curve, curve.raw))
            {
                chart.addSeries("raw " + curve.method, xxx, curve.raw);
                maxY = Math.max(maxY, Util.max(curve.raw));
            }

            chart.addLineSeries("bins", Bins.ppy_x(bins, 100), Bins.ppy_y(bins, 100));
            maxY = Math.max(maxY, Util.max(Bins.ppy_y(bins, 100)));

            if (options.hasExtra("chart-spline") && !curve.method.equals("spline"))
            {
                double[] ss = rawSpline(bins, title, targetResolution, yearHint, gender, options);
                chart.addSeries("raw spline", xxx, ss);
                maxY = Math.max(maxY, Util.max(ss));
            }

            if (options.hasExtra("chart-csasra") && !curve.method.equals("csasra"))
            {
                double[] cc = rawCSASRA(bins, title, targetResolution, yearHint, gender, options);
                chart.addSeries("raw csasra", xxx, cc);
                maxY = Math.max(maxY, Util.max(cc));
            }

            chart.maxY(clipMaxY(maxY, options));

            // chart.display();

            CaptureImages ci = CaptureImages.get();
            String fn = yearHint + " " + gender.name() + " " + title;
            chart.exportImage(ci.cx, ci.cy, ci.path(fn + ".png"));
        }

        /*
         * Display chart
         */
        if (Util.False || options.displayChart())
        {
            int ppy = 1;
            double[] xxx = Bins.ppy_x(bins, ppy);
            ChartXYSplineAdvanced chart = new ChartXYSplineAdvanced(title, "x", "y").showSplinePane(false);

            chart.addSeries("final " + curve.method, xxx, curve.curve);

            if (curve.raw != null && Util.differ(curve.curve, curve.raw))
                chart.addSeries("raw " + curve.method, xxx, curve.raw);

            chart.addSeries("bins", xxx, Bins.ppy_y(bins, ppy));

            if (options.hasExtra("chart-spline") && !curve.method.equals("spline"))
            {
                double[] ss = rawSpline(bins, title, targetResolution, yearHint, gender, options);
                chart.addSeries("raw spline", xxx, ss);
            }

            if (options.hasExtra("chart-csasra") && !curve.method.equals("csasra"))
            {
                double[] ss = rawCSASRA(bins, title, targetResolution, yearHint, gender, options);
                chart.addSeries("raw csasra", xxx, ss);
            }

            chart.display();
        }

        return curve.curve;
    }

    private static double clipMaxY(double v, InterpolationOptions options)
    {
        /*
         * 1_000 => 50
         * 10_000 => 500
         * 100_000 => 500
         */
        long scaleQuant = scaleQuant(v);

        long nq = Math.round(Math.floor(v)) / scaleQuant;

        double excess = v - nq * scaleQuant;

        if (excess <= 70 && options.allowChartMinorClipping())
            return nq * scaleQuant;
        else
            return (nq + 1) * scaleQuant;
    }

    private static long scaleQuant(double v)
    {
        // round up to next power of 10
        int n = (int) Math.ceil(Math.log10(v));
        long scale = Math.round(Math.pow(10, n));
        return scale / 20;
    }

    private static double[] rawSpline(
            Bin[] bins,
            String title,
            TargetResolution targetResolution,
            Integer yearHint,
            Gender gender,
            InterpolationOptions options) throws Exception
    {
        options = options.clone();
        options.useSecondaryRefineYearlyAges = false;
        CurveResult curve = curve_spline(bins, title, targetResolution, yearHint, gender, options);
        return curve.curve;
    }

    private static double[] rawCSASRA(
            Bin[] bins,
            String title,
            TargetResolution targetResolution,
            Integer yearHint,
            Gender gender,
            InterpolationOptions options) throws Exception
    {
        options = options.clone();
        options.useSecondaryRefineYearlyAges = false;
        CurveResult curve = curve_csasra(bins, title, targetResolution, yearHint, gender, options);
        return curve.curve;
    }

    /* ================================================================================================ */

    private static CurveResult curve_csasra(
            Bin[] bins,
            String title,
            TargetResolution targetResolution,
            Integer yearHint,
            Gender gender,
            InterpolationOptions options)
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

        if (targetResolution == TargetResolution.YEARLY && options.useSecondaryRefineYearlyAges)
        {
            /* уточнить разбивку на возраста 0-9 */
            double[] raw = Util.dup(yy);
            yy = RefineYearlyPopulation.refine(bins, title, yy, yearHint, gender, options);
            CurveVerifier.validate_means(yy, bins);
            return new CurveResult("csasra", yy, raw);
        }
        else
        {
            return new CurveResult("csasra", yy);
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
    private static CurveResult curve_spline(Bin[] bins, String title, TargetResolution targetResolution, Integer yearHint, Gender gender,
            InterpolationOptions options)
            throws Exception
    {
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
            splineOptions.basicSplineType(SteffenSplineInterpolator.class);
            yyy1 = MeanPreservingIterativeSpline.eval(bins, ppy, splineOptions, precision);
        }

        if (Util.False)
        {
            splineOptions.basicSplineType(AkimaSplineInterpolator.class);
            yyy2 = MeanPreservingIterativeSpline.eval(bins, ppy, splineOptions, precision);
        }

        if (Util.True)
        {
            splineOptions.basicSplineType(ConstrainedCubicSplineInterpolator.class);
            yyy3 = MeanPreservingIterativeSpline.eval(bins, ppy, splineOptions, precision);
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

        if (targetResolution == TargetResolution.YEARLY && options.useSecondaryRefineYearlyAges)
        {
            double[] raw = Util.dup(yy);
            /* уточнить разбивку на возраста 0-9 */
            yy = RefineYearlyPopulation.refine(bins, title, yy, yearHint, gender, options);
            CurveVerifier.validate_means(yy, bins);
            return new CurveResult("spline", yy, raw);
        }
        else
        {
            return new CurveResult("spline", yy);
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
