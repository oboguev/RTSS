package rtss.losses199x;

import rtss.data.asfr.AgeSpecificFertilityRates;
import rtss.data.asfr.AgeSpecificFertilityRatesByYear;
import rtss.data.bin.Bin;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.synthetic.MortalityTableGKS;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.selectors.Area;

public class LoadData
{
    public static PopulationByLocality polulation1989() throws Exception
    {
        PopulationByLocality p1989 = PopulationByLocality.census(Area.RSFSR, 1989);
        return p1989;
    }

    public static CombinedMortalityTable mortalityTable() throws Exception
    {
        CombinedMortalityTable cmt = MortalityTableGKS.getMortalityTable(Area.RSFSR, "1986-1987");
        return cmt;
    }
    
    public static AgeSpecificFertilityRates loadASFR() throws Exception
    {
        AgeSpecificFertilityRatesByYear yearly_asfrs = AgeSpecificFertilityRatesByYear.load("age_specific_fertility_rates/RSFSR/RSFSR-ASFR.xlsx");
        
        /* 
         * Take average for 1981-1989
         */
        Bin[] bins = null;
        int nyears = 0;
        
        for (int year = 1981; year <= 1989; year++)
        {
            AgeSpecificFertilityRates a = yearly_asfrs.getForYear(year);
            if (bins == null)
            {
                nyears = 1;
                bins = a.binsReadonly();
            }
            else
            {
                nyears++;
                Bin[] xbins = a.binsReadonly();
                
                if (bins.length != xbins.length)
                    throw new Exception("Mismatching ASFR data");
                
                for (int k = 0; k < bins.length; k++)
                {
                    Bin bin = bins[k];
                    Bin xbin = xbins[k];
                    if (bin.age_x1 != xbin.age_x1 || bin.age_x2 != xbin.age_x2)
                        throw new Exception("Mismatching ASFR data");
                    bin.avg += xbin.avg;
                }
                
            }
        }
        
        for (Bin bin : bins)
            bin.avg /= nyears;
        
        AgeSpecificFertilityRates asfr = new AgeSpecificFertilityRates(bins);
        return asfr;
    }
}
