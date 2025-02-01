package rtss.ww2losses.helpers;

import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Gender;
import rtss.util.Util;
import rtss.ww2losses.HalfYearEntries;
import rtss.ww2losses.HalfYearEntry;
import rtss.ww2losses.HalfYearEntries.HalfYearSelector;

/*
 * Проверка самосогласованности вычисленных значений
 */
public class VerifyHalfYears
{
    public void verify(HalfYearEntries<HalfYearEntry> halves) throws Exception
    {
        Util.out("Верификация:");
        
        for (HalfYearEntry he : halves)
            verify(he);
    }

    public void verify(HalfYearEntry he) throws Exception
    {
        if (he.year == 1946)
            return;
        
        PopulationContext p1 = he.actual_population;
        PopulationContext p2 = he.next.actual_population;
        
        same(he.actual_deaths.sum(), he.actual_peace_deaths.sum() + he.actual_excess_wartime_deaths.sum());  
        same(he.actual_deaths.sum(Gender.MALE), he.actual_peace_deaths.sum(Gender.MALE) + he.actual_excess_wartime_deaths.sum(Gender.MALE));  
        same(he.actual_deaths.sum(Gender.FEMALE), he.actual_peace_deaths.sum(Gender.FEMALE) + he.actual_excess_wartime_deaths.sum(Gender.FEMALE));
        
        double v1 = -1 * (p2.sum() - p1.sum());
        double v2 = -1 * (he.actual_births - he.actual_deaths.sum());
        
        out(String.format("%d.%d %8s %8s %8s = %s", 
                          he.year, he.halfyear.seq(1),
                          f2k(v1), f2k(v2), f2k(v1-v2), f2s(v1-v2)));
        
        if (he.year == 1941 && he.halfyear == HalfYearSelector.FirstHalfYear)
        {
            // ###
        }
        else
        {
            // same(p2.sum() - p1.sum(), he.actual_births - he.actual_deaths.sum());
        }
        
        // ###
    }
    
    private void same(double a, double b) throws Exception
    {
        Util.assertion(Util.same(a, b));
    }
    
    /* ====================================================================== */
    
    private void out(String what)
    {
        Util.out(what);
    }

    private void outk(String what, double v)
    {
        out(what + ": " + f2k(v));
    }

    private String f2k(double v)
    {
        return f2s(v / 1000.0);
    }
    
    private String f2s(double v)
    {
        String s = String.format("%,15.0f", v);
        while (s.startsWith(" "))
            s = s.substring(1);
        return s;
    }
}
