package rtss.data.population;

import rtss.data.population.struct.PopulationByLocality;
import rtss.data.selectors.Area;
import rtss.util.Util;

public class TestLoadPopulation
{
    public static void main(String[] args)
    {
        try
        {
            PopulationByLocality p;
            p = PopulationByLocality.census(Area.USSR, 1970);
            p = PopulationByLocality.census(Area.RSFSR, 1970);
            p = PopulationByLocality.census(Area.USSR, 1979);
            p = PopulationByLocality.census(Area.RSFSR, 1979);
            Util.out("** Complete");
        }
        catch (Exception ex)
        {
            Util.err("** Exception: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            Util.noop();
        }
    }
}
