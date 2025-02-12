package rtss.validate_table_193x;

import static rtss.data.population.projection.ForwardPopulation.years2days;

import rtss.data.ValueConstraint;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.synthetic.PatchMortalityTable;
import rtss.data.mortality.synthetic.PatchMortalityTable.PatchInstruction;
import rtss.data.mortality.synthetic.PatchMortalityTable.PatchOpcode;
import rtss.data.population.projection.ForwardPopulationT;
import rtss.data.population.projection.ForwardPopulationUR;
import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.forward_1926_193x.Adjust_1937;
import rtss.forward_1926_193x.Adjust_1939;
import rtss.util.Util;
import rtss.util.plot.PopulationChart;

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
        // final PopulationContext p1937 = p1937_original.toPopulationContext();
        final PopulationContext p1937 = new Adjust_1937().adjust(p1937_original).toPopulationContext();

        final PopulationByLocality p1939_original = PopulationByLocality.census(Area.USSR, 1939).smooth(DoSmoothPopulation);
        final PopulationContext p1939 = new Adjust_1939().adjust(Area.USSR, p1939_original).toPopulationContext();

        /*
         * Сдвинуть структуру населения по переписи 1939 года вниз по возрасту 
         * на размер промежутка между периписями 
         */
        PopulationContext p1939_down = move_down_1939(p1939.toTotal());
        PopulationChart.chart("Сравнение возрастных слоёв по переписям 1937 и 1939 гг.")
                .show("1937", p1937.toTotal())
                .show("1939", p1939_down)
                .display();
        
        // print_difference(p1939_down, p1937.toTotal());

        CombinedMortalityTable mt = new CombinedMortalityTable(tablePath);

        Util.out("Множитель коэффциентов смертности для схождения численности населения (в возрастах 3-100 лет) январь 1937 -> январь 1939 по передвижке");
        Util.out("к численности по переписи января 1939 года:");
        Util.out("возраст --- для раздельной передвижки городского и сельского населения --- для передвижки без разбивки населения по типу местности");
        
        Double final_multiplier_t = null;
        
        for (int cutoffAge = 3; cutoffAge <= 20; cutoffAge++)
        {
            double multiplier_t = find_multiplier(p1937.toTotal(), p1939.toTotal(), mt, false, cutoffAge);
            double multiplier_ur = find_multiplier(p1937, p1939, mt, true, cutoffAge);
            Util.out(String.format("%-2d %.4f %.4f", cutoffAge, multiplier_ur, multiplier_t));

            if (Util.False)
            {
                double diff_t = difference(p1937.toTotal(), p1939.toTotal(), mt, multiplier_t, false, 3);
                double diff_ur = difference(p1937, p1939, mt, multiplier_ur, true, 3);
                Util.unused(diff_t, diff_ur);
            }
            
            if (final_multiplier_t == null)
                final_multiplier_t = multiplier_t;
        }

        show_divergence(p1937, p1939, mt, final_multiplier_t);
        show_divergence(p1937, p1939, mt, 1.4);

        Util.noop();
    }
    
    private void show_divergence(PopulationContext p1937, PopulationContext p1939, CombinedMortalityTable mt, double multiplier) throws Exception
    {
        PopulationContext p = forward_without_births(p1937.toTotal(), p1939.toTotal(), mt, multiplier, false, true);
        p = p.selectByAgeYears(3, Population.MAX_AGE);
        p = p.sub(p1939.toTotal().selectByAgeYears(3, Population.MAX_AGE), ValueConstraint.NONE);

        p.display("Расхождение между переписью 1939 года и передвижкой с множителем " + multiplier);
    }

    /* ================================================================================================================== */

    private double find_multiplier(
            final PopulationContext p1937,
            final PopulationContext p1939,
            CombinedMortalityTable mt,
            boolean ur, 
            int cutoffAge) throws Exception
    {
        double mmax = 1.5;
        int mmaxcount = 0;

        double m1 = 0.5;
        double m2 = mmax;

        for (int pass = 0;; pass++)
        {
            if (pass > 10000)
                throw new Exception("вычисление не сходится");

            double m = (m1 + m2) / 2;

            if (Util.False && Math.abs(m1 - m2) < 0.0005)
                return m;

            double d = difference(p1937, p1939, mt, m, ur, cutoffAge);
            if (Math.abs(d) < 100)
                return m;

            if (Math.abs(m1 - m2) < 0.00005 && Math.abs(d) > 40_000)
                return -1;            
            
            if (Math.abs(m1 - m2) < 0.00005 && Math.abs(d) < 20_000)
                return m;
            
            if (Math.abs(m - mmax) < 0.001)
            {
                if (mmaxcount++ >= 5 && Util.False)
                {
                    mmaxcount = 0;
                    mmax += 0.1;
                    m2 += 0.1;
                }
            }

            if (d > 0)
            {
                // передвижка даёт слишком много населения, повысить мультипликатор смертности
                m1 = m;
            }
            else
            {
                // передвижка даёт слишком мало населения, понизить мультипликатор смертности
                m2 = m;
            }
        }
    }

    private double difference(
            final PopulationContext p1937,
            final PopulationContext p1939,
            CombinedMortalityTable mt,
            double multiplier,
            boolean ur, 
            int cutoffAge) throws Exception
    {
        PatchInstruction instruction = new PatchInstruction(PatchOpcode.Multiply, 0, Population.MAX_AGE, multiplier);
        CombinedMortalityTable xmt = PatchMortalityTable.patch(mt, instruction, "с множителем " + multiplier);
        PopulationContext p = forward_without_births(p1937, p1939, xmt, ur, false);
        return difference(p, p1939, cutoffAge);
    }

    private double difference(final PopulationContext p, final PopulationContext p1939, int cutoffAge) throws Exception
    {
        double v_p = p.sumAges(Locality.TOTAL, Gender.BOTH, cutoffAge, Population.MAX_AGE);
        double v_p1939 = p1939.sumAges(Locality.TOTAL, Gender.BOTH, cutoffAge, Population.MAX_AGE);
        return v_p - v_p1939;
    }

    /* ================================================================================================================== */

    private PopulationContext forward_without_births(
            final PopulationContext p1937,
            final PopulationContext p1939,
            final CombinedMortalityTable mt,
            double multiplier,
            final boolean ur,
            boolean printDeaths) throws Exception
    {
        PatchInstruction instruction = new PatchInstruction(PatchOpcode.Multiply, 0, Population.MAX_AGE, multiplier);
        CombinedMortalityTable xmt = PatchMortalityTable.patch(mt, instruction, "с множителем " + multiplier);
        return forward_without_births(p1937, p1939, xmt, ur, printDeaths);
    }

    private PopulationContext forward_without_births(
            final PopulationContext p1937,
            final PopulationContext p1939,
            final CombinedMortalityTable mt,
            final boolean ur,
            boolean printDeaths) throws Exception
    {
        if (ur)
            return forward_without_births_ur(p1937, p1939, mt, printDeaths);
        else
            return forward_without_births_t(p1937, p1939, mt, printDeaths);
    }

    private PopulationContext forward_without_births_ur(
            final PopulationContext p1937,
            final PopulationContext p1939,
            final CombinedMortalityTable mt,
            boolean printDeaths) throws Exception
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
            final CombinedMortalityTable mt,
            boolean printDeaths) throws Exception
    {
        PopulationContext p = p1937.clone();
        
        double deaths = 0;

        /* 1937 -> 1938 */
        ForwardPopulationT fw = new ForwardPopulationT();
        fw.setBirthRateTotal(0);
        fw.forward(p, mt, 1.0);
        deaths += fw.getObservedDeaths();

        /* 1938 -> 1939 */
        fw = new ForwardPopulationT();
        fw.setBirthRateTotal(0);
        fw.forward(p, mt, 1.0);
        deaths += fw.getObservedDeaths();

        /* последний отрезок в 11 дней */
        fw = new ForwardPopulationT();
        fw.setBirthRateTotal(0);
        fw.forward(p, mt, 0.03);
        deaths += fw.getObservedDeaths();
        
        if (printDeaths)
            Util.out(String.format("Число смертей: %,d тыс. чел.", (int) Math.round(deaths / 1000)));

        return p;
    }

    /* ================================================================================================================== */

    public double urban_fraction(final PopulationContext p, final Gender gender) throws Exception
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

    /* ================================================================================================================== */

    /*
     * Сдвинуть структуру населения по переписи 1939 года вниз по возрасту 
     * на размер промежутка между периписями 1937 и 1939 гг. 
     */
    private PopulationContext move_down_1939(final PopulationContext p1939) throws Exception
    {
        PopulationContext p1939_down = p1939.moveDownByDays(2 * 365 + 11);
        fill_upper_ages(p1939_down, p1939, Gender.MALE);
        fill_upper_ages(p1939_down, p1939, Gender.FEMALE);
        return p1939_down;
    }

    private void fill_upper_ages(PopulationContext p1939_down, final PopulationContext p1939, Gender gender) throws Exception
    {
        double v97 = p1939.sumAge(Locality.TOTAL, gender, 97);
        double v98 = p1939.sumAge(Locality.TOTAL, gender, 98);
        double v99 = p1939.sumAge(Locality.TOTAL, gender, 99);
        double v100 = p1939.sumAge(Locality.TOTAL, gender, 100);

        double f98 = within_range(v98 / v97, 0.3, 0.9, 0.7);
        double f99 = within_range(v99 / v97, 0.3, 0.9, 0.6);
        double f100 = within_range(v100 / v97, 0.3, 0.9, 0.5);

        fill_upper_ages(p1939_down, gender, 97, 98, f98);
        fill_upper_ages(p1939_down, gender, 97, 99, f99);
        fill_upper_ages(p1939_down, gender, 97, 100, f100);
    }

    private void fill_upper_ages(PopulationContext p1939_down, Gender gender, int year_age_from, int year_age_to, double f) throws Exception
    {
        int from_nd1 = years2days(year_age_from);
        int from_nd2 = from_nd1 + 365 - 1;
        int to_nd1 = years2days(year_age_to);

        for (int nd = from_nd1; nd <= from_nd2; nd++)
        {
            double v = p1939_down.getDay(Locality.TOTAL, gender, nd);
            p1939_down.setDay(Locality.TOTAL, gender, to_nd1 + (nd - from_nd1), v * f);
        }
    }

    private double within_range(double f, double fmin, double fmax, double fallback)
    {
        if (f > fmin && f < fmax)
            return f;
        else
            return fallback;
    }
    
    /* ====================================================================================================== */

    @SuppressWarnings("unused")
    private void print_difference(PopulationContext p1939_down, PopulationContext p1937) throws Exception
    {
        for (Gender gender : Gender.TwoGenders)
        {
            Util.out("");
            Util.out("Нехватка в переписи 1937 года для пола " + gender);
            for (int age = 16; age <= 32; age++)
            {
                double v39 = p1939_down.getYearValue(gender, age); 
                double v37 = p1937.getYearValue(gender, age);
                double v = Math.max(0, v39 - v37);
                Util.out(String.format("%d %,d", age, (int) Math.ceil(v)));
            }
        }
    }
}
