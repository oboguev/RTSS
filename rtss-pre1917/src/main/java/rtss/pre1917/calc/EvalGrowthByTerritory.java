package rtss.pre1917.calc;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.data.migration.TotalMigration;
import rtss.pre1917.eval.EvalGrowthRate;
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
            
            for (int year = 1896; year <= toYear; year++)
            {
                if (year == 1898 || year == 1905 || year == 1910)
                    continue;
                
                Territory t = tdsVitalRates.get(tname);
                TerritoryYear ty = t.territoryYear(year);
                long pop_vital = ty.progressive_population.total.both;

                double cbr = (PROMILLE * ty.births.total.both) / pop_vital;
                double cdr = (PROMILLE * ty.deaths.total.both) / pop_vital;
            }
        }
    }
}
