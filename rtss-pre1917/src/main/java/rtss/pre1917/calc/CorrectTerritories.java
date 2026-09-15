package rtss.pre1917.calc;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryNames;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.eval.EvalGrowthRate;
import rtss.pre1917.eval.EvalProgressive;
import rtss.pre1917.eval.EvalStabilizedFergana;
import rtss.pre1917.eval.EvalStabilizedSemirechye;
import rtss.pre1917.eval.EvalStabilizedV2;
import rtss.pre1917.eval.FixEarlyPeriod;

public class CorrectTerritories
{
    private final String taxonName;
    private final TerritoryDataSet tdsPopulation;
    private final TerritoryDataSet tdsVitalRates;
    private final int fromYear;
    private final int toYear;

    private final TerritoryDataSet tdsCensus1897 = new LoadData().loadCensus1897(LoadOptions.DONT_VERIFY,
                                                                                 LoadOptions.MERGE_CITIES,
                                                                                 LoadOptions.MERGE_POST1897_REGIONS);

    private final TerritoryDataSet tdsCSK = new LoadData().loadEzhegodnikRossii(LoadOptions.DONT_VERIFY,
                                                                                LoadOptions.ADJUST_FEMALE_BIRTHS,
                                                                                LoadOptions.MERGE_CITIES,
                                                                                LoadOptions.MERGE_POST1897_REGIONS);

    private final EvalGrowthRate evalGrowthRate = new EvalGrowthRate(tdsCensus1897);

    public CorrectTerritories(String taxonName, int fromYear, int toYear,
            TerritoryDataSet tdsPopulation,
            TerritoryDataSet tdsVitalRates)
            throws Exception
    {
        this.taxonName = taxonName;
        this.fromYear = fromYear;
        this.toYear = toYear;
        this.tdsPopulation = tdsPopulation;
        this.tdsVitalRates = tdsVitalRates;
    }

    /* ================================================================================================ */

    public boolean isCorrected(String tname)
    {
        switch (tname)
        {
        // Польша
        case "Сувалкская":
        case "Люблинская с Седлецкой и Холмской":

            // Кавказ
        case "Дагестанская обл.":
        case "Карсская обл.":
        case "Терская обл.":
        case "Тифлисская":
        case "Бакинская с Баку":

            // Средняя Азия
        case "Закаспийская обл.":
        case "Семиреченская обл.":
        case "Сыр-Дарьинская обл.":
        case "Ферганская обл.":
        case "Самаркандская обл.":
        case "Уральская обл.":

            // Сибирь
        case "Забайкальская обл.":
        case "Приморская обл. с Камчатской обл.":
            return true;

        default:
            return false;
        }
    }

    /* ================================================================================================ */

