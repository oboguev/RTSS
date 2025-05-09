package rtss.data.mortality.synthetic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
// import rtss.data.curves.CurveSegmentTrend;
// import rtss.data.curves.CurveUtil;
import rtss.data.curves.CurveVerifier;
// import rtss.data.curves.EnsureMonotonicYearlyPoints;
import rtss.data.curves.InterpolateAsMeanPreservingCurve;
import rtss.data.curves.InterpolateUShapeAsMeanPreservingCurve;
import rtss.data.curves.OsierTask;
import rtss.data.curves.TuneCCS;
import rtss.data.curves.ViewCurve;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.MortalityInfo;
import rtss.data.mortality.MortalityUtil;
import rtss.data.mortality.SingleMortalityTable;
import rtss.data.mortality.laws.HeligmanPollard_R;
import rtss.data.population.struct.Population;
import rtss.data.population.synthetic.PopulationADH;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.external.Osier.OsierMortalityType;
import rtss.math.interpolate.ConstrainedCubicSplineInterpolator;
import rtss.math.interpolate.mpspline.MeanPreservingIntegralSpline;
import rtss.math.pclm.PCLM_Rizzi_2015;
import rtss.util.Clipboard;
import rtss.util.Util;
// import rtss.util.XY;
import rtss.util.plot.ChartXYSplineAdvanced;

/*
 * Загрузить поло-возрастные покаатели смертности (агреггированные по возрастным группам) из файла Excel
 * и построить на их основании таблицу смертности с годовым шагом.
 * 
 * Данные АДХ часто содержат слишком низкую смертность в группе 85-100 сравнительно с группой 80-84.
 * Это видно:
 * 
 *     - по ходу кривой Хелигмана-Полларда с коэффицентами подобранными под данные АДХ (curve_hp ниже)
 *       в двух последних диапазонах (80-84-100)
 *       
 *     - по завороту сплайна (curve_spline_1) в тех же диапазонах
 * 
 *     - по завороту кривой PCLM (curve_pclm) в тех же диапазонах
 * 
 */
public class MortalityTableADH
{
    public static final int MAX_AGE = CombinedMortalityTable.MAX_AGE;
    private static Map<String, CombinedMortalityTable> cache = new HashMap<>();

    static public boolean UsePrecomputedFiles = true;
    static public boolean UseCache = true;

    public static CombinedMortalityTable getMortalityTable(Area area, int year) throws Exception
    {
        return getMortalityTable(area, "" + year);
    }

    public static synchronized CombinedMortalityTable getMortalityTable(Area area, String year) throws Exception
    {
        String path = String.format("mortality_tables/%s/%s", area.name(), year);

        CombinedMortalityTable cmt = null;

        // look in cache
        if (UseCache)
        {
            cmt = cache.get(path);
            if (cmt != null)
                return cmt;
        }

        // try loading from resource
        if (UsePrecomputedFiles)
        {
            try
            {
                if (Util.True)
                {
                    cmt = CombinedMortalityTable.loadTotal(path);
                }
            }
            catch (Exception ex)
            {
                // ignore
                Util.noop();
            }
        }

        if (cmt == null)
            cmt = get(area, year);

        if (UseCache)
        {
            cmt.seal();
            cache.put(path, cmt);
        }

        return cmt;
    }

