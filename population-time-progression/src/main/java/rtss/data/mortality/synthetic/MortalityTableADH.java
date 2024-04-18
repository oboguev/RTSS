package rtss.data.mortality.synthetic;

import java.util.HashMap;
import java.util.Map;

import javax.validation.ConstraintViolationException;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.curves.InterpolateAsMeanPreservingCurve;
import rtss.data.curves.InterpolateUShapeAsMeanPreservingCurve;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.MortalityInfo;
import rtss.data.mortality.SingleMortalityTable;
import rtss.data.population.Population;
import rtss.data.population.synthetic.PopulationADH;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;
import rtss.util.plot.ChartXYSplineAdvanced;

/*
 * Загрузить поло-возрастные покаатели смертности (агреггированные по возрастным группам) из файла Excel
 * и построить на их основании таблицу смертности с годовым шагом.
 */
public class MortalityTableADH
{
    public static final int MAX_AGE = CombinedMortalityTable.MAX_AGE;
    private static Map<String, CombinedMortalityTable> cache = new HashMap<>();

    public static CombinedMortalityTable getMortalityTable(Area area, int year) throws Exception
    {
        return getMortalityTable(area, "" + year);
    }

    public static synchronized CombinedMortalityTable getMortalityTable(Area area, String year) throws Exception
    {
        String path = String.format("mortality_tables/%s/%s", area.name(), year);

        // look in cache
        CombinedMortalityTable cmt = cache.get(path);
        if (cmt != null)
            return cmt;

        // try loading from resource
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

        if (cmt == null)
            cmt = get(area, year);

        cmt.seal();
        cache.put(path, cmt);
        return cmt;
    }

    /*
     * Read data from Excel and generate the table with 1-year resolution
     */
    private static CombinedMortalityTable get(Area area, String year) throws Exception
    {
        CombinedMortalityTable cmt = CombinedMortalityTable.newEmptyTable();

        String path = String.format("mortality_tables/%s/%s-MortalityRates-ADH.xlsx", area.name(), area.name());
        Bin[] male_mortality_bins = MortalityRatesFromExcel.loadRates(path, Gender.MALE, year);
        Bin[] female_mortality_bins = MortalityRatesFromExcel.loadRates(path, Gender.FEMALE, year);

        Population p = PopulationADH.getPopulation(area, year);
        Bin[] male_population_sum_bins = p.binSumByAge(Gender.MALE, male_mortality_bins);
        Bin[] female_population_sum_bins = p.binSumByAge(Gender.FEMALE, female_mortality_bins);
        
        fix_80_85_100(male_mortality_bins, male_population_sum_bins); 
        fix_80_85_100(female_mortality_bins, female_population_sum_bins); 

        cmt.setTable(Locality.TOTAL, Gender.MALE, makeSingleTable(male_mortality_bins));
        cmt.setTable(Locality.TOTAL, Gender.FEMALE, makeSingleTable(female_mortality_bins));

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

    private static SingleMortalityTable makeSingleTable(Bin... bins) throws Exception
    {
        double[] curve = null;
        
        try
        {
            curve = InterpolateAsMeanPreservingCurve.curve(bins);
        }
        catch (ConstraintViolationException ex)
        {
            // ignore
        }
        
        if (curve == null)
            curve = InterpolateUShapeAsMeanPreservingCurve.curve(bins);
        
        curve = Util.divide(curve, 1000);
        return SingleMortalityTable.from_qx("computed", curve);
    }

    @SuppressWarnings("unused")
    private static void display(CombinedMortalityTable cmt, Locality locality, Gender gender) throws Exception
    {
        double[] qx = cmt.getSingleTable(locality, gender).qx();

        Util.print(cmt.comment() + " qx", qx, 0);

        new ChartXYSplineAdvanced(cmt.comment() + " qx", "age", "mortality")
                .addSeries("qx", qx)
                .display();
    }

    /*
     * Для некоторых лет (1927-1933, 1937, 1946-1948) рассчитанная АДХ мужская смертность в возрастной группе 85-100 ниже, 
     * чем в группе 80-84.
     * 
     * Это не только представляется весьма сомнительным фактически, но и вызывает резкий перегиб и немонотонное поведение 
     * строимой кривой смертности в этом возрастном диапазоне. 
     * 
     * Откорректировать значения смертности в этих двух группах таким образом, чтобы общее число смертей осталось неизменным.
     * 
     * Понизить значение смертности в возрасте 80-84 и повысить её для возраста 85-100 так, чтобы смертность в группе 85-100 
     * была в @ratio раз выше, чем в группе 80-84. 
     */
    private static void fix_80_85_100(Bin[] m, final Bin[] psum) throws Exception
    {
        final double ratio = 1.15;

        if (m.length < 3)
            return;

        Bin m2 = Bins.lastBin(m); // 85-100
        Bin m1 = m2.prev; // 80-84
        Bin m0 = m1.prev; // 75-79

        if (m0.age_x1 == 75 && m0.age_x2 == 79 &&
            m1.age_x1 == 80 && m1.age_x2 == 84 &&
            m2.age_x1 == 85 && m2.age_x2 == 100)
        {
            // proceed
        }   
        else
        {
            return;
        }

        if (m1.avg < m2.avg)
            return;

        Bin p2 = Bins.lastBin(psum);
        Bin p1 = p2.prev;

        /* content of psum bins is actually population sum, not average, so do not divide by bin width */
        double deaths = m1.avg * p1.avg + m2.avg * p2.avg;

        double v1 = deaths / (p1.avg + ratio * p2.avg);
        double v2 = ratio * v1;
    
        if (v1 <= m0.avg)
            throw new Exception("Unable to correct inverted mortality rate at 85+");
        
        m1.avg = v1;
        m2.avg = v2;
    
        double deaths2 = m1.avg * p1.avg + m2.avg * p2.avg;
        if (Util.differ(deaths, deaths2))
            throw new Exception("Unable to correct inverted mortality rate at 85+");
    }
}