    public void corrections() throws Exception
    {
        if (taxonName.equals("русские губернии Европейской России и Кавказа, кроме Черноморской"))
            return;

        /* не включать кочевников астраханских степей в подсчёт естественого движения */
        excludeFromVitalRates(Taxon.Астраханская_кочевники);

        corrections_Poland();
        corrections_Kavkaz();
        corrections_CentralAsia();
        corrections_Siberia();

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
        case "СССР-1926":
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

    private void corrections_Poland() throws Exception
    {
        if (isCorrected("Сувалкская"))
            new AdjustTerritories(tdsPopulation).setCSK(tdsCSK).fixSuvalkskaia();

        if (isCorrected("Люблинская с Седлецкой и Холмской"))
        {
            final String LubSedHolm = "Люблинская с Седлецкой и Холмской";
            Territory t = tdsPopulation.get(LubSedHolm);
            TerritoryYear ty1910 = t.territoryYearOrNull(1910);
            for (int year = 1911; year <= 1913; year++)
            {
                TerritoryYear ty = t.territoryYearOrNull(year);
                ty.births.total.both = ty1910.births.total.both;
                ty.deaths.total.both = ty1910.deaths.total.both;
            }
            new EvalProgressive(tdsPopulation).evalProgressive(LubSedHolm);
        }
    }

    private void corrections_Kavkaz() throws Exception
    {
        /* пересчёт численности населения для Дагестана */
        if (isCorrected("Дагестанская обл."))
            new AdjustTerritories(tdsPopulation).fixDagestan();

        /* не включать Дагестан в подсчёт рождаемости и смертности */
        excludeFromVitalRates("Дагестанская обл.");

        if (isCorrected("Карсская обл."))
            useStabilizedV2("Карсская обл.", 1907, 1913, false);

        if (isCorrected("Терская обл."))
            useStabilizedV2("Терская обл.", 1910, 1914, true);

        if (isCorrected("Тифлисская"))
        {
            // useStabilized("Тифлисская", 1903, 1914);
            useStabilizedV2("Тифлисская", 1905, 1914, 1909, 1914, true);
        }

        if (isCorrected("Бакинская с Баку"))
            new AdjustTerritories(tdsPopulation).setCSK(tdsCSK).fixBakinskaiaWithBaku();

        excludeFromVitalRates("Елисаветпольская");
        excludeFromVitalRates("Бакинская с Баку");
        excludeFromVitalRates("Кутаисская с Батумской");
    }

    private void corrections_CentralAsia() throws Exception
    {
        if (isCorrected("Закаспийская обл."))
            useStabilized("Закаспийская обл.", 1911, 1913);

        if (isCorrected("Семиреченская обл."))
        {
            // useStabilized("Семиреченская обл.", 1912, 1914);
            fixSemirechye("Семиреченская обл.");
        }

        if (isCorrected("Сыр-Дарьинская обл."))
            useStabilized("Сыр-Дарьинская обл.", 1908);

        if (isCorrected("Ферганская обл."))
        {
            // useStabilized("Ферганская обл.", 1912);
            fixFergana("Ферганская обл.");
        }

        if (isCorrected("Самаркандская обл."))
            new AdjustTerritories(tdsPopulation).setCSK(tdsCSK).fixSamarkand();

        if (isCorrected("Уральская обл."))
            new AdjustTerritories(tdsPopulation).setCSK(tdsCSK).fixUralskaia();

        excludeFromVitalRates("Акмолинская обл.");
        excludeFromVitalRates("Тургайская обл.");
        excludeFromVitalRates("Самаркандская обл.");
        excludeFromVitalRates("Семипалатинская обл.");
        excludeFromVitalRates("Уральская обл.");
    }

    private void corrections_Siberia() throws Exception
    {
        if (isCorrected("Забайкальская обл."))
            useStabilizedV2("Забайкальская обл.", 1908, 1913, true);

        if (isCorrected("Приморская обл. с Камчатской обл."))
            fixEarlyPeriod("Приморская обл. с Камчатской обл.", 1881, 1898, 1899, 1903);

        excludeFromVitalRates("Приморская обл. с Камчатской обл.");
    }

    /* ================================================================================================ */

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

        Territory tEval = evalGrowthRate.evalTerritory(t, y1, y2);
        tdsPopulation.put(tname, tEval);
        tdsVitalRates.put(tname, tEval.dup());
    }

    /*
     * Пересчитать территорию по стабилизированному участку (метод версии V2)
     */
    @SuppressWarnings("unused")
    private void useStabilizedV2(String tname, int y1, int y2, boolean adjust1892) throws Exception
    {
        useStabilizedV2(tname, y1, y2, adjust1892, null);
    }