    /*
     * Read data from Excel and generate the table with 1-year resolution
     */
    private static CombinedMortalityTable get(Area area, String year) throws Exception
    {
        String debug_title_male = String.format("АДХ-%s %s %s", area.toString(), year, Gender.MALE.name());
        String debug_title_female = String.format("АДХ-%s %s %s", area.toString(), year, Gender.FEMALE.name());

        CombinedMortalityTable cmt = CombinedMortalityTable.newEmptyTable();

        String path = String.format("mortality_tables/%s/%s-MortalityRates-ADH.xlsx", area.name(), area.name());
        Bin[] male_mortality_bins = MortalityRatesFromExcel.loadRates(path, Gender.MALE, year);
        Bin[] female_mortality_bins = MortalityRatesFromExcel.loadRates(path, Gender.FEMALE, year);
        
        /*
         * Значения в файле (и таблице приложения 5 книги АДХ) приведены в формате "mx".
         * Преобразовать в формат "qx".
         */
        if (Util.True)
        {
            male_mortality_bins = Bins.multiply(male_mortality_bins, 0.001);
            male_mortality_bins = MortalityUtil.mx2qx(male_mortality_bins);
            male_mortality_bins = Bins.multiply(male_mortality_bins, 1000.0);

            female_mortality_bins = Bins.multiply(female_mortality_bins, 0.001);
            female_mortality_bins = MortalityUtil.mx2qx(female_mortality_bins);
            female_mortality_bins = Bins.multiply(female_mortality_bins, 1000.0);
        }

        Population p = PopulationADH.getPopulation(area, year);
        Bin[] male_population_sum_bins = p.binSumByAge(Gender.MALE, male_mortality_bins);
        Bin[] female_population_sum_bins = p.binSumByAge(Gender.FEMALE, female_mortality_bins);

        fix_80_85_100(male_mortality_bins, male_population_sum_bins);
        fix_80_85_100(female_mortality_bins, female_population_sum_bins);

        fix_40_44(female_mortality_bins, female_population_sum_bins);

        cmt.setTable(Locality.TOTAL, Gender.MALE, makeSingleTable(male_mortality_bins, debug_title_male));
        cmt.setTable(Locality.TOTAL, Gender.FEMALE, makeSingleTable(female_mortality_bins, debug_title_female));

        double[] qx = new double[MAX_AGE + 1];
        for (int age = 0; age <= MAX_AGE; age++)
        {
            Bin males = Bins.binForAge(age, male_population_sum_bins);
            Bin females = Bins.binForAge(age, female_population_sum_bins);

            double m_fraction = males.avg / (males.avg + females.avg);
            double f_fraction = females.avg / (males.avg + females.avg);

            MortalityInfo mi_m = cmt.get(Locality.TOTAL, Gender.MALE, age);
            MortalityInfo mi_f = cmt.get(Locality.TOTAL, Gender.FEMALE, age);

            qx[age] = m_fraction * mi_m.qx + f_fraction * mi_f.qx;
        }

        cmt.setTable(Locality.TOTAL, Gender.BOTH, SingleMortalityTable.from_qx("computed", qx));
        cmt.comment("АДХ-РСФСР-" + year);

        // display(cmt, Locality.TOTAL, Gender.MALE);

        if (Util.False)
        {
            String comment = "# Таблица построена модулем " + MortalityTableADH.class.getCanonicalName() + " по данным в АДХ-Россия";
            cmt.saveTable("P:\\@\\zzzz", comment);
        }

        return cmt;
    }

    private static SingleMortalityTable makeSingleTable(Bin[] bins, String debug_title) throws Exception
    {
        double[] qx = curve(bins, debug_title);
        return SingleMortalityTable.from_qx("computed", Util.divide(qx, 1000));
    }

