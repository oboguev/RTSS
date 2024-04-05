package rtss.ww2losses.population_194x;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.PopulationByLocality;

/**
 * Вычислить возрастную структуру и численность гипотетического населения СССР на начало 1946 года
 * в том случае, если бы рождаемость и смертность в 1941-1945 гг. оставались такими же, как в 1940 году 
 */
public class USSR_Expected_Population_In_Early_1946
{
    /*
     * AДХ, "Население Советского Союза", стр. 120
     */
    public double CBR_1939 = 40.0;
    public double CBR_1940 = 36.1;
    
    private static final int MAX_AGE = PopulationByLocality.MAX_AGE;
    private CombinedMortalityTable mt1938 = new CombinedMortalityTable("mortality_tables/USSR/1938-1939");
    
    public USSR_Expected_Population_In_Early_1946() throws Exception
    {
        
    }

}
