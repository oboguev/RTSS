package rtss.pre1917.calc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.validate.CheckProgressiveAvailable;
import rtss.util.Util;

public class EvalGrowthByTerritory extends EvalCountryBase
{
    public static void main(String[] args)
    {
        try
        {
            new EvalGrowthByTerritory().eval();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private EvalGrowthByTerritory() throws Exception
    {
        super("Империя", 1913);
    }
    
    private static class TerritoryResult implements Comparable<TerritoryResult >
    {
        public final String tname;
        public final double ngr;

        public TerritoryResult(String tname, double ngr)
        {
            this.tname = tname;
            this.ngr = ngr;
        }

        @Override
        public int compareTo(TerritoryResult o)
        {
            return (int) Math.signum(o.ngr - this.ngr);
        }
    }
    
    private List<TerritoryResult> results = new ArrayList<>();

    private void eval() throws Exception
    {
        /* ===================== Численность населения ===================== */

        tdsPopulation = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY,
                                                LoadOptions.ADJUST_FEMALE_BIRTHS,
                                                LoadOptions.FILL_MISSING_BD,
                                                LoadOptions.MERGE_CITIES,
                                                LoadOptions.MERGE_POST1897_REGIONS,
                                                LoadOptions.EVAL_PROGRESSIVE);
        tdsPopulation.leaveOnlyTotalBoth();
        eval_1896(tdsPopulation);

        tdsPopulation = FilterByTaxon.filterByTaxon(taxonName, tdsPopulation);

        new CheckProgressiveAvailable(tdsPopulation).check(toYear + 1);

        /* ===================== Естественное движение ===================== */

        tdsVitalRates = tdsPopulation.dup();

        /* ===================== Правки ===================== */

        corrections();

        /* ===================== Результаты ===================== */
        
        for (String tname : tdsVitalRates.keySet())
        {
            if (Taxon.isComposite(tname))
                continue;
            
            double ngr_average = 0;
            int nyears = 0;
            
            for (int year = 1896; year <= toYear; year++)
            {
                if (year == 1898 || year == 1905 || year == 1910)
                    continue;
                
                Territory t = tdsVitalRates.get(tname);
                TerritoryYear ty = t.territoryYear(year);
                
                long pop_vital = ty.progressive_population.total.both;
                double cbr = (PROMILLE * denull(ty.births.total.both)) / pop_vital;
                double cdr = (PROMILLE * denull(ty.deaths.total.both)) / pop_vital;
                double ngr = cbr - cdr;
                ngr_average += ngr;
                nyears++;
            }
            
            results.add(new TerritoryResult(tname, ngr_average / nyears));
        }
        
        Collections.sort(results);
        
        for (TerritoryResult r : results)
        {
            Util.out(String.format("%s %.1f", r.tname, r.ngr));
        }
    }
    
    private long denull(Long v)
    {
        return v != null ? v : 0;
    }
}
