package rtss.data.population.synthetic;

import rtss.data.selectors.Area;
import rtss.util.Util;

public class TestPopulationADH
{
    public static void main(String[] args)
    {
        try
        {
            PopulationADH.getPopulationByLocality(Area.RSFSR, 1939);
            PopulationADH.getPopulationByLocality(Area.RSFSR, 1940);
            PopulationADH.getPopulationByLocality(Area.RSFSR, 1941);
            PopulationADH.getPopulationByLocality(Area.RSFSR, 1946);
            PopulationADH.getPopulationByLocality(Area.RSFSR, 1947);

            PopulationADH.getPopulationByLocality(Area.USSR, "1939-границы-1938");
            PopulationADH.getPopulationByLocality(Area.USSR, "1939-границы-1946");
            PopulationADH.getPopulationByLocality(Area.USSR, 1940);
            PopulationADH.getPopulationByLocality(Area.USSR, 1941);
            PopulationADH.getPopulationByLocality(Area.USSR, 1946);
            PopulationADH.getPopulationByLocality(Area.USSR, 1947);

            Util.out("*** Completed");
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
        }
    }
}