    private static double[] curve(Bin[] bins, String debug_title) throws Exception
    {
        /*
         * Tried to use Osier library (see Sigurd Dyrting, "Osier : A Library for Demographic Calculations"
         * https://www.researchgate.net/publication/325818052_Osier_A_Library_for_Demographic_Calculations).
         * 
         * Unfortunately, most of its methods work only in mx space, not qx space, and since mx -> qx transform
         * is nonlinear, it breaks mean preservation.
         */

        // mx: works but last bins are poor fit
        // qx: fails
        // curve_osier(bins, "HELIGMAN_POLLARD", "", debug_title);

        // mx: works but not mean preserving
        // qx: Works but incorrect
        // curve_osier(bins, "HELIGMAN_POLLARD8", "", debug_title);

        // mx: works with a glitch (and does not work with added Use=0 bin)
        // qx: fails
        // curve_osier(bins, "ADJUSTED_HELIGMAN_POLLARD8", "", debug_title);

        // mx: works but not mean preserving (and severely deviates from bin unless tuned)
        // curve_osier(bins, "NIDI", "", debug_title);

        // requires standard mortality object
        // curve_osier(bins, "BRASS", "", debug_title);

        // not mean-preserving for upper bins
        // curve_osier(appendFakeBin(bins), OsierMortalityType.QX2MX, "PCLM", "P=12000000;MaxIts=100;KnotSpacing=2;Degree=3;Penalty=AIC", debug_title);

        // mx: works but not mean-preserving
        // qx: works but incorrect
        // curve_osier(appendFakeBin(bins), "CALIBRATED_SPLINE", "", debug_title);

        // requires standard mortality object
        // curve_osier(bins, "TOPALS", "", debug_title);

        // requires standard mortality, by default non mean-preserving for 80-84
        // curve_osier(appendFakeBin(bins), "PTOPALS", "", debug_title);

        // not mean preserving for the last bin
        // curve_osier(appendFakeBin(bins), OsierMortalityType.QX2MX, "KERNEL_REGRESSION", "", debug_title);
        // curve_osier(appendFakeBin(bins), OsierMortalityType.QX2MX, "KERNEL_REGRESSION", "Degree=5;Bandwidth=CV", debug_title);
        // curve_osier(appendFakeBin(bins), OsierMortalityType.QX2MX, "KERNEL_REGRESSION", "Degree=3;Bandwidth=GCV", debug_title);
        // curve_osier(appendFakeBin(bins), OsierMortalityType.QX2MX, "KERNEL_REGRESSION", "Degree=5;Bandwidth=ROT", debug_title);
        // curve_osier(appendFakeBin(bins), OsierMortalityType.QX2MX, "KERNEL_REGRESSION", "Degree=5;Bandwidth=IROT", debug_title);
        // curve_osier(appendFakeBin(bins), OsierMortalityType.QX2MX, "KERNEL_REGRESSION", "Degree=3;Bandwidth=AIC", debug_title);
        // curve_osier(appendFakeBin(bins), OsierMortalityType.QX2MX, "KERNEL_REGRESSION", "Degree=3;Bandwidth=MSE", debug_title);

        // it is a smoothing of HYBRID_FORCE
        // curve_osier(bins, "SVM", "", debug_title);

        // mx: mean-preserving?
        // curve_osier(bins, "SMOOTHED_ASDR", "", debug_title);

        // qx: does not compute correctly
        // mx: has a curvature bend in 80-84
        // mx avg deviation under 1% for ages 0-69, under 2% for ages 70-84, large (20%) for 85+
        // however when back-converted to qx deviation becomes much larger (e.g. 5% for bin 1-4)
        // curve_osier(bins, OsierMortalityType.QX2MX, "HYBRID_FORCE", "", debug_title);

        // does not work correctly
        // curve_osier(appendFakeBin(bins), "CUBIC_SF", "", debug_title);

        // mean-preserving?
        // curve_osier(bins, "MOD_QUADRATIC_FORCE", "", debug_title);

        // return curve_hp(bins, debug_title);
        // return curve_spline_1(bins, debug_title);
        
        /*
         * Another approach to explore for mean-preserving decomposition could have been the 
         * use of B-splines with smoothness criteria plus mean-preservation criteria, then 
         * perhaps solved via optimization toolbox.
         * 
         * Also see MATLAB psplines  
         *   https://www.mathworks.com/help/curvefit/smoothing-splines.html
         *   https://www.mathworks.com/help/curvefit/smoothing-data.html
         * and google ("penalized spline" source code). This can also be used to impose
         * piece-wise monotonicity (U-shape) criteria.     
         */

        return curve_pclm(bins, debug_title);
    }

    private static double[] curve_pclm(Bin[] bins, String debug_title) throws Exception
    {
        final int ppy = 1;

        /*
         * To suppress boundary effects, append fake bin with growing rate 
         */
        Bin[] xbins = bins;
        Bin first = Bins.firstBin(bins);
        Bin last = Bins.lastBin(bins);
        if (Util.True)
            xbins = appendFakeBin(bins);

        final double lambda = 0.0001;
        double[] yyy = PCLM_Rizzi_2015.pclm(xbins, lambda, ppy);
        yyy = Util.splice(yyy, first.age_x1, ppy * (last.age_x2 + 1) - 1);

        if (Util.True)
        {
            /*
             * Display yearly curve
             */
            String title = "PCLM yearly curve " + debug_title;
            ViewCurve.view(title, bins, "qx", yyy);
        }

        double[] yy = Bins.ppy2yearly(yyy, ppy);

        CurveVerifier.positive(yy, bins, debug_title, true);
        CurveVerifier.verifyUShape(yy, bins, false, debug_title, false);
        CurveVerifier.validate_means(yy, bins);
        
        if (Util.False && bins[0].widths_in_years == 1)
        {
            // the change is rare and tiny, such as 71.009 => 71.006
            if (Math.abs(bins[0].avg - yy[0]) >= 0.001)
            {
                Util.err(String.format("PCLM Q0: %s %.3f => %.3f", debug_title, bins[0].avg, yy[0]));
                Util.noop();
            }
        }

        return yy;
    }

