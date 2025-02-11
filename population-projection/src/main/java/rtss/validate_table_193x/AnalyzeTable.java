package rtss.validate_table_193x;

import java.util.HashMap;
import java.util.Map;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.struct.Population;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

/*
 * Распечатать половые пропорции и пропорции городского и сельского населения 
 * заключённые в таблице смертности. 
 */
public class AnalyzeTable
{
    public static void main(String[] args)
    {
        try
        {
            new AnalyzeTable().analyze("mortality_tables/USSR/1938-1939");
        }
        catch (Throwable ex)
        {
            Util.err("** Exception");
            ex.printStackTrace();
        }
    }

    private Map<String, String> ratios = new HashMap<>();
    private final String nbsp = "" + (char) 0xA0;

    private void analyze(String tablePath) throws Exception
    {
        CombinedMortalityTable mt = new CombinedMortalityTable(tablePath);

        for (Locality locality : Locality.AllLocalities)
            genderProportionForLocality(mt, locality);

        Util.out("% мужского населения в совокупном, городском, сельском и год рождения");
        for (int age = 0; age <= Population.MAX_AGE; age++)
        {
            String t = ratios.get(key(age, Locality.TOTAL));
            String u = ratios.get(key(age, Locality.URBAN));
            String r = ratios.get(key(age, Locality.RURAL));
            Util.out(String.format("%-3d %12s %12s %12s     %4d", age, t, u, r, 1938 - age));
        }

        ratios.clear();

        for (Gender gender : Gender.ThreeGenders)
            urbanProportionForGender(mt, gender);

        Util.out("");
        Util.out("% городского населения в совокупном, мужском, женском и год рождения");
        for (int age = 0; age <= Population.MAX_AGE; age++)
        {
            String b = ratios.get(key(age, Gender.BOTH));
            String m = ratios.get(key(age, Gender.MALE));
            String f = ratios.get(key(age, Gender.FEMALE));
            Util.out(String.format("%-3d %12s %12s %12s     %4d", age, b, m, f, 1938 - age));
        }
    }

    /* ========================================================================================================================= */
    
    private String key(int age, Locality locality)
    {
        return locality.name() + "." + age;
    }

    private void genderProportionForLocality(CombinedMortalityTable mt, Locality locality) throws Exception
    {
        double[] qx_both = mt.getSingleTable(locality, Gender.BOTH).qx();
        double[] qx_male = mt.getSingleTable(locality, Gender.MALE).qx();
        double[] qx_female = mt.getSingleTable(locality, Gender.FEMALE).qx();

        Double[] male_fraction = male_fraction(qx_both, qx_male, qx_female);

        for (int age = 0; age <= Population.MAX_AGE; age++)
        {
            Double v = male_fraction[age];
            String s = "????";
            if (v != null)
            {
                s = String.format("%5.1f", v * 100.0);
                s = appendPrecision(s, qx_male[age], qx_female[age]);
            }

            ratios.put(key(age, locality), s);
        }
    }

    /*
     * Вычислить долю мужского населения, по годам возраста, использованную при составлении таблицы
     */
    private static Double[] male_fraction(double[] qx_both, double[] qx_male, double[] qx_female) throws Exception
    {
        Util.assertion(qx_both.length == qx_male.length);
        Util.assertion(qx_both.length == qx_female.length);

        Double[] male_fraction = new Double[qx_both.length];

        for (int age = 0; age < qx_both.length; age++)
        {
            // если мужская и женская смертности точно совпадают, пропорции следует определять из данных о населении
            // или интерполяцией соседних значений пропорции
            if (qx_male[age] == qx_female[age])
            {
                male_fraction[age] = null;
            }
            else
            {
                male_fraction[age] = (qx_both[age] - qx_female[age]) / (qx_male[age] - qx_female[age]);
            }
        }

        // проверка мниимальной разумности значений
        for (int age = 0; age < qx_both.length; age++)
            Util.assertion(male_fraction[age] >= 0 && male_fraction[age] <= 1);

        return male_fraction;
    }

    /* ========================================================================================================================= */

    private String key(int age, Gender gender)
    {
        return gender.name() + "." + age;
    }

    /*
     * Вычислить долю городскоого населения, по годам возраста, использованную при составлении таблицы
     */
    private void urbanProportionForGender(CombinedMortalityTable mt, Gender gender) throws Exception
    {
        double[] qx_total = mt.getSingleTable(Locality.TOTAL, gender).qx();
        double[] qx_urban = mt.getSingleTable(Locality.URBAN, gender).qx();
        double[] qx_rural = mt.getSingleTable(Locality.RURAL, gender).qx();

        Double[] urban_fraction = urban_fraction(qx_total, qx_urban, qx_rural);

        for (int age = 0; age <= Population.MAX_AGE; age++)
        {
            Double v = urban_fraction[age];
            String s = "????";
            if (v != null)
            {
                s = String.format("%.1f", v * 100.0);
                s = appendPrecision(s, qx_urban[age], qx_rural[age]);
            }

            ratios.put(key(age, gender), s);
        }
    }

    private Double[] urban_fraction(double[] qx_total, double[] qx_urban, double[] qx_rural) throws Exception
    {
        Util.assertion(qx_total.length == qx_urban.length);
        Util.assertion(qx_total.length == qx_rural.length);

        Double[] urban_fraction = new Double[qx_total.length];

        for (int age = 0; age < qx_total.length; age++)
        {
            // если городская и сельская смертности точно совпадают, пропорции следует определять из данных о населении
            // или интерполяцией соседних значений пропорции
            if (qx_urban[age] == qx_rural[age])
            {
                urban_fraction[age] = null;
            }
            else
            {
                urban_fraction[age] = (qx_total[age] - qx_rural[age]) / (qx_urban[age] - qx_rural[age]);
            }
        }

        // проверка мниимальной разумности значений
        for (int age = 0; age < qx_total.length; age++)
        {
            Util.assertion(urban_fraction[age] == null ||
                           urban_fraction[age] >= 0 && urban_fraction[age] <= 1);
        }

        return urban_fraction;
    }

    /* ========================================================================================================================= */

    /*
     * Последний десятичный знак в опубликованных коэффициентах
     */
    private final double precision = 0.00001;
    
    private String appendPrecision(String s, double v1, double v2)
    {
        double dv = Math.abs(v1 - v2);
        
        int play = 0;
        
        if (dv <= precision * 1.01)
        {
            play = 100;
        }
        else if (dv <= precision * 20.01)
        {
            int n = (int) Math.round(dv / precision);
            play = Math.round(100 / n);
        }
        else
        {
            // do nothing
        }
        
        if (play != 0)
        {
            s = s + nbsp + "±" + nbsp + play;
        }
        
        return s;
    }
}
