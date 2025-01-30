package rtss.ww2losses.helpers;

import rtss.data.population.forward.PopulationContext;
import rtss.data.selectors.Gender;
import rtss.util.Util;
import rtss.ww2losses.HalfYearEntries;
import rtss.ww2losses.HalfYearEntries.HalfYearSelector;
import rtss.ww2losses.HalfYearEntry;
import rtss.ww2losses.params.AreaParameters;

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
        
        show("1946, фактическое", p1946_actual, age + 1946 - 1941, false);
    }
    
    private static void show(String what, PopulationContext p, int age, boolean secondHalfyear) throws Exception
    {
        String sage = "возраст " + age;
        
        // ### вырезку из PopulationContext 
        
        double f = p.getYearValue(Gender.FEMALE, age); 
        double m = p.getYearValue(Gender.MALE, age); 
        double b = p.getYearValue(Gender.BOTH, age);
        
        if (secondHalfyear && Util.False)
        {
            double f2 = p.getYearValue(Gender.FEMALE, age + 1); 
            double m2 = p.getYearValue(Gender.MALE, age + 1); 
            double b2 = p.getYearValue(Gender.BOTH, age + 1);
            
            f = (f + f2) / 2;
            m = (m + m2) / 2;
            b = (b + b2) / 2;
            sage += ".5";
            
            return;
        }
        
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
