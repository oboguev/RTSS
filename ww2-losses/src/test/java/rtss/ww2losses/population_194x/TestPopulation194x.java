package rtss.ww2losses.population_194x;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.PopulationByLocality;
import rtss.util.Util;

public class TestPopulation194x
{
    public static void main(String[] args)
    {
        try
        {
            PopulationByLocality p;

            p = new USSR_Population_In_Early_1940().evaluate();
            
            CombinedMortalityTable mt1940 = new USSR_MortalityTable_1940().evaluate();
            
            p = new USSR_Population_In_Middle_1941().evaluate();
            
            Util.unused(p);
            Util.unused(mt1940);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
