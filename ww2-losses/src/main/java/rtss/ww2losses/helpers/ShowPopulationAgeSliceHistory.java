package rtss.ww2losses.helpers;

import rtss.data.selectors.Gender;
import rtss.util.Util;
import rtss.ww2losses.params.AreaParameters;
import rtss.ww2losses.struct.HalfYearEntries;
import rtss.ww2losses.struct.HalfYearEntry;
import rtss.ww2losses.struct.HalfYearEntries.HalfYearSelector;

import static rtss.data.population.projection.ForwardPopulation.years2days;

import rtss.data.population.struct.PopulationContext;

/*
 * Распечатать движение населения определённых возрастов сквозь период войны
 */
public class ShowPopulationAgeSliceHistory
{
    public static void showWithoutBirhts(AreaParameters ap, PopulationContext p1946_actual, HalfYearEntries<HalfYearEntry> halves, int age) throws Exception
    {
        Util.out("");
        Util.out(String.format("%s, возраст %d (на начало 1941)", ap.area.toString(), age));
        Util.out("                           M      F     M+F");
        Util.out("                         =====  =====  =====");
        
        for (HalfYearEntry he : halves)
        {
            show(he.toString(), he.p_nonwar_without_births, age + he.year - 1941, he.halfyear == HalfYearSelector.SecondHalfYear);
        }
        
        show("1946 фактическое", p1946_actual, age + 1946 - 1941, false);
    }
    
    private static void show(String what, PopulationContext p, double age, boolean secondHalfyear) throws Exception
    {
        if (secondHalfyear)
            age += 0.5;
        
        String sage = String.format("возраст %.1f", age);

        int nd = years2days(age);
        
        double f = p.sumDays(Gender.FEMALE, nd, nd + p.DAYS_PER_YEAR - 1);
        double m = p.sumDays(Gender.MALE, nd, nd + p.DAYS_PER_YEAR - 1);
        double b = p.sumDays(Gender.BOTH, nd, nd + p.DAYS_PER_YEAR - 1);
        
        Util.out(String.format("%-24s %5s  %5s  %5s  [%s]", what, f2k(m), f2k(f), f2k(b), sage));
    }
    
    /* ================================================================================= */

    public static void showActual(HalfYearEntries<HalfYearEntry> halves, double age) throws Exception
    {
        Util.out("");
        Util.out(String.format("Проводка возраста %.1f", age));
        
        int nd1 = years2days(age);

        for (HalfYearEntry he : halves)
        {
            PopulationContext p = he.actual_population;
            
            int nd2 = nd1 + p.DAYS_PER_YEAR - 1;
            
            nd1 = Math.min(nd1, p.MAX_DAY);
            nd2 = Math.min(nd2, p.MAX_DAY);
            double v = p.sumDays(nd1, nd2);
            
            Util.out(String.format("%d.%d %5.1f %f", he.year, he.halfyear.seq(1), age, v));
            
            age += 0.5;
            nd1 += years2days(0.5);
        }
    }
    
    /* ================================================================================= */

    private static String f2k(double v)
    {
        return f2s(v / 1000.0);
    }

    private static String f2s(double v)
    {
        String s = String.format("%,15.0f", v);
        while (s.startsWith(" "))
            s = s.substring(1);
        return s;
    }
}
