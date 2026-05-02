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
import rtss.pre1917.merge.MergeTaxon;
import rtss.pre1917.merge.MergeTaxon.MergeTaxonOptions;
import rtss.pre1917.merge.MergeTaxon.WhichYears;
import rtss.pre1917.validate.CheckProgressiveAvailable;
import rtss.pre1917.war.ApplyWarDeaths;
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

    private static Long EmpirePopulation1904 = null;
    private static Long EmpirePopulation1914 = null;

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

        /* ===================== Загрузить данные о численности и естественном движении населения ===================== */

        tdsPopulation = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY,
                                                LoadOptions.ADJUST_FEMALE_BIRTHS,
                                                LoadOptions.FILL_MISSING_BD,
                                                LoadOptions.MERGE_CITIES,
                                                LoadOptions.MERGE_POST1897_REGIONS,
                                                LoadOptions.EVAL_PROGRESSIVE);
        tdsPopulation.leaveOnlyTotalBoth();

        if (verbose)
        {
            FilterByTaxon.filteredOutByTaxon(taxonName, tdsPopulation).showTerritoryNames("Не используемые территории, в т.ч. составные");
            FilterByTaxon.filterByTaxon(taxonName, tdsPopulation).showTerritoryNames("Территории для численности населения", taxonName, 1914);
        }

        /* ================================= Правки ================================ */

        /*
         * Первоначально tdsVitalRates используется только как список имён территорий (отметка "включать или не включать"), 
         * выводимый из corrections(). 
         * Но сами фактические данные о населении внутри tdsVitalRates затем обновятся из tdsPopulation / tdsExportPopulation. 
         */
        tdsVitalRates = tdsPopulation.dup();

        corrections();
        
        if (taxonName.equals("Империя"))
            calc_empire();
        else
            calc_non_empire();

        /* ===================== Построить структуру с результатом ===================== */

        showPopulationPercentageVitalRatesVsPopulation();

        return buildTaxonYearlyPopulationData();
    }
    
    private void calc_empire() throws Exception
    {
        // do NOT apply war losses to individual territories -- not yet
        
        /* ===================== Сохранить для экспорта данных ===================== */

        tdsExportPopulation = tdsPopulation.dup();
        for (String tname : new HashSet<>(tdsExportPopulation.keySet()))
        {
            if (Taxon.isComposite(tname))
                tdsExportPopulation.remove(tname);
        }
        
        filterPopulationSetsByTaxon();
        Set<String> territoriesExcludedFromVitalRates = refreshVitalSetData();
        checkProgressiveAvailable();
        mergeTaxonPopulation();
        
        /* Часть иммиграции не разбиваемая по губерниям */
        applyLumpImmigration(tmPopulation, true);
        
        if (DoCountMilitaryDeaths)
        {
            // apply war losses manually (in bulk) to tmPopulation
            ApplyWarDeaths.applyToEmpire(tmPopulation);
            
            // apply war losses to individual territories in tdsExport and tdsVital 
            EmpirePopulation1904 = tmPopulation.territoryYearOrNull(1904).progressive_population.total.both;
            EmpirePopulation1914 = tmPopulation.territoryYearOrNull(1914).progressive_population.total.both;
            new ApplyWarDeaths(EmpirePopulation1904, EmpirePopulation1914).apply(tdsExportPopulation);
            new ApplyWarDeaths(EmpirePopulation1904, EmpirePopulation1914).apply(tdsVitalRates);
        }
        else
        {
            EmpirePopulation1904 = null;
            EmpirePopulation1914 = null;
        }
        
        mergeTaxonVitalRates(territoriesExcludedFromVitalRates);
        
        /* Часть иммиграции не разбиваемая по губерниям */
        applyLumpImmigration(tmVitalRates, false);
    }

    private void calc_non_empire() throws Exception
    {
        if (DoCountMilitaryDeaths)
        {
            // apply war losses to individual territories
            if (EmpirePopulation1904 == null || EmpirePopulation1914 == null)
                throw new Exception("Сначала нужно вычислить таксон Империя");
            new ApplyWarDeaths(EmpirePopulation1904, EmpirePopulation1914).apply(tdsPopulation);
        }

        filterPopulationSetsByTaxon();
        Set<String> territoriesExcludedFromVitalRates = refreshVitalSetData();
        checkProgressiveAvailable();
        mergeTaxonPopulation();
        mergeTaxonVitalRates(territoriesExcludedFromVitalRates);

        // do NOT apply war losses to tmPopulation or tmVital, as they were already applied 
        
        /* Часть иммиграции не разбиваемая по губерниям */
        applyLumpImmigration(tmPopulation, true);
        applyLumpImmigration(tmVitalRates, false);
    }
    
    private void filterPopulationSetsByTaxon() throws Exception
    {
        tdsPopulation = FilterByTaxon.filterByTaxon(taxonName, tdsPopulation);
        tdsVitalRates = FilterByTaxon.filterByTaxon(taxonName, tdsVitalRates);
    }
    
    private Set<String> refreshVitalSetData() throws Exception
    {
        Set<String> territoriesExcludedFromVitalRates = new HashSet<>();
        
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
                if (Taxon.isComposite(tname))
                    Util.err("Composite taxon in vital rates: " + tname);
                tdsVitalRates.put(tname, t.dup());
            }
        }

        if (tdsVitalRates.size() == tdsPopulation.size())
        {
            Util.out("Для подсчёта естественного движения используются все входящие территории");
        }
        else
        {
            Util.out("Для подсчёта естественного движения не используются территории:");
            for (String tname : Util.sort(tdsPopulation.keySet()))
            {
                if (!tdsVitalRates.containsKey(tname))
                {
                    territoriesExcludedFromVitalRates.add(tname);
                    Util.out("    " + tname);
                }
            }

        }
        
        return territoriesExcludedFromVitalRates;
    }
    
    private void checkProgressiveAvailable() throws Exception
    {
        new CheckProgressiveAvailable(tdsPopulation).check(toYear + 1);
        new CheckProgressiveAvailable(tdsVitalRates).check(toYear + 1);
        if (tdsExportPopulation != null)
            new CheckProgressiveAvailable(tdsExportPopulation).check(toYear + 1);
    }
    
    private void mergeTaxonPopulation() throws Exception
    {
        tmPopulation = MergeTaxon.mergeTaxon(tdsPopulation, taxonName, WhichYears.AllSetYears, new MergeTaxonOptions()
                .flagMissing("progressive_population.total.both", 1896, toYear + 1)
                .allowMissingTeritory("Закатальский окр.")
                .allowMissingTeritory("Сухумский окр."));
    }

    private void mergeTaxonVitalRates(Set<String> territoriesExcludedFromVitalRates) throws Exception
    {
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
    }

    private TaxonYearlyPopulationData buildTaxonYearlyPopulationData()
    {
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
     * Часть иммиграции не разбиваемая по губерниям
     */
    private void applyLumpImmigration(Territory tm, boolean print)
    {
        long lumpTotal = 0;

        for (int year = 1896; year <= toYear; year++)
        {
            LumpImmigration lump = immigration.lumpImmigrationForYear(year);
            final double TurkeyFactor = 2.33;
            long lumpYearSum = 0;

            switch (taxonName)
            {
            case "Империя":
                lumpYearSum += immigration(tm, year, Math.round(lump.turkey * TurkeyFactor));
                lumpYearSum += immigration(tm, year, lump.persia);
                lumpYearSum += immigration(tm, year, lump.japan);
                lumpYearSum += immigration(tm, year, lump.china);
                break;

            case "СССР-1991":
                lumpYearSum += immigration(tm, year, Math.round(lump.turkey * TurkeyFactor));
                lumpYearSum += immigration(tm, year, lump.persia);
                lumpYearSum += immigration(tm, year, lump.japan);
                lumpYearSum += immigration(tm, year, lump.china);
                break;

            case "РСФСР-1991":
            case "Сибирь":
                lumpYearSum += immigration(tm, year, lump.japan);
                lumpYearSum += immigration(tm, year, lump.china);
                break;

            case "Кавказ":
                lumpYearSum += immigration(tm, year, Math.round(lump.turkey * TurkeyFactor));
                lumpYearSum += immigration(tm, year, lump.persia);
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

            if (tm.territoryYearOrNull(year).migration.total.both == null)
            {
                tm.territoryYearOrNull(year).migration.total.both = lumpYearSum;
            }
            else
            {
                tm.territoryYearOrNull(year).migration.total.both += lumpYearSum;
            }
        }

        if (print)
            Util.out(String.format("Величина части иммиграции не разбиваемой по губерниям, с 1896 по конец периода: %,d", lumpTotal));
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
