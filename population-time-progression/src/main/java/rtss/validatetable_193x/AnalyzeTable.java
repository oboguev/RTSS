package rtss.validatetable_193x;

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

    private Map<String, Double> ratios = new HashMap<>();

    private void analyze(String tablePath) throws Exception
    {
        CombinedMortalityTable mt = new CombinedMortalityTable(tablePath);

        for (Locality locality : Locality.AllLocalities)
            genderProportionForLocality(mt, locality);

        Util.out("% мужского населения в совокупном, городском, сельском и год рождения");
        for (int age = 0; age <= Population.MAX_AGE; age++)
        {
            double t = 100.0 * ratios.get(key(age, Locality.TOTAL));
            double u = 100.0 * ratios.get(key(age, Locality.URBAN));
            double r = 100.0 * ratios.get(key(age, Locality.RURAL));
            Util.out(String.format("%-3d %5.1f %5.1f %5.1f %4d", age, t, u, r, 1938 - age));
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
        double[] male_fraction = male_fraction(qx_both, qx_male, qx_female);

        for (int age = 0; age <= Population.MAX_AGE; age++)
            ratios.put(key(age, locality), male_fraction[age]);
    }

    /*
     * Вычислить долю мужского населения, по годам возраста, использованную при составлении таблицы.
     * Функция опредляет величину из самой таблицы.
     * На практике, таблица ГКС 1938-1939 гг. выказывает значительные вариации в половой пропорции,
     * со значиями доли мужского пола колеблющимися от 0.25 до 0.66.
     */
    private static double[] male_fraction(double[] qx_both, double[] qx_male, double[] qx_female) throws Exception
    {
        Util.assertion(qx_both.length == qx_male.length);
        Util.assertion(qx_both.length == qx_female.length);

        double[] male_fraction = new double[qx_both.length];

        for (int age = 0; age < qx_both.length; age++)
        {
            // если мужская и женская смертности точно совпадают, пропорции следует определять из данных о населении
            // или интерполяцией соседних значений пропорции
            Util.assertion(qx_male[age] != qx_female[age]);
            male_fraction[age] = (qx_both[age] - qx_female[age]) / (qx_male[age] - qx_female[age]);
        }
        // проверка мниимальной разумности значений
        for (int age = 0; age < qx_both.length; age++)
        {
            Util.assertion(male_fraction[age] >= 0 && male_fraction[age] <= 1);
        }

        return male_fraction;
    }
}
