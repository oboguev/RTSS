package rtss.pre1917.calc;

import java.util.HashSet;
import java.util.Set;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.calc.containers.TaxonYearData;
import rtss.pre1917.calc.containers.TaxonYearlyPopulationData;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.data.migration.ImmigrationYear.LumpImmigration;
import rtss.pre1917.eval.ApplyWarDeaths;
import rtss.pre1917.merge.MergeTaxon;
import rtss.pre1917.merge.MergeTaxon.MergeTaxonOptions;
import rtss.pre1917.merge.MergeTaxon.WhichYears;
import rtss.pre1917.validate.CheckProgressiveAvailable;
import rtss.util.Util;

public class EvalCountryTaxon extends EvalCountryBase
{
    public static void main(String[] args)
    {
        if (DoCountMilitaryDeaths)
        {
            Util.out("В этом варианте расчёта учитываются военные смерти в 1904-1905 и 1914 годах");
        }
        else
        {
            Util.out("В этом варианте расчёта не учитываются военные смерти в 1904-1905 и 1914 годах");
        }

        try
        {
            new EvalCountryTaxon("Империя", 1913).calc(true).print().printDifferenceWithCSK().printDifferenceWithUGVI()
                    .exportData("c:\\@\\Final.csv", "c:\\@\\Final.txt");
            new EvalCountryTaxon("РСФСР-1991", 1914).calc(true).print();
            new EvalCountryTaxon("СССР-1991", 1913).calc(true).print();

            new EvalCountryTaxon("Европейская часть РСФСР-1991", 1914).calc(true).print();
            new EvalCountryTaxon("Сибирь", 1913).calc(true).print();
            new EvalCountryTaxon("Новороссия", 1913).calc(true).print();
            new EvalCountryTaxon("Малороссия", 1913).calc(true).print();
            new EvalCountryTaxon("Белоруссия", 1913).calc(true).print();
            new EvalCountryTaxon("Литва", 1913).calc(true).print();
            new EvalCountryTaxon("Кавказ", 1913).calc(true).print();
            new EvalCountryTaxon("Средняя Азия", 1913).calc(true).print();
            new EvalCountryTaxon("привислинские губернии", 1913).calc(true).print();
            new EvalCountryTaxon("Остзейские губернии", 1913).calc(true).print();
            new EvalCountryTaxon("50 губерний Европейской России", 1913).calc(true).print();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private Territory tmPopulation;
    private Territory tmVitalRates;
    private TerritoryDataSet tdsExportPopulation;

    private static TaxonYearlyPopulationData typdRusEvro;

    private final static boolean DoCountMilitaryDeaths = Util.True;

    private final String RusEvro = "русские губернии Европейской России и Кавказа, кроме Черноморской";

    private EvalCountryTaxon(String taxonName, int toYear) throws Exception
    {
        super(taxonName, toYear);

        if (Util.False && typdRusEvro == null && !taxonName.equals(RusEvro))
            typdRusEvro = new EvalCountryTaxon(RusEvro, 1914).calc(false);
    }

    private TaxonYearlyPopulationData calc(boolean verbose) throws Exception
    {
        if (verbose)
        {
            Util.out("");
            Util.out("====================================================================================================================");
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
        {
            FilterByTaxon.filteredOutByTaxon(taxonName, tdsPopulation).showTerritoryNames("Не используемые территории, в т.ч. составные");
            FilterByTaxon.filterByTaxon(taxonName, tdsPopulation).showTerritoryNames("Территории для численности населения");
        }

        /* ================================= Правки ================================ */

        tdsVitalRates = tdsPopulation.dup();
        Set<String> territoriesExcludedFromVitalRates = new HashSet<>();

        corrections();

        /* ===================== Сохранить для экспорта данных ===================== */

        switch (taxonName)
        {
        case "Империя":
            tdsExportPopulation = tdsPopulation.dup();
            for (String tname : new HashSet<>(tdsExportPopulation.keySet()))
            {
                if (Taxon.isComposite(tname))
                    tdsExportPopulation.remove(tname);
            }
            break;
        }

        /* ================================================================= */

        tdsPopulation = FilterByTaxon.filterByTaxon(taxonName, tdsPopulation);
        tdsVitalRates = FilterByTaxon.filterByTaxon(taxonName, tdsVitalRates);

        /* ===================== Естественное движение ===================== */

        for (String tname : new HashSet<>(tdsVitalRates.keySet()))
        {
            Territory t = tdsPopulation.get(tname);
            if (t == null)
            {
                if (!Taxon.isComposite(tname))
                    Util.err("In vital rates set but not in population set: " + tname);
                tdsVitalRates.remove(tname);
            }
            else
            {
                tdsVitalRates.put(tname, t.dup());
            }
        }

        if (tdsVitalRates.size() == tdsPopulation.size())
        {
            Util.out("Для подсчёта естественного движения используются все входящие территории");
        }
        else
        {
            Util.out("Для подсчёта естественного движения не используются:");
            for (String tname : Util.sort(tdsPopulation.keySet()))
            {
                if (!tdsVitalRates.containsKey(tname))
                {
                    territoriesExcludedFromVitalRates.add(tname);
                    Util.out("    " + tname);
                }
            }

        }

        new CheckProgressiveAvailable(tdsPopulation).check(toYear + 1);

        /* ===================== Суммирование по таксону ===================== */

        tmPopulation = MergeTaxon.mergeTaxon(tdsPopulation, taxonName, WhichYears.AllSetYears, new MergeTaxonOptions()
                .flagMissing("progressive_population.total.both", 1896, toYear + 1)
                .allowMissingTeritory("Закатальский окр.")
                .allowMissingTeritory("Сухумский окр."));

        tmVitalRates = MergeTaxon.mergeTaxon(tdsVitalRates, taxonName, WhichYears.AllSetYears, new MergeTaxonOptions()
                .flagMissing("progressive_population.total.both", 1896, toYear + 1)
                .flagMissing("births.total.both", 1896, toYear)
                .flagMissing("deaths.total.both", 1896, toYear)
                .allowMissingTeritories(territoriesExcludedFromVitalRates)
                .allowMissingSelectorsTeritoriesYears(Set.of("births.total.both", "deaths.total.both"),
                                                      Set.of("Сахалин"),
                                                      Set.of(1903, 1904, 1905, 1906, 1907))
                .allowMissingTeritory("Закатальский окр.")
                .allowMissingTeritory("Сухумский окр."));

        /* ===================== Часть иммиграции не разбиваемая по губерниям ===================== */

        long lumpTotal = 0;

        for (int year = 1896; year <= toYear; year++)
        {
            LumpImmigration lump = immigration.lumpImmigrationForYear(year);
            final double TurkeyFactor = 2.33;
            long lumpYearSum = 0;

            switch (taxonName)
            {
            case "Империя":
                lumpYearSum += immigration(tmPopulation, year, Math.round(lump.turkey * TurkeyFactor));
                lumpYearSum += immigration(tmPopulation, year, lump.persia);
                lumpYearSum += immigration(tmPopulation, year, lump.japan);
                lumpYearSum += immigration(tmPopulation, year, lump.china);
                break;

            case "СССР-1991":
                lumpYearSum += immigration(tmPopulation, year, Math.round(lump.turkey * TurkeyFactor));
                lumpYearSum += immigration(tmPopulation, year, lump.persia);
                lumpYearSum += immigration(tmPopulation, year, lump.japan);
                lumpYearSum += immigration(tmPopulation, year, lump.china);
                break;

            case "РСФСР-1991":
            case "Сибирь":
                lumpYearSum += immigration(tmPopulation, year, lump.japan);
                lumpYearSum += immigration(tmPopulation, year, lump.china);
                break;

            case "Кавказ":
                lumpYearSum += immigration(tmPopulation, year, Math.round(lump.turkey * TurkeyFactor));
                lumpYearSum += immigration(tmPopulation, year, lump.persia);
                break;

            case "Новороссия":
            case "Малороссия":
            case "привислинские губернии":
            case "остзейские губернии":
            case "Литва":
            case "Средняя Азия":
                break;
            }

            lumpTotal += lumpYearSum;

            if (tmPopulation.territoryYearOrNull(year).migration.total.both == null)
            {
                tmPopulation.territoryYearOrNull(year).migration.total.both = lumpYearSum;
            }
            else
            {
                tmPopulation.territoryYearOrNull(year).migration.total.both += lumpYearSum;
            }
        }

        Util.out(String.format("Величина части иммиграции не разбиваемой по губерниям, с 1896 по конец периода: %,d", lumpTotal));

        /* ===================== Учёт военных потерь ===================== */

        if (DoCountMilitaryDeaths)
        {
            // Империя: japaneseWarDeaths(1.0)
            // СССР-1991: japaneseWarDeaths(0.926)
            // РСФСР-1991: japaneseWarDeaths(0.527)
            japaneseWarDeaths();

            if (taxonName.equals("РСФСР-1991"))
                extraDeaths(1914, 94_000);
        }

        switch (taxonName)
        {
        case "Империя":
            long empirePopulation1904 = tmPopulation.territoryYearOrNull(1904).progressive_population.total.both;
            long empirePopulation1914 = tmPopulation.territoryYearOrNull(1914).progressive_population.total.both;
            new ApplyWarDeaths(empirePopulation1904, empirePopulation1914).apply(tdsExportPopulation);
            break;
        }

        /* ===================== Построить структуру с результатом ===================== */

        showPopulationPercentageVitalRatesVsPopulation();

        TaxonYearlyPopulationData cd = new TaxonYearlyPopulationData(taxonName,
                                                                     tdsPopulation,
                                                                     tdsVitalRates,
                                                                     tdsCSK,
                                                                     tdsExportPopulation,
                                                                     toYear);
        TaxonYearData yd;

        for (int year = 1896; year <= toYear; year++)
        {
            yd = new TaxonYearData();

            long pop_total = tmPopulation.territoryYear(year).progressive_population.total.both;
            long pop_total_next = tmPopulation.territoryYear(year + 1).progressive_population.total.both;

            TerritoryYear ty = tmVitalRates.territoryYear(year);
            long pop_vital = ty.progressive_population.total.both;

            TerritoryYear ty_next = tmVitalRates.territoryYear(year + 1);
            long pop_vital_next = ty_next.progressive_population.total.both;
            long pop_vital_middle = (pop_vital + pop_vital_next) / 2;

            double cbr = (PROMILLE * ty.births.total.both) / pop_vital;
            double cdr = (PROMILLE * ty.deaths.total.both) / pop_vital;

            double cbr_middle = (PROMILLE * ty.births.total.both) / pop_vital_middle;
            double cdr_middle = (PROMILLE * ty.deaths.total.both) / pop_vital_middle;

            yd.population = pop_total;
            yd.cbr = cbr;
            yd.cdr = cdr;
            yd.cbr_middle = cbr_middle;
            yd.cdr_middle = cdr_middle;
            yd.population_increase = pop_total_next - pop_total;
            yd.migration = tmPopulation.territoryYear(year).migration.total.both;

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

    private long immigration(Territory t, int year, long amount)
    {
        t.cascadeAdjustProgressivePopulation(year, amount);
        return amount;
    }

    private void showPopulationPercentageVitalRatesVsPopulation() throws Exception
    {
        Util.out(String.format("Для расчёта естественого движения в таксоне %s использованы территории включающие", taxonName));
        Util.out(String.format("    в %d-%d годах %.1f%% населения", 1896, toYear, populationPercentageVitalRatesVsPopulation(1896, toYear)));
        Util.out(String.format("    в %d году %.1f%% населения", 1896, populationPercentageVitalRatesVsPopulation(1896, 1896)));
        Util.out(String.format("    в %d году %.1f%% населения", toYear, populationPercentageVitalRatesVsPopulation(toYear, toYear)));
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
