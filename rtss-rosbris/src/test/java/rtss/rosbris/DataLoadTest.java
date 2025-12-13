package rtss.rosbris;

import rtss.data.population.struct.PopulationByLocality;
import rtss.util.Util;

public class DataLoadTest
{
    public static void main(String[] args)
    {
        try
        {
            new DataLoadTest().test_1();
            Util.out("** Completed");
        }
        catch (Exception ex)
        {
            Util.err("** Exception: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }

    }

    private void test_1() throws Exception
    {
        PopulationByLocality p = null;
        RosBrisDeathRates dr = null;
        RosBrisFertilityRates br = null;

        Util.unused(p, dr, br);

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
        RosBrisDeathRates.use2021census(false);
        for (int year = 2012; year <= 2022; year++)
            dr = RosBrisDeathRates.loadMX(RosBrisTerritories.RF_BEFORE_2014, year);

        RosBrisDeathRates.use2021census(true);
        for (int year = 2012; year <= 2022; year++)
            dr = RosBrisDeathRates.loadMX(RosBrisTerritories.RF_BEFORE_2014, year);

        /* birth rates (mx) */
        RosBrisFertilityRates.use2021census(false);
        for (int year = 2012; year <= 2022; year++)
            br = RosBrisFertilityRates.loadFertilityRates(RosBrisTerritories.RF_BEFORE_2014, year);

        RosBrisDeathRates.use2021census(true);
        for (int year = 2012; year <= 2023; year++)
            br = RosBrisFertilityRates.loadFertilityRates(RosBrisTerritories.RF_BEFORE_2014, year);
    }
}
