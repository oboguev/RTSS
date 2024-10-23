package rtss.pre1917.calc;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.calc.containers.TaxonYearData;
import rtss.pre1917.calc.containers.TaxonYearlyPopulationData;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.merge.MergeTaxon;
import rtss.pre1917.merge.MergeTaxon.WhichYears;
import rtss.pre1917.validate.CheckProgressiveAvailable;
import rtss.util.Util;

public class EvalCountryTaxon extends EvalCountryBase
{
    public static void main(String[] args)
    {
        try
        {
            new EvalCountryTaxon("Империя", 1913).calc(true).print();
            new EvalCountryTaxon("РСФСР-1991", 1914).calc(true).print();
            new EvalCountryTaxon("СССР-1991", 1913).calc(true).print();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private Territory tmPopulation;
    private Territory tmVitalRates;

    private static TaxonYearlyPopulationData typdRusEvro;

    private final static boolean DoCountMilitaryDeaths = Util.True;

    private EvalCountryTaxon(String taxonName, int toYear) throws Exception
    {
        super(taxonName, toYear);

        if (typdRusEvro == null && !taxonName.equals("русские губернии Европейской России и Кавказа, кроме Черноморской"))
            typdRusEvro = new EvalCountryTaxon("русские губернии Европейской России и Кавказа, кроме Черноморской", 1914).calc(false);
    }

    private TaxonYearlyPopulationData calc(boolean verbose) throws Exception
    {
        if (verbose)
        {
            Util.out("***************************************************************************************************");
            Util.out("");
            Util.out("Расчёт для " + taxonName);
            Util.out("");
        }

        /* ===================== Численность населения ===================== */

        tdsPopulation = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY,
                                                LoadOptions.ADJUST_FEMALE_BIRTHS,
                                                LoadOptions.FILL_MISSING_BD,
                                                LoadOptions.MERGE_CITIES,
                                                LoadOptions.MERGE_POST1897_REGIONS,
                                                LoadOptions.EVAL_PROGRESSIVE);
        tdsPopulation.leaveOnlyTotalBoth();
        eval_1896(tdsPopulation);

        if (verbose)
            FilterByTaxon.filteredOutByTaxon(taxonName, tdsPopulation).showTerritoryNames("Не используемые территории, в т.ч. составные");

        tdsPopulation = FilterByTaxon.filterByTaxon(taxonName, tdsPopulation);

        if (verbose)
            tdsPopulation.showTerritoryNames("Территории для численности населения");

        new CheckProgressiveAvailable(tdsPopulation).check(toYear + 1);

        /* ===================== Естественное движение ===================== */

        tdsVitalRates = tdsPopulation.dup();

        /* ===================== Правки ===================== */

        corrections();

        /* ===================== Суммирование по таксону ===================== */

        tmPopulation = MergeTaxon.mergeTaxon(tdsPopulation, taxonName, WhichYears.AllSetYears);
        tmVitalRates = MergeTaxon.mergeTaxon(tdsVitalRates, taxonName, WhichYears.AllSetYears);

        /* ===================== Учёт военных потерь ===================== */

        if (DoCountMilitaryDeaths)
        {
            /*
             * Военные потери
             */
            if (taxonName.equals("Империя"))
            {
                japaneseWarDeaths();
                // japaneseWarDeaths(1.0);
            }
            else if (taxonName.equals("СССР-1991"))
            {
                japaneseWarDeaths();
                // japaneseWarDeaths(0.926);
            }
            else if (taxonName.equals("РСФСР-1991"))
            {
                japaneseWarDeaths();
                // japaneseWarDeaths(0.527);
                extraDeaths(1914, 94_000);
            }
        }

        /* ===================== Построить структуру с результатом ===================== */
        
        showPopulationPercentageVitalRatesVsPopulation();

        TaxonYearlyPopulationData cd = new TaxonYearlyPopulationData(taxonName);
        TaxonYearData yd;

        for (int year = 1896; year <= toYear; year++)
        {
            yd = new TaxonYearData();

            long pop_total = tmPopulation.territoryYear(year).progressive_population.total.both;
            long pop_total_next = tmPopulation.territoryYear(year + 1).progressive_population.total.both;

            TerritoryYear ty = tmVitalRates.territoryYear(year);
            long pop_vital = ty.progressive_population.total.both;

            double cbr = (PROMILLE * ty.births.total.both) / pop_vital;
            double cdr = (PROMILLE * ty.deaths.total.both) / pop_vital;

            yd.population = pop_total;
            yd.cbr = cbr;
            yd.cdr = cdr;
            yd.population_increase = pop_total_next - pop_total;

            cd.put(year, yd);
        }

        yd = new TaxonYearData();
        yd.population = tmPopulation.territoryYear(toYear + 1).progressive_population.total.both;
        cd.put(toYear + 1, yd);

        return cd;
    }
    
    /*
     * Число военных смертей для всей Империи в 1904 и 1905 гг.
     */
    private final double EmpireWarDeaths_1904 = 25_589;
    private final double EmpireWarDeaths_1905 = 25_363;
    private static Long EmpirePopulation1904 = null; 
    
    private void japaneseWarDeaths() throws Exception
    {
        TerritoryYear ty = tmPopulation.territoryYearOrNull(1904);
        if (taxonName.equals("Империя"))
        {
            EmpirePopulation1904 = ty.progressive_population.total.both;
            japaneseWarDeaths(1.0); 
        }
        else 
        {
            if (EmpirePopulation1904 == null)
                throw new Exception("Не расчитано население Империи");
            double fraction = (double) ty.progressive_population.total.both / EmpirePopulation1904;
            Util.out(String.format("Доля военных смертей в войне 1904-1905 гг. для %s: %.1f%% (население: %,d из %,d)", 
                                   taxonName, fraction * 100, ty.progressive_population.total.both, EmpirePopulation1904));
            japaneseWarDeaths(fraction); 
        }
    }
    
    private void japaneseWarDeaths(double fraction) throws Exception
    {
        extraDeaths(1904, Math.round(EmpireWarDeaths_1904 * fraction));
        extraDeaths(1905, Math.round(EmpireWarDeaths_1905 * fraction));
    }

    protected void extraDeaths(int year, long deaths) throws Exception
    {
        tmPopulation.extraDeaths(year, deaths);
        tmVitalRates.extraDeaths(year, deaths);
    }
    
    private void showPopulationPercentageVitalRatesVsPopulation() throws Exception
    {
        Util.out(String.format("Для расчёта естественого движения в таксоне %s использованы территории включающие", taxonName));
        Util.out(String.format("    в %d-%d годах %.1f населения", 1896, toYear, populationPercentageVitalRatesVsPopulation(1896, toYear)));
        Util.out(String.format("    в %d году %.1f населения", 1896, populationPercentageVitalRatesVsPopulation(1896, 1896)));
        Util.out(String.format("    в %d году  %.1f населения", toYear, populationPercentageVitalRatesVsPopulation(toYear, toYear)));
    }
    
    private double populationPercentageVitalRatesVsPopulation(int y1, int y2) throws Exception
    {
        long p1 = 0;
        long p2 = 0;
        
        for (int year = y1; year <= y2; year++)
        {
            TerritoryYear ty = tmVitalRates.territoryYearOrNull(year);
            p1 += ty.progressive_population.total.both;
            
            ty = tmPopulation.territoryYearOrNull(year);
            p2 += ty.progressive_population.total.both;
        }
        
        return (100.0 * p1) / p2;
    }
}
