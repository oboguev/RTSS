package rtss.ww2losses.helpers;

import rtss.data.population.forward.PopulationContext;
import rtss.data.selectors.Gender;
import rtss.util.Util;
import rtss.ww2losses.HalfYearEntries;
import rtss.ww2losses.HalfYearEntries.HalfYearSelector;
import rtss.ww2losses.HalfYearEntry;
import rtss.ww2losses.params.AreaParameters;

import static rtss.data.population.forward.ForwardPopulation.years2days;

/*
 * Распечатать движение населения определённых возрастов сквозь период войны
 */
public class ShowForecast
{
    public static void show(AreaParameters ap, PopulationContext p1946_actual, HalfYearEntries<HalfYearEntry> halves, int age) throws Exception
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
        
        Util.out(String.format("%-24s %5s  %5s  %5s  [%s]", what, f2k(m/1000.0), f2k(f/1000.0), f2k(b/1000.0), sage));
    }

    private static String f2k(double v)
    {
        String s = String.format("%,15.0f", v);
        while (s.startsWith(" "))
            s = s.substring(1);
        return s;
    }
}
