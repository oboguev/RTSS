package rtss.losses199x;

import rtss.data.asfr.AgeSpecificFertilityRates;
import rtss.data.asfr.AgeSpecificFertilityRatesByYear;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.synthetic.MortalityTableGKS;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.selectors.Area;
import rtss.data.selectors.Locality;
import rtss.rosbris.RosBrisPopulationMidyearForDeaths;
import rtss.rosbris.RosBrisTerritories;
import rtss.rosbris.core.RosBrisDataSet;
import rtss.util.Util;

public class Losses199X
{
    public static void main(String[] args)
    {
        try
        {
            PopulationByLocality p1989 = LoadData.polulation1989();
            CombinedMortalityTable cmt = LoadData.mortalityTable();
            AgeSpecificFertilityRates asfr = LoadData.loadASFR();

            for (int year = 1989; year <= 2022; year++)
            {
                RosBrisPopulationMidyearForDeaths.getPopulationByLocality(RosBrisTerritories.RF_BEFORE_2014, year);
            }

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
