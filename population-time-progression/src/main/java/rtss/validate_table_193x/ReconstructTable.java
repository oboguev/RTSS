package rtss.validate_table_193x;

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

import static rtss.data.population.forward.ForwardPopulation.years2days;

/*
 * Построить таблицу смертности населения СССР для 1937-1938 гг.
 */
public class ReconstructTable
{
    public static void main(String[] args)
    {
        try
        {
            new ReconstructTable().do_main();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception");
            ex.printStackTrace();
        }
    }

    private void do_main() throws Exception
    {
        // ### smooth = false?
        final boolean DoSmoothPopulation = false;

        final PopulationByLocality p1937_original = un100(PopulationByLocality.census(Area.USSR, 1937)).smooth(DoSmoothPopulation);
        final PopulationContext p1937 = new Adjust_1937().adjust(p1937_original).toPopulationContext().toTotal();

        final PopulationByLocality p1939_original = un100(PopulationByLocality.census(Area.USSR, 1939)).smooth(DoSmoothPopulation);
        final PopulationContext p1939 = new Adjust_1939().adjust(Area.USSR, p1939_original).toPopulationContext().toTotal();

        /*
         * Сдвинуть структуру населения по переписи 1939 года вниз по возрасту 
         * на размер промежутка между периписями 
         */
        PopulationContext p1939_down = move_down_1939(p1939);

        new PopulationChart("Соотношение между слоями населения по переписям 1937 и 1939 года")
                .show("1937", p1937)
                .show("1939", p1939_down)
                .display();

        buildTable(p1937.clone(), p1939_down);
    }

    private void buildTable(PopulationContext p1, PopulationContext p2) throws Exception
    {
        // buildTable(p1, p2, Gender.BOTH);
        buildTable(p1, p2, Gender.MALE);
        buildTable(p1, p2, Gender.FEMALE);
    }

    private void buildTable(PopulationContext p1, PopulationContext p2, Gender gender) throws Exception
    {
        double[] v1 = p1.toPopulation().asArray(gender);
        double[] v2 = p2.toPopulation().asArray(gender);
        buildTable(v1, v2, gender);
    }

    private void buildTable(double[] v1, double[] v2, Gender gender) throws Exception
    {
        for (int age = 0; age <= Population.MAX_AGE; age++)
        {
            double vp1 = v1[age];
            double vp2 = v2[age];

            Util.assertion(vp1 > 0 && vp2 > 0);

            double f = 1 - vp2 / vp1;
            if (f <= 0 || f >= 1)
                Util.err(String.format("%s %-3d %6.2f %8s => %8s", gender.name(), age, f, f2s(vp1), f2s(vp2)));
            // Util.assertion(f >= 0 && f <= 1);
        }

        // ### диффиузно распределить избыток
    }

    private String f2s(double v)
    {
        String s = String.format("%,15.0f", v);
        while (s.startsWith(" "))
            s = s.substring(1);
        return s;
    }

    /* ============================================================================================================= */

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

        double f98 = within_open_range(v98 / v97, 0, 1);
        double f99 = within_open_range(v99 / v97, 0, 1);
        double f100 = within_open_range(v100 / v97, 0, 1);

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

    /* ============================================================================================================= */

    /*
     * Устранить накопление возрастов 100+ в возрасте 100 (оставив значение только для самого возраста 100),
     * а также убрать "неизвестные возраста"
     */
    private PopulationByLocality un100(PopulationByLocality p) throws Exception
    {
        Population urban = p.forLocality(Locality.URBAN);
        Population rural = p.forLocality(Locality.RURAL);
        return new PopulationByLocality(un100(urban), un100(rural));
    }

    private Population un100(Population p) throws Exception
    {
        p = p.clone();
        p.resetUnknown();
        p.recalcTotal();

        un100(p, Gender.MALE);
        un100(p, Gender.FEMALE);

        p.recalcTotal();
        p.makeBoth();

        return p;
    }

    private void un100(Population p, Gender gender) throws Exception
    {
        @SuppressWarnings("all")
        boolean cond = (Population.MAX_AGE == 100);
        Util.assertion(cond);

        double v97 = p.get(gender, 97);
        double v98 = p.get(gender, 98);
        double v99 = p.get(gender, 99);

        double f = v99 / ((v97 + v98) / 2);

        f = within_open_range(f, 0, 1);

        p.set(gender, 100, v99 * f);
    }

    private double within_open_range(double f, double fmin, double fmax) throws Exception
    {
        Util.assertion(f > fmin && f < fmax);
        return f;
    }
}
