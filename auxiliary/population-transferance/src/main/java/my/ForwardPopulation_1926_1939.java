package my;

import data.population.PopulationByLocality;

public class ForwardPopulation_1926_1939
{
    public final boolean DoSmoothPopulation = true;

    private PopulationByLocality p1926 = PopulationByLocality.load("population_data/USSR/1926").smooth(DoSmoothPopulation);
    private PopulationByLocality p1939 = PopulationByLocality.load("population_data/USSR/1939").smooth(DoSmoothPopulation);;

    public ForwardPopulation_1926_1939() throws Exception
    {
    }
    
    public void forward() throws Exception
    {
    }
}
