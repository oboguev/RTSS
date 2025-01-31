package rtss.ww2losses.population_194x;

import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.selectors.Locality;

public class AdjustPopulation
{
    public Population adjust(Population p) throws Exception
    {
        return p;
    }

    public PopulationByLocality adjust(PopulationByLocality p) throws Exception
    {
        Population urban = p.forLocality(Locality.URBAN);
        Population rural = p.forLocality(Locality.RURAL);
        Population total = p.forLocality(Locality.TOTAL);

        if (urban != null)
            urban = adjust(urban);

        if (rural != null)
            rural = adjust(rural);

        if (total != null)
            total = adjust(total);
        
        return new PopulationByLocality(total, urban, rural);
    }
}
