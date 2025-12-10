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
            PopulationByLocality p1989 = LoadData.populationCensus1989();
            CombinedMortalityTable cmt = LoadData.mortalityTable();
            AgeSpecificFertilityRates asfr = LoadData.loadASFR();

            for (int year = 1990; year <= 2016; year++)
            {
                PopulationByLocality p = LoadData.actualPopulation(year);
                double v = p.forLocality(Locality.TOTAL).sum();
                Util.out(String.format("%4d %,d", year, (long) v));
            }
            
            LoadData.actualPopulation(1990);

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
