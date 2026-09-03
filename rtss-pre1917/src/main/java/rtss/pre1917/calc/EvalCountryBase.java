package rtss.pre1917.calc;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryNames;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.data.migration.Immigration;
import rtss.pre1917.data.migration.TotalMigration;
import rtss.pre1917.eval.EvalGrowthRate;
import rtss.pre1917.eval.FixEarlyPeriod;
import rtss.util.Util;

public class EvalCountryBase
{
    protected TerritoryDataSet tdsPopulation;
    protected TerritoryDataSet tdsVitalRates;

    protected final TerritoryDataSet tdsCSK = new LoadData().loadEzhegodnikRossii(LoadOptions.DONT_VERIFY,
                                                                                  LoadOptions.ADJUST_FEMALE_BIRTHS,
                                                                                  LoadOptions.MERGE_CITIES,
                                                                                  LoadOptions.MERGE_POST1897_REGIONS);
    protected final TotalMigration totalMigration = TotalMigration.getTotalMigration();
    protected final Immigration immigration = new LoadData().loadImmigration();

    protected final double PROMILLE = 1000.0;

    protected final String taxonName;
    protected final int fromYear;
    protected final int toYear;

    protected EvalCountryBase(String taxonName, int fromYear, int toYear) throws Exception
    {
        this.taxonName = taxonName;
        this.fromYear = fromYear;
        this.toYear = toYear;
    }

    public void corrections() throws Exception
    {
        CorrectTerritories ct = new CorrectTerritories(taxonName, fromYear, toYear, tdsPopulation, tdsVitalRates);
        ct.corrections();
    }
}
