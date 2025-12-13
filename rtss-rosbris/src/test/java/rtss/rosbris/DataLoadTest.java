package rtss.rosbris;

import rtss.data.population.struct.PopulationByLocality;
import rtss.util.Util;

public class DataLoadTest
{
    public static void main(String[] args)
    {
        try
        {
            PopulationByLocality p;
            RosBrisDeathRate dr;
            
            /* population for deaths (exposure) */
            RosBrisPopulationExposureForDeaths.use2021census(true);
            for (int year = 2012; year <= 2022; year++)
                p = RosBrisPopulationExposureForDeaths.getPopulationByLocality(RosBrisTerritories.RF_BEFORE_2014, year);
            
            RosBrisPopulationExposureForDeaths.use2021census(false);
            for (int year = 2012; year <= 2022; year++)
                p = RosBrisPopulationExposureForDeaths.getPopulationByLocality(RosBrisTerritories.RF_BEFORE_2014, year);

            /* population for births */
            RosBrisFemalePopulationAverageForBirths.use2021census(true);
            for (int year = 2012; year <= 2022; year++)
                p = RosBrisFemalePopulationAverageForBirths.getPopulationByLocality(RosBrisTerritories.RF_BEFORE_2014, year);
            
            RosBrisFemalePopulationAverageForBirths.use2021census(false);
            for (int year = 2012; year <= 2022; year++)
                p = RosBrisFemalePopulationAverageForBirths.getPopulationByLocality(RosBrisTerritories.RF_BEFORE_2014, year);

            /* death rates (mx) */
            RosBrisDeathRate.use2021census(false);
            for (int year = 2012; year <= 2022; year++)
                dr = RosBrisDeathRate.loadMX(RosBrisTerritories.RF_BEFORE_2014, year);

            RosBrisDeathRate.use2021census(true);
            for (int year = 2012; year <= 2022; year++)
                dr = RosBrisDeathRate.loadMX(RosBrisTerritories.RF_BEFORE_2014, year);

            // ### test load data DRa and BRa
            Util.noop();
            Util.out("** Completed");
        }
        catch (Exception ex)
        {
            Util.err("** Exception: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    }

}