    /*
     * To suppress boundary effects, append fake bin with growing rate 
     */
    private static Bin[] appendFakeBin(Bin[] bins) throws Exception
    {
        Bin last = Bins.lastBin(bins);
        List<Bin> list = new ArrayList<>();
        for (Bin bin : bins)
            list.add(new Bin(bin));
        list.add(new Bin(last.age_x2 + 1,
                         last.age_x2 + last.widths_in_years,
                         last.avg + 1.8 * (last.avg - last.prev.avg)));
        return Bins.bins(list);
    }

    @SuppressWarnings("unused")
    private static double[] curve_osier(Bin[] bins, String method, String params, String debug_title) throws Exception
    {
        return curve_osier(bins, OsierMortalityType.QX2MX, method, params, debug_title);
    }

    private static double[] curve_osier(Bin[] bins, OsierMortalityType mtype, String method, String params, String debug_title) throws Exception
    {
        int ppy = 1;

        if (params != null && params.length() != 0)
        {
            boolean first = true;
            for (String ps : params.split(";"))
            {
                if (first)
                {
                    method += ":";
                    first = false;
                }
                else
                {
                    method += ",";
                }

                method += '"' + ps + '"';
            }
        }

        double[] yy = OsierTask.mortality(bins, mtype, "XXX", method, ppy);
        if (Util.True)
        {
            String title = "Osier curve (" + method + ") " + debug_title;
            switch (mtype)
            {
            case QX:
                ViewCurve.view(title, bins, "qx", yy);
                break;

            case MX:
                ViewCurve.view(title, bins, "mx", yy);
                break;

            case QX2MX:
                ViewCurve.view(title, MortalityUtil.proqx2mx(bins), "mx", MortalityUtil.proqx2mx(yy));
                break;
            }
        }

        double[] y = Bins.ppy2yearly(yy, ppy);
        if (bins[0].widths_in_years == 1 && !Util.differ(y[0], bins[0].avg, 0.05))
            y[0] = bins[0].avg;
        CurveVerifier.validate_means(y, bins);
        return y;
    }

    @SuppressWarnings("unused")
    private static double[] curve_hp(Bin[] bins, String debug_title) throws Exception
    {
        /*
         * Unfortunately, existing R implementation of Heligman-Pollard estimator
         * fits the curve to points rather than intervals, and means are severely deviated  
         */
        boolean display = Util.False;
        int ppy = display ? 10 : 1000;
        double[] yy = new HeligmanPollard_R(bins).curve(ppy);

        if (display)
        {
            String title = "HP curve " + debug_title;
            ViewCurve.view(title, bins, "qx", yy);
        }

        double[] y = Bins.ppy2yearly(yy, ppy);

        // will fail here
        // CurveVerifier.validate_means(y, bins);

        return y;
    }

    /*
     * Generated curve is incorrect
     */
    @SuppressWarnings("unused")
    private static double[] curve_osier_hp8(Bin[] bins, String debug_title) throws Exception
    {
        int ppy = 10;
        double[] yy = OsierTask.mortality(bins, OsierMortalityType.QX2MX, "XXX", "HELIGMAN_POLLARD8", ppy);
        if (Util.True)
        {
            String title = "Osier HP curve " + debug_title;
            ViewCurve.view(title, bins, "qx", yy);
        }
        double[] y = Bins.ppy2yearly(yy, ppy);
        // will fail here
        // CurveVerifier.validate_means(y, bins);
        return y;
    }

