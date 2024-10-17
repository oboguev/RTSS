package rtss.pre1917.calc;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.data.migration.TotalMigration;
import rtss.pre1917.eval.EvalGrowthRate;

public class EvalCountryBase
{
    protected TerritoryDataSet tdsPopulation;
    protected Territory tmPopulation;

    protected TerritoryDataSet tdsVitalRates;
    protected Territory tmVitalRates;

    protected final TerritoryDataSet tdsCensus1897 = new LoadData().loadCensus1897(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
    protected final TerritoryDataSet tdsCSK = new LoadData().loadEzhegodnikRossii(LoadOptions.DONT_VERIFY,
                                                                                LoadOptions.ADJUST_FEMALE_BIRTHS,
                                                                                LoadOptions.MERGE_CITIES,
                                                                                LoadOptions.MERGE_POST1897_REGIONS);
    protected final TotalMigration totalMigration = TotalMigration.getTotalMigration();
    protected final EvalGrowthRate evalGrowthRate = new EvalGrowthRate(tdsCensus1897);

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
        boolean match = false;

        if (taxonName.equals("русские губернии Европейской России и Кавказа, кроме Черноморской"))
            return;

        Long nAddChernomorskaya = null;

        if (taxonName.equals("Империя") || taxonName.equals("СССР-1991") || taxonName.equals("РСФСР-1991"))
        {
            /* пересчёт численности населения для Дагестана */
            new AdjustTerritories(tdsPopulation).fixDagestan();

            /* не включать Дагестан в подсчёт рождаемости и смертности */
            excludeFromVitalRates("Дагестанская обл.");

            nAddChernomorskaya = (long) 1_300 + 1_600;

            match = true;
        }

        if (taxonName.equals("Империя") || taxonName.equals("СССР-1991"))
        {
            useStabilized("Закаспийская обл.", 1911, 1913);
            useStabilized("Семиреченская обл.", 1912, 1914);
            useStabilized("Сыр-Дарьинская обл.", 1908);
            useStabilized("Ферганская обл.", 1912);
            useStabilized("Карсская обл.", 1907, 1913);
            useStabilized("Тифлисская", 1903, 1914);

            new AdjustTerritories(tdsPopulation).setCSK(tdsCSK).fixSamarkand();
            new AdjustTerritories(tdsPopulation).setCSK(tdsCSK).fixUralskaia();
            new AdjustTerritories(tdsPopulation).setCSK(tdsCSK).fixBakinskaiaWithBaku();

            nAddChernomorskaya = (long) 1_300;

            excludeFromVitalRates("Елисаветпольская");
            excludeFromVitalRates("Самаркандская обл.");
            excludeFromVitalRates("Семипалатинская обл.");
            excludeFromVitalRates("Уральская обл.");
            excludeFromVitalRates("Бакинская с Баку");
            excludeFromVitalRates("Кутаисская с Батумской");

            match = true;
        }

        if (nAddChernomorskaya != null)
        {
            for (int year = 1896; year <= toYear; year++)
            {
                tdsPopulation.get("Черноморская").cascadeAdjustProgressivePopulation(year, nAddChernomorskaya);
                tdsVitalRates.get("Черноморская").cascadeAdjustProgressivePopulation(year, nAddChernomorskaya);
            }
        }

        if (!match)
            throw new Exception("Неверный таксон " + taxonName);
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

    private void excludeFromVitalRates(String tname)
    {
        // проверка против опечаток: отсутствующее имя вызовет NullPointerException
        Territory t = tdsVitalRates.get(tname);
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
        Territory t = tdsPopulation.get(tname);
        Territory tEval = evalGrowthRate.evalTerritory(t);

        for (int year : tEval.years())
        {
            TerritoryYear ty = tEval.territoryYearOrNull(year);
            ty.progressive_population = ty.population.dup(ty);
        }

        tdsPopulation.put(tname, tEval);
        tdsVitalRates.put(tname, tEval.dup());
    }

    protected void extraDeaths(int year, long deaths) throws Exception
    {
        tmPopulation.extraDeaths(year, deaths);
        tmVitalRates.extraDeaths(year, deaths);
    }
}