    @SuppressWarnings("unused")
    private void useStabilizedV2(String tname, int y1, int y2, boolean adjust1892, Double lambda) throws Exception
    {
        TerritoryNames.checkValidTerritoryName(tname);

        Territory t = tdsPopulation.get(tname);
        if (t == null)
            return;

        TerritoryYear ty1913 = t.territoryYearOrNull(1913);
        TerritoryYear ty1914 = t.territoryYearOrNull(1914);
        if (ty1914.births.total.both == null)
            ty1914.births.total.both = ty1913.births.total.both;
        if (ty1914.deaths.total.both == null)
            ty1914.deaths.total.both = ty1913.deaths.total.both;

        final int window = 7;
        if (lambda == null)
            lambda = 0.5;
        EvalStabilizedV2 es = new EvalStabilizedV2(tdsCensus1897, window, lambda, adjust1892);

        Territory tEval = es.evalTerritory(t, y1, y2, y1, y2);

        tdsPopulation.put(tname, tEval);
        tdsVitalRates.put(tname, tEval.dup());
    }

    /*
     * Пересчитать территорию по стабилизированному участку (метод версии V2)
     */
    @SuppressWarnings("unused")
    private void useStabilizedV2(String tname, int by1, int by2, int dy1, int dy2, boolean adjust1892) throws Exception
    {
        TerritoryNames.checkValidTerritoryName(tname);

        Territory t = tdsPopulation.get(tname);
        if (t == null)
            return;

        final int window = 7;
        final double lambda = 0.5;
        EvalStabilizedV2 es = new EvalStabilizedV2(tdsCensus1897, window, lambda, adjust1892);

        Territory tEval = es.evalTerritory(t, by1, by2, dy1, dy2);

        tdsPopulation.put(tname, tEval);
        tdsVitalRates.put(tname, tEval.dup());
    }

    private void fixEarlyPeriod(String tname, int yl1, int yl2, int yr1, int yr2) throws Exception
    {
        TerritoryNames.checkValidTerritoryName(tname);
        Territory t = tdsPopulation.get(tname);
        if (t == null)
            return;

        Territory tCensus = tdsCensus1897.get(tname);

        Territory xt = new FixEarlyPeriod(fromYear).fix(t, tCensus, yl1, yl2, yr1, yr2);
        tdsPopulation.put(tname, xt);
        tdsVitalRates.put(tname, xt.dup());
    }

    @SuppressWarnings("unused")
    private void fixFergana(String tname) throws Exception
    {
        TerritoryNames.checkValidTerritoryName(tname);

        Territory t = tdsPopulation.get(tname);
        if (t == null)
            return;

        final int window = 7;
        final double lambda = 0.5;

        Territory tEval = new EvalStabilizedFergana(tdsCensus1897, window, lambda).evalTerritory(t);
        tdsPopulation.put(tname, tEval);
        tdsVitalRates.put(tname, tEval.dup());
    }

    @SuppressWarnings("unused")
    private void fixSemirechye(String tname) throws Exception
    {
        TerritoryNames.checkValidTerritoryName(tname);

        Territory t = tdsPopulation.get(tname);
        if (t == null)
            return;

        final int window = 7;
        final double lambda = 0.5;

        Territory tEval = new EvalStabilizedSemirechye(tdsCensus1897, window, lambda).evalTerritory(t);
        tdsPopulation.put(tname, tEval);
        tdsVitalRates.put(tname, tEval.dup());
    }

    /* ================================================================================================ */

    public void finalizeEmpireExport(TerritoryDataSet tds) throws Exception
    {
        if (!taxonName.equals("Империя"))
            throw new IllegalArgumentException();

        /*
         * Черноморская губерния: внутрироссийская миграция извне РСФСР (1,600) и иммиграция извне России (1,300).
         */
        final long nAddChernomorskayaInner = 1_600;
        final long nAddChernomorskayaForeign = 1_300;
        final Long nAddChernomorskaya = nAddChernomorskayaForeign + nAddChernomorskayaInner;

        for (int year = 1896; year <= 1914; year++)
        {
            tds.get("Черноморская").cascadeAdjustProgressivePopulation(year, nAddChernomorskaya);
            tds.get("Черноморская").territoryYearOrNull(year).migration.total.both += nAddChernomorskaya;
        }
    }
}