    /*
     * Fails
     */
    @SuppressWarnings("unused")
    private static double[] curve_osier_hp8_adjusted(Bin[] bins, String debug_title) throws Exception
    {
        int ppy = 10;
        double[] yy = OsierTask.mortality(bins, OsierMortalityType.QX2MX, "XXX", "ADJUSTED_HELIGMAN_POLLARD8", ppy);
        if (Util.True)
        {
            String title = "Osier HP curve " + debug_title;
            ViewCurve.view(title, bins, "qx", yy);
        }
        double[] y = Bins.ppy2yearly(yy, ppy);
        // will fail here
        // CurveVerifier.validate_means(y, bins);
        return y;
    }

    /*
     * Fails
     */
    @SuppressWarnings("unused")
    private static double[] curve_osier_hp(Bin[] bins, String debug_title) throws Exception
    {
        int ppy = 1;
        double[] yy = OsierTask.mortality(bins, OsierMortalityType.QX2MX, "XXX", "HELIGMAN_POLLARD", ppy);
        if (Util.True)
        {
            String title = "Osier HP curve " + debug_title;
            ViewCurve.view(title, bins, "qx", yy);
        }
        double[] y = Bins.ppy2yearly(yy, ppy);
        // will fail here
        // CurveVerifier.validate_means(y, bins);
        return y;
    }

    @SuppressWarnings("unused")
    private static double[] curve_spline_1(Bin[] bins, String debug_title) throws Exception
    {
        CurveVerifier.verifyUShape(bins, false, debug_title, true);

        if (Util.False)
        {
            int[] x = Bins.start_x(bins);
            double[] y = Bins.midpoint_y(bins);
            Clipboard.put(", ", x, y);
            Util.noop();
        }

        // CurveSegmentTrend[] trends = CurveUtil.getUShapeSegmentTrends(bins, debug_title);

        final int ppy = 1000;

        Bin[] xbins = bins;
        if (Util.True)
            xbins = split_1_4(bins);

        MeanPreservingIntegralSpline.Options options = new MeanPreservingIntegralSpline.Options();
        options = options.ppy(ppy).debug_title(debug_title).basicSplineType(ConstrainedCubicSplineInterpolator.class);
        options = options.splineParams("title", debug_title);
        // do not use f2.trends since it over-determines the spline and makes value of s' discontinuous between segments 
        // options = options.splineParams("f2.trends", trends);
        double[] yyy = MeanPreservingIntegralSpline.eval(xbins, options);
        double f1n = new TuneCCS(xbins, options, yyy).tuneLastSegment();
        options = options.splineParams("f1.n", f1n);
        options = options.splineParams("f1.0", yyy[0] * 1.5);
        // options = options.splineParams("f2.trace", true);
        yyy = MeanPreservingIntegralSpline.eval(xbins, options);

        if (Util.False)
        {
            /*
             * Display sub-yearly curve
             */
            String title = "MP-integral sub-yearly curve " + debug_title;
            ViewCurve.view(title, bins, "qx", yyy);
        }

        double[] yy = Bins.ppy2yearly(yyy, ppy);
        if (Util.False)
        {
            /*
             * Display yearly curve
             */
            String title = "MP-integral yearly curve " + debug_title;
            ViewCurve.view(title, bins, "qx", yy);
        }

        CurveVerifier.positive(yy, bins, debug_title, true);
        // new EnsureMonotonicYearlyPoints(bins, yy, debug_title).fix();
        CurveVerifier.verifyUShape(yy, bins, false, debug_title, false);
        CurveVerifier.validate_means(yy, bins);

        return yy;
    }

    @SuppressWarnings("unused")
    private static double[] curve_spline_2(Bin[] bins, String debug_title) throws Exception
    {
        final int ppy = 1000;

        CurveVerifier.verifyUShape(bins, false, debug_title, true);

        InterpolateAsMeanPreservingCurve.Options options = new InterpolateAsMeanPreservingCurve.Options();
        options = options.debug_title(debug_title).ensurePositive(true).ensureMonotonicallyDecreasing_1_4_5_9(true);
        options = options.ppy(ppy).displayCurve(false);
        double[] curve = InterpolateAsMeanPreservingCurve.curve(bins, options);

        CurveVerifier.positive(curve, bins, debug_title, true);
        CurveVerifier.verifyUShape(curve, bins, debug_title, false);
        CurveVerifier.validate_means(curve, bins);
        return curve;
    }

