package rtss.pre1917.calc;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.calc.containers.TaxonYearData;
import rtss.pre1917.calc.containers.TaxonYearlyPopulationData;
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
            new EvalCountryTaxon("РСФСР-1991", 1914).calc(true).print();
            new EvalCountryTaxon("Империя", 1913).calc(true).print();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

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
                extraDeaths(1904, 25_589);
                extraDeaths(1905, 25_363);
            }
            else if (taxonName.equals("СССР-1991"))
            {
                // ### потери 1905
                // ### потери 1914
            }
            else if (taxonName.equals("РСФСР-1991"))
            {
                extraDeaths(1904, 13_491);
                extraDeaths(1905, 13_372);
                extraDeaths(1914, 94_000);
            }
        }

        /* ===================== Построить структуру с результатом ===================== */

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
}
