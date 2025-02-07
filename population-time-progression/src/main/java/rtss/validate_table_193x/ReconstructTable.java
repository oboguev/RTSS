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
        final boolean DoSmoothPopulation = true;

        final PopulationByLocality p1937_original = un100(PopulationByLocality.census(Area.USSR, 1937)).smooth(DoSmoothPopulation);
        final PopulationContext p1937 = new Adjust_1937().adjust(p1937_original).toPopulationContext().toTotal();

        final PopulationByLocality p1939_original = un100(PopulationByLocality.census(Area.USSR, 1939)).smooth(DoSmoothPopulation);
        final PopulationContext p1939 = new Adjust_1939().adjust(Area.USSR, p1939_original).toPopulationContext().toTotal();

        // ### fill ages 98-100 after move down

        buildTable(p1937.clone(), p1939.moveDownByDays(2 * 365 + 11));
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
     * Устранить накопление возрастов 100+ в возрасте 100,
     * также убрать "неизвестные возраста"
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
        
        double vv = v99 / ((v97 + v98) / 2);
        
        Util.assertion(vv > 0 && vv < 1);
        
        p.set(gender, 100, v99 * vv);
    }
}
