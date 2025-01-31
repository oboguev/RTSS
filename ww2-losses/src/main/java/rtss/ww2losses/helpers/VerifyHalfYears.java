package rtss.ww2losses.helpers;

import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Gender;
import rtss.util.Util;
import rtss.ww2losses.HalfYearEntries;
import rtss.ww2losses.HalfYearEntry;

/*
 * Проверка самосогласованности вычисленных значений
 */
public class VerifyHalfYears
{
    public void verify(HalfYearEntries<HalfYearEntry> halves) throws Exception
    {
        for (HalfYearEntry he : halves)
            verify(he);
    }

    public void verify(HalfYearEntry he) throws Exception
    {
        PopulationContext p1 = he.actual_population;
        PopulationContext p2 = he.next.actual_population;
        
        same(he.actual_deaths.sum(), he.actual_peace_deaths.sum() + he.actual_excess_wartime_deaths.sum());  
        same(he.actual_deaths.sum(Gender.MALE), he.actual_peace_deaths.sum(Gender.MALE) + he.actual_excess_wartime_deaths.sum(Gender.MALE));  
        same(he.actual_deaths.sum(Gender.FEMALE), he.actual_peace_deaths.sum(Gender.FEMALE) + he.actual_excess_wartime_deaths.sum(Gender.FEMALE));
        
        same(p2.sum() - p1.sum(), he.actual_births - he.actual_peace_deaths.sum());
        // ###
    }
    
    private void same(double a, double b) throws Exception
    {
        Util.assertion(Util.same(a, b));
    }
}
