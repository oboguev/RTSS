package rtss.validate_table_193x;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.forward.ForwardPopulationT;
import rtss.data.population.forward.ForwardPopulationUR;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.forward_1926_193x.Adjust_1937;
import rtss.forward_1926_193x.Adjust_1939;
import rtss.util.Util;

/*
 * Приложить таблицу смертности ГКС 1938-1939 для передвижки населения переписи 1937 года 
 * к моменту переписи 1939 года. Сравнить результаты. 
 */
public class ApplyTable
{
    public static void main(String[] args)
    {
        try
        {
            new ApplyTable().do_main("mortality_tables/USSR/1938-1939");
        }
        catch (Throwable ex)
        {
            Util.err("** Exception");
            ex.printStackTrace();
        }
    }

    private ApplyTable() throws Exception
    {
    }

    private void do_main(String tablePath) throws Exception
    {
        final boolean DoSmoothPopulation = true;

        final PopulationByLocality p1937_original = PopulationByLocality.census(Area.USSR, 1937).smooth(DoSmoothPopulation);
        final PopulationContext p1937 = new Adjust_1937().adjust(p1937_original).toPopulationContext();

        final PopulationByLocality p1939_original = PopulationByLocality.census(Area.USSR, 1939).smooth(DoSmoothPopulation);
        final PopulationContext p1939 = new Adjust_1939().adjust(Area.USSR, p1939_original).toPopulationContext();

        CombinedMortalityTable mt = new CombinedMortalityTable(tablePath);

        PopulationContext p = forward_without_births(p1937, p1939, mt, true);
        Util.noop();
    }

    /* ================================================================================================================== */

    private PopulationContext forward_without_births(
            final PopulationContext p1937,
            final PopulationContext p1939,
            final CombinedMortalityTable mt,
            boolean ur) throws Exception
    {
        if (ur)
            return forward_without_births_ur(p1937, p1939, mt);
        else
            return forward_without_births_t(p1937, p1939, mt);
    }

    private PopulationContext forward_without_births_ur(
            final PopulationContext p1937,
            final PopulationContext p1939,
            final CombinedMortalityTable mt) throws Exception
    {
        double urban_male_fraction_1937 = urban_fraction(p1937, Gender.MALE);
        double urban_male_fraction_1939 = urban_fraction(p1939, Gender.MALE);
        double urban_male_fraction_1938 = (urban_male_fraction_1937 + urban_male_fraction_1939) / 2;

        double urban_female_fraction_1937 = urban_fraction(p1937, Gender.FEMALE);
        double urban_female_fraction_1939 = urban_fraction(p1939, Gender.FEMALE);
        double urban_female_fraction_1938 = (urban_female_fraction_1937 + urban_female_fraction_1939) / 2;

        PopulationContext p = p1937.clone();

        /* 1937 -> 1938 */
        ForwardPopulationUR fw = new ForwardPopulationUR();
        fw.setBirthRateUrban(0);
        fw.setBirthRateRural(0);
        fw.forward(p, mt, 1.0);

        /*
         * Перераспределить население между городским и сельским, отражая урбанизацию 
         */
        urbanize(p, Gender.MALE, urban_male_fraction_1938);
        urbanize(p, Gender.FEMALE, urban_female_fraction_1938);

        /* 1938 -> 1939 */
        fw = new ForwardPopulationUR();
        fw.setBirthRateUrban(0);
        fw.setBirthRateRural(0);
        fw.forward(p, mt, 1.0);

        /* последний отрезок в 11 дней */
        fw = new ForwardPopulationUR();
        fw.setBirthRateUrban(0);
        fw.setBirthRateRural(0);
        fw.forward(p, mt, 0.03);

        return p;
    }

    private PopulationContext forward_without_births_t(
            final PopulationContext p1937,
            final PopulationContext p1939,
            final CombinedMortalityTable mt) throws Exception
    {
        double urban_male_fraction_1937 = urban_fraction(p1937, Gender.MALE);
        double urban_male_fraction_1939 = urban_fraction(p1939, Gender.MALE);
        double urban_male_fraction_1938 = (urban_male_fraction_1937 + urban_male_fraction_1939) / 2;

        double urban_female_fraction_1937 = urban_fraction(p1937, Gender.FEMALE);
        double urban_female_fraction_1939 = urban_fraction(p1939, Gender.FEMALE);
        double urban_female_fraction_1938 = (urban_female_fraction_1937 + urban_female_fraction_1939) / 2;

        PopulationContext p = p1937.clone();

        /* 1937 -> 1938 */
        ForwardPopulationT fw = new ForwardPopulationT();
        fw.setBirthRateTotal(0);
        fw.forward(p, mt, 1.0);

        /*
         * Перераспределить население между городским и сельским, отражая урбанизацию 
         */
        urbanize(p, Gender.MALE, urban_male_fraction_1938);
        urbanize(p, Gender.FEMALE, urban_female_fraction_1938);

        /* 1938 -> 1939 */
        fw = new ForwardPopulationT();
        fw.setBirthRateTotal(0);
        fw.forward(p, mt, 1.0);

        /* последний отрезок в 11 дней */
        fw = new ForwardPopulationT();
        fw.setBirthRateTotal(0);
        fw.forward(p, mt, 0.03);

        return p;
    }

    /* ================================================================================================================== */

    public double urban_fraction(PopulationContext p, Gender gender) throws Exception
    {
        double urban = p.sumDays(Locality.URBAN, gender, 0, p.MAX_DAY);
        double total = p.sumDays(Locality.TOTAL, gender, 0, p.MAX_DAY);
        return urban / total;
    }

    /*
     * Перенести часть населения из сельского в городское для достижения указанного уровня урбанизации.
     * Мы вычисляем требуемое количество передвижения и переносим это население.
     * Перенос прилагается только к возрастным группам 0-49, сельские группы в возрасте 50+ остаются неизменными.
     * В группах 0-49 перенос распределяется равномерно, пропорционально численности этих групп.  
     */
    public void urbanize(
            PopulationContext p,
            final Gender gender,
            final double target_urban_level)
            throws Exception
    {
        /*
         * Целевая и текущая численность городского населения во всех возрастах
         */
        double total_population = p.sumAllAges(Locality.TOTAL, gender);
        double target_urban = target_urban_level * total_population;
        double current_urban = p.sumAllAges(Locality.URBAN, gender);

        /*
         * Численность населения, которое нужно перенести 
         */
        double move = target_urban - current_urban;
        if (move < 0)
            throw new Exception("Negative rural -> urban movement amount");

        /*
         * Мы переносим население из сельского в городское в возрастных группах 0-49.
         * Группы 50+ остаются неизменными.
         * Переносимое население распределяется равномерно между возрастами 0-49 
         * пропорционально их численности в сельском населении.
         */
        double rural049 = p.sumAllAges(Locality.RURAL, gender);

        double factor = move / rural049;

        for (int age = 0; age <= 49; age++)
        {
            for (int nd = p.firstDayForAge(age); nd <= p.lastDayForAge(age); nd++)
            {
                double r2 = p.getDay(Locality.RURAL, gender, nd);
                move = r2 * factor;
                p.subDay(Locality.RURAL, gender, nd, move);
                p.addDay(Locality.URBAN, gender, nd, move);
            }
        }

        /*
         * Verify that new level (of transformed population) is correct
         */
        if (Util.differ(target_urban_level, urban_fraction(p, gender), 0.4 * 0.01))
            throw new Exception("Miscalculated urbanization");
    }
}
