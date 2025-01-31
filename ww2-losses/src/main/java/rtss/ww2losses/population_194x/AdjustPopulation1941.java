package rtss.ww2losses.population_194x;

import rtss.data.bin.Bin;
import rtss.data.population.struct.Population;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.util.Util;

public class AdjustPopulation1941 extends AdjustPopulation
{
    private static final int[] ADH_refined_widths = { 1, 1, 1, 1, 1, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 15 };
    private final Area area;
    
    public AdjustPopulation1941(Area area)
    {
        this.area = area;
    }
    
    @Override
    public Population adjust(Population p) throws Exception
    {
        Population p0 = p;
                
        switch (area)
        {
        case USSR:
            p = p.clone();
            adjust_USSR(p);
            break;

        case RSFSR:
            p = p.clone();
            adjust_RSFSR(p);
            break;
        }
        
        Bin[] male = p.binSumByAge(Gender.MALE, ADH_refined_widths);
        Bin[] female = p.binSumByAge(Gender.FEMALE, ADH_refined_widths);
        p = new Population(male, female);
        
        Util.assertion(Util.same(p.sum(Gender.MALE), p0.sum(Gender.MALE)));
        Util.assertion(Util.same(p.sum(Gender.FEMALE), p0.sum(Gender.FEMALE)));
        
        return p;
    }
    
    private void adjust_USSR(Population p)
    {
        // ###
    }

    private void adjust_RSFSR(Population p)
    {
        // ###
    }
}
