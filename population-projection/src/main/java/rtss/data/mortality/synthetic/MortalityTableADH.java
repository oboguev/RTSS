package rtss.data.mortality.synthetic;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
// import rtss.data.curves.CurveSegmentTrend;
// import rtss.data.curves.CurveUtil;
// import rtss.data.curves.EnsureMonotonicYearlyPoints;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.MortalityInfo;
import rtss.data.mortality.MortalityUtil;
import rtss.data.mortality.SingleMortalityTable;
import rtss.data.population.struct.Population;
import rtss.data.population.synthetic.PopulationADH;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
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
 *     - по ходу кривой Хелигмана-Полларда с коэффицентами подобранными под данные АДХ (curve_hp)
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
    static private ConcurrentHashMap<Area, String> FilesVersion = new ConcurrentHashMap<>();

    static
    {
        FilesVersion.put(Area.USSR, "ADH");
        FilesVersion.put(Area.RSFSR, "ADH");
    }

    public static synchronized String setFilesVersion(Area area, String filesVersion)
    {
        String previous = FilesVersion.get(area);
        FilesVersion.put(area, filesVersion);
        return previous;
    }

    public static synchronized CombinedMortalityTable getMortalityTable(Area area, int year) throws Exception
    {
        String path = null;
        if (FilesVersion.get(area) == null || FilesVersion.get(area).length() == 0)
            path = String.format("mortality_tables/%s/%s", area.name(), year);
        else
            path = String.format("mortality_tables/%s/%s/%s", area.name(), FilesVersion.get(area), year);

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
    private static CombinedMortalityTable get(Area area, int year) throws Exception
    {
        String debug_title_male = String.format("АДХ-%s %s %s", area.toString(), year, Gender.MALE.name());
        String debug_title_female = String.format("АДХ-%s %s %s", area.toString(), year, Gender.FEMALE.name());

        CombinedMortalityTable cmt = CombinedMortalityTable.newEmptyTable();

        String path = String.format("mortality_tables/%s/%s-MortalityRates-ADH.xlsx", area.name(), area.name());
        Bin[] male_mortality_bins = MortalityRatesFromExcel.loadRates(path, Gender.MALE, year);
        Bin[] female_mortality_bins = MortalityRatesFromExcel.loadRates(path, Gender.FEMALE, year);

        /*
         * Значения в файле (и таблице приложения 5 книги АДХ) приведены в формате "mx", 
         * кроме возраста 0, который уже q0.
         * Преобразовать значения "mx" в формат "qx".
         */
        if (Util.True)
        {
            if (male_mortality_bins[0].widths_in_years != 1 || female_mortality_bins[0].widths_in_years != 1)
                throw new Exception("Неожиданная структура записи");

            double mq0 = male_mortality_bins[0].avg;
            double fq0 = female_mortality_bins[0].avg;

            male_mortality_bins = Bins.multiply(male_mortality_bins, 0.001);
            male_mortality_bins = MortalityUtil.mx2qx(male_mortality_bins, Gender.MALE);
            male_mortality_bins = Bins.multiply(male_mortality_bins, 1000.0);

            female_mortality_bins = Bins.multiply(female_mortality_bins, 0.001);
            female_mortality_bins = MortalityUtil.mx2qx(female_mortality_bins, Gender.FEMALE);
            female_mortality_bins = Bins.multiply(female_mortality_bins, 1000.0);

            male_mortality_bins[0].avg = mq0;
            female_mortality_bins[0].avg = fq0;
        }

        Population p = PopulationADH.getPopulation(area, year);
        Bin[] male_population_sum_bins = p.binSumByAge(Gender.MALE, male_mortality_bins);
        Bin[] female_population_sum_bins = p.binSumByAge(Gender.FEMALE, female_mortality_bins);

        fix_80_85_100(male_mortality_bins, male_population_sum_bins);
        fix_80_85_100(female_mortality_bins, female_population_sum_bins);

        fix_40_44(female_mortality_bins, female_population_sum_bins);

        CombinedMortalityTable modelMt = modelTableFor(area, year);

        cmt.setTable(Locality.TOTAL,
                     Gender.MALE,
                     BuildSingleTable.makeSingleTable(male_mortality_bins,
                                                      p.asArray(Gender.MALE),
                                                      debug_title_male,
                                                      modelTable(modelMt, Locality.TOTAL, Gender.MALE)));

        cmt.setTable(Locality.TOTAL,
                     Gender.FEMALE,
                     BuildSingleTable.makeSingleTable(female_mortality_bins,
                                                      p.asArray(Gender.FEMALE),
                                                      debug_title_female,
                                                      modelTable(modelMt, Locality.TOTAL, Gender.FEMALE)));

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

    private static SingleMortalityTable modelTable(CombinedMortalityTable mt, Locality locality, Gender gender) throws Exception
    {
        if (mt == null)
            return null;
        else
            return mt.getSingleTable(locality, gender);
    }
    
    /*
     * Модельная таблица применяется для задания общей формы "хвоста" qx старших возрастов
     */
    private static CombinedMortalityTable modelTableFor(Area area, int year) throws Exception
    {
        if (area == Area.RSFSR && year >= 1954)
        {
            return CombinedMortalityTable.loadCached("RSFSR/1958-1959");
        }
        else if (year <= 1927)
        {
            return CombinedMortalityTable.loadCached("USSR/1926-1927");
        }
        else if (year > 1927 && year < 1938)
        {
            CombinedMortalityTable cmt1 = CombinedMortalityTable.loadCached("USSR/1926-1927");
            CombinedMortalityTable cmt2 = CombinedMortalityTable.loadCached("USSR/1938-1939");
            return CombinedMortalityTable.interpolate(cmt1, cmt2, weight(year, 1927, 1938));
        }
        else if (year == 1938 || year == 1939)
        {
            return CombinedMortalityTable.loadCached("USSR/1938-1939");
        }
        else if (year > 1939 && year < 1958)
        {
            CombinedMortalityTable cmt1 = CombinedMortalityTable.loadCached("USSR/1938-1939");
            CombinedMortalityTable cmt2 = CombinedMortalityTable.loadCached("USSR/1958-1959");
            return CombinedMortalityTable.interpolate(cmt1, cmt2, weight(year, 1939, 1958));
        }
        else if (year >= 1958)
        {
            return CombinedMortalityTable.loadCached("USSR/1958-1959");
        }
        else
        {
            return null;
        }
    }
    
    private static double weight(int year, int y1, int y2)
    {
        if (!(y2 > y1))
            throw new IllegalArgumentException();

        double y = year;
        double w = (y - y1) / (y2 - y1);
        if (w < 0)
            w = 0;
        if (w > 1)
            w = 1;
        return w;
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

    @SuppressWarnings("unused")
    private static void display(CombinedMortalityTable cmt, Locality locality, Gender gender) throws Exception
    {
        double[] qx = cmt.getSingleTable(locality, gender).qx();

        Util.print(cmt.comment() + " qx", qx, 0);

        new ChartXYSplineAdvanced(cmt.comment() + " qx", "age", "mortality").showSplinePane(false)
                .addSeries("qx", qx)
                .display();
    }
}
