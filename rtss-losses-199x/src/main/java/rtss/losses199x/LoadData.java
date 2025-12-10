package rtss.losses199x;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import rtss.data.asfr.AgeSpecificFertilityRates;
import rtss.data.asfr.AgeSpecificFertilityRatesByYear;
import rtss.data.bin.Bin;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.synthetic.MortalityTableGKS;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.selectors.Area;
import rtss.data.selectors.Locality;
import rtss.rosbris.RosBrisPopulationMidyearForDeaths;
import rtss.rosbris.RosBrisTerritories;
import rtss.util.Util;
import rtss.util.excel.Excel;
import rtss.util.excel.ExcelRC;

public class LoadData
{
    public static PopulationByLocality populationCensus1989() throws Exception
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
    
    private static Map<Integer,PopulationByLocality> actualMidyearPopulation; 

    public static PopulationByLocality actualPopulation(int year) throws Exception
    {
        if (actualMidyearPopulation == null)
        {
            Map<Integer,PopulationByLocality> m = new HashMap<>();
            
            for (int yy = 1989; yy <= 2022; yy++)
            {
                PopulationByLocality p = RosBrisPopulationMidyearForDeaths.getPopulationByLocality(RosBrisTerritories.RF_BEFORE_2014, yy);
                m.put(yy, p);
                // double v = p.forLocality(Locality.TOTAL).sum();
                // Util.out(String.format("%4d %,d", yy, (long) v));
            }
            
            actualMidyearPopulation = m;
        }
        
        PopulationByLocality p1 = actualMidyearPopulation.get(year - 1);
        PopulationByLocality p2 = actualMidyearPopulation.get(year);
        PopulationByLocality p = p1.avg(p2);
        return p;
    }
    
    public static Map<Integer,Double> actualBirths(int y1, int y2) throws Exception
    {
        return actualTableValues(y1, y2, "рождения total");
    }
    
    public static Map<Integer,Double> actualDeaths(int y1, int y2) throws Exception
    {
        return actualTableValues(y1, y2, "смерти total");
    }

    public static Map<Integer,Double> actualTableValues(int y1, int y2, String columnTitle) throws Exception
    {
        Map<Integer,Double> m = new LinkedHashMap<>();
        ExcelRC rc = Excel.readSheet("199x-population-births-deaths.xlsx", true, "Data с Крымом");
        
        List<Object> yearColumnValues = rc.columnValues("год");
        for (int nr = 1; nr < yearColumnValues.size(); nr++)
        {
            Object o = yearColumnValues.get(nr);
            if (ExcelRC .isBlank(o))
                continue;
            int year = ExcelRC.asInt(o);
            if (year >= y1 && year <= y2)
            {
                o = rc.rowColumnValue(nr, columnTitle);
                Double value = ExcelRC.asDouble(o);
                m.put(year, value);
            }
        }

        return m;
    }
}
