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
import rtss.data.curves.EnsureMonotonicYearlyPoints;
import rtss.data.curves.InterpolateAsMeanPreservingCurve;
import rtss.data.curves.InterpolateUShapeAsMeanPreservingCurve;
import rtss.data.curves.TuneCCS;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.MortalityInfo;
import rtss.data.mortality.SingleMortalityTable;
import rtss.data.mortality.laws.HeligmanPollard_R;
import rtss.data.population.Population;
import rtss.data.population.synthetic.PopulationADH;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.math.interpolate.ConstrainedCubicSplineInterpolator;
import rtss.math.interpolate.mpspline.MeanPreservingIntegralSpline;
import rtss.util.Clipboard;
import rtss.util.Util;
// import rtss.util.XY;
import rtss.util.plot.ChartXYSplineAdvanced;

/*
 * Загрузить поло-возрастные покаатели смертности (агреггированные по возрастным группам) из файла Excel
 * и построить на их основании таблицу смертности с годовым шагом.
 */
public class MortalityTableADH
{
    public static final int MAX_AGE = CombinedMortalityTable.MAX_AGE;
    private static Map<String, CombinedMortalityTable> cache = new HashMap<>();

    static public boolean UseCache = false; // ###
    static public boolean UsePrecomputed = false; // ###

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
        if (UsePrecomputed)
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
        return curve_1(bins, debug_title);
    }

    @SuppressWarnings("unused")
    private static double[] curve_1(Bin[] bins, String debug_title) throws Exception
    {
        CurveVerifier.verifyUShape(bins, false, debug_title, true);
        
        if (Util.False)
        {
            /*
             * Unfortunately, existing R implementation of Heligman-Pollard estimator
             * fits the curve to points rather than intervals, and means are severely deviated  
             */
            int ppy = 10;
            double[] yy = new HeligmanPollard_R(bins).curve(ppy);
            double[] xxx = Bins.ppy_x(bins, ppy);
            String title = "HP curve " + debug_title;
            ChartXYSplineAdvanced chart = new ChartXYSplineAdvanced(title, "x", "y").showSplinePane(false);
            chart.addSeries("qx", xxx, yy);
            chart.addSeries("bins", xxx, Bins.ppy_y(bins, ppy));
            chart.display();
            double[] y = Bins.ppy2yearly(yy, ppy);
            CurveVerifier.validate_means(y, bins);
            Util.noop();
        }
        
        if (Util.False)
        {
            int [] x = Bins.start_x(bins);
            double[] y = Bins.midpoint_y(bins);
            Clipboard.put(", " , x, y);
            Util.noop();
        }
        
        // CurveSegmentTrend[] trends = CurveUtil.getUShapeSegmentTrends(bins, debug_title);

        final int ppy = 10; // ###
        
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
            double[] xxx = Bins.ppy_x(xbins, ppy);
            String title = "MP-integral sub-yearly curve " + debug_title;
            ChartXYSplineAdvanced chart = new ChartXYSplineAdvanced(title, "x", "y").showSplinePane(false);
            chart.addSeries("qx", xxx, yyy);
            chart.addSeries("bins", xxx, Bins.ppy_y(xbins, ppy));
            chart.display();
        }

        double[] yy = Bins.ppy2yearly(yyy, ppy);
        if (Util.False)
        {
            /*
             * Display yearly curve
             */
            double[] xxx = Bins.ppy_x(bins, 1);
            String title = "MP-integral yearly curve " + debug_title;
            ChartXYSplineAdvanced chart = new ChartXYSplineAdvanced(title, "x", "y").showSplinePane(false);
            chart.addSeries("qx", xxx, yy);
            chart.addSeries("bins", xxx, Bins.ppy_y(bins, 1));
            chart.display();
        }

        CurveVerifier.positive(yy, bins, debug_title, true);
        // ### new EnsureMonotonicYearlyPoints(bins, yy, debug_title).fix();
        CurveVerifier.verifyUShape(yy, bins, false, debug_title, false);
        CurveVerifier.validate_means(yy, bins);

        return yy;
    }

    @SuppressWarnings("unused")
    private static double[] curve_2(Bin[] bins, String debug_title) throws Exception
    {
        final int ppy = 10; // ###

        CurveVerifier.verifyUShape(bins, false, debug_title, true);

        InterpolateAsMeanPreservingCurve.Options options = new InterpolateAsMeanPreservingCurve.Options();
        options = options.debug_title(debug_title).ensurePositive(true).ensureMonotonicallyDecreasing_1_4_5_9(true);
        options = options.ppy(ppy).displayCurve(false); // ###
        double[] curve = InterpolateAsMeanPreservingCurve.curve(bins, options);

        CurveVerifier.positive(curve, bins, debug_title, true);
        CurveVerifier.verifyUShape(curve, bins, debug_title, false);
        CurveVerifier.validate_means(curve, bins);
        return curve;
    }

    @SuppressWarnings("unused")
    private static double[] curve_3(Bin[] bins, String debug_title) throws Exception
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
