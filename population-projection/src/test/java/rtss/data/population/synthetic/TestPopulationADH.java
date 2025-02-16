package rtss.data.population.synthetic;

import rtss.data.selectors.Area;
import rtss.util.Util;

public class TestPopulationADH
{
    public static void main(String[] args)
    {
        try
        {
            if (Util.False)
            {
                PopulationADH.getPopulationByLocality(Area.USSR, 1926);
                PopulationADH.getPopulationByLocality(Area.USSR, 1927);
                PopulationADH.getPopulationByLocality(Area.USSR, 1937);
                PopulationADH.getPopulationByLocality(Area.USSR, 1938);
                PopulationADH.getPopulationByLocality(Area.USSR, "1939-границы-1938");
                PopulationADH.getPopulationByLocality(Area.USSR, "1939-границы-1946");
                PopulationADH.getPopulationByLocality(Area.USSR, 1940);
                PopulationADH.getPopulationByLocality(Area.USSR, 1941);
                PopulationADH.getPopulationByLocality(Area.USSR, 1946);
                PopulationADH.getPopulationByLocality(Area.USSR, 1947);
            }

            if (Util.True)
            {
                for (int year = 1927; year <= 1959; year++)
                {
                    if (year >= 1942 && year <= 1945)
                        continue;
                    PopulationADH.getPopulationByLocality(Area.RSFSR, year);
                }
            }

            Util.out("*** Completed");
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
        }
    }
}
