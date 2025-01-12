package rtss.ww2losses.helpers;

import rtss.data.population.PopulationByLocality;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;
import rtss.ww2losses.HalfYearEntry;
import rtss.ww2losses.params.AreaParameters;
import rtss.ww2losses.util.HalfYearEntries;
import rtss.ww2losses.util.HalfYearEntries.HalfYearSelector;

/*
 * Распечатать движение населения определённых возрастов сквозь период войны
 */
public class ShowForecast
{
    public static void show(AreaParameters ap, PopulationByLocality p1946_actual, HalfYearEntries<HalfYearEntry> halves, int age) throws Exception
    {
        Util.out("");
        Util.out(String.format("%s, возраст %d (на начало 1941)", ap.area.toString(), age));
        
        for (HalfYearEntry he : halves)
        {
            show(he.toString(), he.p_nonwar_without_births, age + he.year - 1941, he.halfyear == HalfYearSelector.SecondHalfYear);
        }
        
        show("1946, фактическое", p1946_actual, age + 1946 - 1941, false);
    }
    
    private static void show(String what, PopulationByLocality p, int age, boolean secondHalfyear) throws Exception
    {
        double f = p.get(Locality.TOTAL, Gender.FEMALE, age); 
        double m = p.get(Locality.TOTAL, Gender.MALE, age); 
        double b = p.get(Locality.TOTAL, Gender.BOTH, age);
        
        if (secondHalfyear)
        {
            double f2 = p.get(Locality.TOTAL, Gender.FEMALE, age + 1); 
            double m2 = p.get(Locality.TOTAL, Gender.MALE, age + 1); 
            double b2 = p.get(Locality.TOTAL, Gender.BOTH, age + 1);
            
            f = (f + f2) / 2;
            m = (m + m2) / 2;
            b = (b + b2) / 2;
        }
        
        Util.out(String.format("%-24s %5s  %5s  %5s", what, f2k(m/1000.0), f2k(f/1000.0), f2k(b/1000.0)));
    }

    private static String f2k(double v)
    {
        String s = String.format("%,15.0f", v);
        while (s.startsWith(" "))
            s = s.substring(1);
        return s;
    }
}