    @SuppressWarnings("unused")
    private static double[] curve_spline_3(Bin[] bins, String debug_title) throws Exception
    {
        final int ppy = 1000;

        CurveVerifier.verifyUShape(bins, false, debug_title, true);

        InterpolateUShapeAsMeanPreservingCurve.Options options = new InterpolateUShapeAsMeanPreservingCurve.Options();
        options = options.debug_title(debug_title).ensurePositive(true).ensureMonotonicallyDecreasing_1_4_5_9(true);
        options = options.ppy(ppy).displayCurve(false);
        double[] curve = InterpolateUShapeAsMeanPreservingCurve.curve(bins, options);

        CurveVerifier.positive(curve, bins, debug_title, true);
        CurveVerifier.verifyUShape(curve, bins, debug_title, false);
        CurveVerifier.validate_means(curve, bins);
        return curve;
    }

    @SuppressWarnings("unused")
    private static void display(CombinedMortalityTable cmt, Locality locality, Gender gender) throws Exception
    {
        double[] qx = cmt.getSingleTable(locality, gender).qx();

        Util.print(cmt.comment() + " qx", qx, 0);

        new ChartXYSplineAdvanced(cmt.comment() + " qx", "age", "mortality").showSplinePane(false)
                .addSeries("qx", qx)
                .display();
    }

    /*
     * Для некоторых лет (РСФСР 1927-1933, 1937, 1946-1948) рассчитанная АДХ мужская смертность в возрастной группе 85-100 ниже, 
     * чем в группе 80-84. Это не только представляется весьма сомнительным фактически, но и вызывает резкий перегиб и 
     * немонотонное поведение строимой кривой смертности в этом возрастном диапазоне.
     * 
     * Для некоторых других лет смертность в группе 85-100 хотя и не ниже, чем для группы 80-84, но едва-едва выше её, что
     * также малореалистично и также вызывает резкий изгиб кривой смертности на горизонталь, вместо её плавного подъёма.  
     * 
     * Откорректировать значения смертности в этих двух группах таким образом, чтобы общее число смертей в них осталось неизменным,
     * при данной возрастной структуре населения.
     * 
     * Понизить значение смертности в возрасте 80-84 и повысить её для возраста 85-100 так, чтобы смертность в группе 85-100 
     * была выше, чем в группе 80-84. Попытаться установить значения для групп 80-84 и 85-100 таким образом, чтобы шло
     * плавное последовательное нарастание сравнительно с группой 75-79.  
     */
    private static void fix_80_85_100(Bin[] m, final Bin[] psum) throws Exception
    {
        if (m.length < 3 || psum.length < 3)
            return;

        Bin m2 = Bins.lastBin(m); // 85-100
        Bin p2 = Bins.lastBin(psum);

        Bin m1 = m2.prev; // 80-84
        Bin p1 = p2.prev;

        Bin m0 = m1.prev; // 75-79
        Bin p0 = p1.prev;

        if (m0.age_x1 == 75 && m0.age_x2 == 79 &&
            p0.age_x1 == 75 && p0.age_x2 == 79 &&
            m1.age_x1 == 80 && m1.age_x2 == 84 &&
            p1.age_x1 == 80 && p1.age_x2 == 84 &&
            m2.age_x1 == 85 && m2.age_x2 == 100 &&
            p2.age_x1 == 85 && p2.age_x2 == 100)
        {
            // proceed
        }
        else
        {
            return;
        }

        /* content of psum bins is actually population sum, not average, so do not divide by bin width */
        double deaths = m1.avg * p1.avg + m2.avg * p2.avg;

        double v1 = (deaths + m0.avg * p2.avg) / (p1.avg + 2 * p2.avg);
        double v2 = 2 * v1 - m0.avg;

        /* if 80-84 is already below the computed level, do not change it */
        if (m1.avg <= v1)
            return;

        /* should not happen */
        if (v1 <= m0.avg)
            throw new Exception("Internal error");

        m1.avg = v1;
        m2.avg = v2;

        double deaths2 = m1.avg * p1.avg + m2.avg * p2.avg;
        if (Util.differ(deaths, deaths2))
            throw new Exception("Unable to correct inverted mortality rate at 85+");
    }

