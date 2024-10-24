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

public class EvalCountryBase
{
    protected TerritoryDataSet tdsPopulation;
    protected TerritoryDataSet tdsVitalRates;

    protected final TerritoryDataSet tdsCensus1897 = new LoadData().loadCensus1897(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
    protected final TerritoryDataSet tdsCSK = new LoadData().loadEzhegodnikRossii(LoadOptions.DONT_VERIFY,
                                                                                  LoadOptions.ADJUST_FEMALE_BIRTHS,
                                                                                  LoadOptions.MERGE_CITIES,
                                                                                  LoadOptions.MERGE_POST1897_REGIONS);
    protected final TotalMigration totalMigration = TotalMigration.getTotalMigration();
    protected final EvalGrowthRate evalGrowthRate = new EvalGrowthRate(tdsCensus1897);
    protected final Immigration immigration = new LoadData().loadImmigration();

    protected final double PROMILLE = 1000.0;

    protected final String taxonName;
    protected final int toYear;

    protected EvalCountryBase(String taxonName, int toYear) throws Exception
    {
        this.taxonName = taxonName;
        this.toYear = toYear;
    }

    /* ================================================================================================ */

    protected void corrections() throws Exception
    {
        if (taxonName.equals("русские губернии Европейской России и Кавказа, кроме Черноморской"))
            return;

        corrections_Kavkaz();
        corrections_CentralAsia();

        /*
         * Черноморская губерния: внутрироссийская миграция извне РСФСР (1,600) и иммиграция извне России (1,300).
         */
        Long nAddChernomorskaya = null;
        final long nAddChernomorskayaInner = 1_600;
        final long nAddChernomorskayaForeign = 1_300;
        
        switch (taxonName)
        {
        case "Империя":
        case "СССР-1991":
            // уже содержится в учёте турецкой иммиграции 
            // nAddChernomorskaya = nAddChernomorskayaForeign;
            break;
            
        case "РСФСР-1991":
            nAddChernomorskaya = nAddChernomorskayaForeign + nAddChernomorskayaInner;
            break;
            
        default:    
            break;
        }

        if (nAddChernomorskaya != null)
        {
            for (int year = 1896; year <= toYear; year++)
            {
                tdsPopulation.get("Черноморская").cascadeAdjustProgressivePopulation(year, nAddChernomorskaya);
                tdsVitalRates.get("Черноморская").cascadeAdjustProgressivePopulation(year, nAddChernomorskaya);
            }
        }
    }

    private void corrections_Kavkaz() throws Exception
    {
        /* пересчёт численности населения для Дагестана */
        new AdjustTerritories(tdsPopulation).fixDagestan();

        /* не включать Дагестан в подсчёт рождаемости и смертности */
        excludeFromVitalRates("Дагестанская обл.");

        useStabilized("Тифлисская", 1903, 1914);

        if (taxonName.equals("Империя"))
            useStabilized("Карсская обл.", 1907, 1913);

        new AdjustTerritories(tdsPopulation).setCSK(tdsCSK).fixBakinskaiaWithBaku();

        excludeFromVitalRates("Елисаветпольская");
        excludeFromVitalRates("Бакинская с Баку");
        excludeFromVitalRates("Кутаисская с Батумской");
    }

    private void corrections_CentralAsia() throws Exception
    {
        useStabilized("Закаспийская обл.", 1911, 1913);
        useStabilized("Семиреченская обл.", 1912, 1914);
        useStabilized("Сыр-Дарьинская обл.", 1908);
        useStabilized("Ферганская обл.", 1912);

        new AdjustTerritories(tdsPopulation).setCSK(tdsCSK).fixSamarkand();
        new AdjustTerritories(tdsPopulation).setCSK(tdsCSK).fixUralskaia();

        excludeFromVitalRates("Самаркандская обл.");
        excludeFromVitalRates("Семипалатинская обл.");
        excludeFromVitalRates("Уральская обл.");
    }

    /* ================================================================================================ */

    /*
     * Вычислить численность населения губерний и областей на начало 1896 года (прогрессивная оценка)
     */
    protected void eval_1896(TerritoryDataSet tds) throws Exception
    {
        for (String tname : tds.keySet())
        {
            if (Taxon.isComposite(tname))
                continue;

            Territory t = tds.get(tname);
            TerritoryYear ty1896 = t.territoryYearOrNull(1896);
            TerritoryYear ty1897 = t.territoryYearOrNull(1897);

            if (ty1897 != null && ty1896 != null)
            {
                long in = ty1896.births.total.both - ty1896.deaths.total.both;
                in += totalMigration.saldo(tname, 1896);
                ty1896.progressive_population.total.both = ty1897.progressive_population.total.both - in;
            }
        }
    }

    private void excludeFromVitalRates(String tname) throws Exception
    {
        TerritoryNames.checkValidTerritoryName(tname);
        Territory t = tdsVitalRates.get(tname);
        if (t != null)
            tdsVitalRates.remove(t.name);
    }

    private void useStabilized(String tname, int year) throws Exception
    {
        useStabilized(tname, year, year);
    }

    /*
     * Пересчитать территорию по стабилизированному участку
     */
    private void useStabilized(String tname, int y1, int y2) throws Exception
    {
        TerritoryNames.checkValidTerritoryName(tname);

        Territory t = tdsPopulation.get(tname);
        if (t == null)
            return;

        Territory tEval = evalGrowthRate.evalTerritory(t);

        for (int year : tEval.years())
        {
            TerritoryYear ty = tEval.territoryYearOrNull(year);
            ty.progressive_population = ty.population.dup(ty);
        }

        tdsPopulation.put(tname, tEval);
        tdsVitalRates.put(tname, tEval.dup());
    }
}
