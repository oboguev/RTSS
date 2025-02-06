package rtss.ww2losses.population_194x;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.selectors.Area;
import rtss.util.Util;
import rtss.ww2losses.params.AreaParameters;
import rtss.ww2losses.population1941.Population_In_Middle_1941;
import rtss.ww2losses.population194x.MortalityTable_1940;
import rtss.ww2losses.population194x.Population_In_Early_1940;

public class TestPopulation194x
{
    public static void main(String[] args)
    {
        try
        {
            PopulationByLocality p;
            
            AreaParameters ap = AreaParameters.forArea(Area.USSR);

            p = new Population_In_Early_1940(ap).evaluate();
            
            CombinedMortalityTable mt1940 = new MortalityTable_1940(ap).evaluate();
            
            p = new Population_In_Middle_1941(ap).evaluate();
            
            new MortalityTable_1940(ap).show_survival_rates_1941_1946(); 
            
            Util.unused(p);
            Util.unused(mt1940);
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
        }
    }
}