    /*
     * Для некоторых лет (РСФСР 1927-1929) рассчитанная АДХ женская смертность имеет лёгкий провал в возрастной группе 40-44,
     * что представляется сомнительным фактически и также не позволяет использовать монотонные сплайны.
     * 
     * Перераспределить в корзину 40-44 смерти из двух соседних возрастных корзин таким образом, чтобы общее количество смертей 
     * сохранялось, а смертность была монотонной.
     * 
     * Итоговый вариант 1: m0.avg < m1. avg < m2.avg
     * Итоговый вариант 2: m0.avg = m1. avg = m2.avg
     */
    private static void fix_40_44(Bin[] m, final Bin[] psum) throws Exception
    {
        Bin m1 = Bins.binForAge(40, m);
        if (m1 == null)
            return;

        Bin p1 = Bins.binForAge(40, psum);
        if (p1 == null)
            return;

        Bin m2 = m1.next;
        Bin p2 = p1.next;

        Bin m0 = m1.prev;
        Bin p0 = p1.prev;

        if (m0 != null && m2 != null && p0 != null && p2 != null &&
            m0.age_x1 == p0.age_x1 && m0.age_x2 == p0.age_x2 &&
            m1.age_x1 == p1.age_x1 && m1.age_x2 == p1.age_x2 &&
            m2.age_x1 == p2.age_x1 && m2.age_x2 == p2.age_x2 &&
            m0.widths_in_years == m1.widths_in_years &&
            m1.widths_in_years == m2.widths_in_years)
        {
            // proceed
        }
        else
        {
            // no such bin
            return;
        }

        // no dip
        if (m1.avg >= m0.avg)
            return;

        double deaths_012 = m0.avg * p0.avg + m1.avg * p1.avg + m2.avg * p2.avg;

        if (m0.avg == m2.avg)
        {
            m0.avg = m1.avg = m2.avg = deaths_012 / (p0.avg + p1.avg + p2.avg);
        }
        else if (m0.avg < m2.avg && Util.True)
        {
            // This is only for RSFSR-1927-female case
            // Three constraints: 
            //     - total number of deaths in ranges 0, 1 and 2 is constant 
            //     - added deaths in range 0 = added increase in range 2 = removed deaths in range 1 / 2  
            //     - mortality1 = (mortality0 + mortality0) / 2

            double c1 = p2.avg + p1.avg / 2;
            double c2 = p0.avg + p1.avg / 2;
            double c3 = p2.avg / p0.avg;

            double v2 = deaths_012 / c2 - m0.avg + c3 * m2.avg;
            v2 /= c3 + c1 / c2;

            double v0 = deaths_012 - (p2.avg + p1.avg / 2) * v2;
            v0 /= p0.avg + p1.avg / 2;

            double v1 = (v0 + v2) / 2;

            if (v2 >= v1 && v1 >= v0)
            {
                // proceed
            }
            else
            {
                throw new Exception("Internal error");
            }

            m0.avg = v0;
            m1.avg = v1;
            m2.avg = v2;
        }

        double deaths2 = m0.avg * p0.avg + m1.avg * p1.avg + m2.avg * p2.avg;
        if (Util.differ(deaths_012, deaths2))
            throw new Exception("Unable to correct inverted mortality rate at age 40-44");
    }

    /*
     * Half of deaths in age range 1 to 4 (until 5) occur in year 1
     */
    private static Bin[] split_1_4(Bin[] bins) throws Exception
    {
        List<Bin> list = new ArrayList<>();

        for (Bin bin : bins)
        {
            if (bin.age_x1 == 1 && bin.age_x2 == 4)
            {
                double deaths_1_4 = bin.avg * 4;
                double deaths_1 = deaths_1_4 * 0.5;
                double deaths_2_4 = deaths_1_4 - deaths_1;
                list.add(new Bin(1, 1, deaths_1 / 1));
                list.add(new Bin(2, 4, deaths_2_4 / 3));
            }
            else
            {
                list.add(new Bin(bin));
            }
        }

        return Bins.bins(list);
    }
}
