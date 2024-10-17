package rtss.pre1917.calc;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.migration.TotalMigration;
import rtss.pre1917.eval.EvalGrowthRate;
import rtss.pre1917.validate.CheckProgressiveAvailable;
import rtss.util.Util;

public class EvalGrowthByTerritory
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

    private final String taxonName = "Империя";
    private final int toYear = 1913;

    private TerritoryDataSet tdsPopulation;
    private Territory tmPopulation;

    private TerritoryDataSet tdsVitalRates;
    private Territory tmVitalRates;

    private final TerritoryDataSet tdsCensus1897 = new LoadData().loadCensus1897(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
    private final TerritoryDataSet tdsCSK = new LoadData().loadEzhegodnikRossii(LoadOptions.DONT_VERIFY,
                                                                                LoadOptions.ADJUST_FEMALE_BIRTHS,
                                                                                LoadOptions.MERGE_CITIES,
                                                                                LoadOptions.MERGE_POST1897_REGIONS);
    private final TotalMigration totalMigration = TotalMigration.getTotalMigration();
    private final EvalGrowthRate evalGrowthRate = new EvalGrowthRate(tdsCensus1897);

    private final double PROMILLE = 1000.0;

    private EvalGrowthByTerritory() throws Exception
    {
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
        // ### eval_1896(tdsPopulation);

        tdsPopulation = FilterByTaxon.filterByTaxon(taxonName, tdsPopulation);

        new CheckProgressiveAvailable(tdsPopulation).check(toYear + 1);

        /* ===================== Естественное движение ===================== */

        tdsVitalRates = tdsPopulation.dup();

        /* ===================== Правки ===================== */

        // ### corrections();
    }
}
