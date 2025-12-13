package rtss.losses199x;

import java.util.Map;

import rtss.data.asfr.AgeSpecificFertilityRates;
import rtss.data.asfr.AgeSpecificFertilityRatesByYear;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.synthetic.MortalityTableGKS;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.selectors.Area;
import rtss.data.selectors.Locality;
import rtss.rosbris.RosBrisPopulationExposureForDeaths;
import rtss.rosbris.RosBrisTerritories;
import rtss.rosbris.core.RosBrisDataSet;
import rtss.util.Util;

public class Losses199X
{
    public static void main(String[] args)
    {
        try
        {
            new BirthsDeficit().eval();
            
            
            PopulationByLocality p1989 = LoadData.populationCensus1989();
            CombinedMortalityTable cmt = LoadData.mortalityTable();
            AgeSpecificFertilityRates asfr = LoadData.loadASFR(Locality.TOTAL);
            
            Map<Integer,Double> mb = LoadData.actualBirths(1989, 2015);
            Map<Integer,Double> md = LoadData.actualDeaths(1989, 2015);
            
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
