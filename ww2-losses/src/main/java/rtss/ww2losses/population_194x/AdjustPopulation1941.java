package rtss.ww2losses.population_194x;

import rtss.data.bin.Bin;
import rtss.data.population.struct.Population;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;

public class AdjustPopulation1941 implements AdjustPopulation
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
        p = p.clone();
        
        switch (area)
        {
        case USSR:
            adjust_USSR(p);
            break;

        case RSFSR:
            adjust_RSFSR(p);
            break;
        }
        
        Bin[] male = p.binSumByAge(Gender.MALE, ADH_refined_widths);
        Bin[] female = p.binSumByAge(Gender.FEMALE, ADH_refined_widths);
        p = new Population(male, female);
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
