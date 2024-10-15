package rtss.pre1917.calc;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.calc.containers.TaxonYearData;
import rtss.pre1917.calc.containers.TaxonYearlyPopulationData;
import rtss.pre1917.data.InnerMigration;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.eval.EvalGrowthRate;
import rtss.pre1917.merge.MergeTaxon;
import rtss.pre1917.merge.MergeTaxon.WhichYears;
import rtss.pre1917.validate.CheckProgressiveAvailable;
import rtss.util.Util;

public class EvalCountryTaxon
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

    private TerritoryDataSet tdsPopulation;
    private Territory tmPopulation;

    private TerritoryDataSet tdsVitalRates;
    private Territory tmVitalRates;
    
    private final TerritoryDataSet tdsCensus1897 = new LoadData().loadCensus1897(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
    private final InnerMigration innerMigration = new LoadData().loadInnerMigration();
    private final EvalGrowthRate evalGrowthRate = new  EvalGrowthRate(tdsCensus1897, innerMigration);

    private final double PROMILLE = 1000.0;

    private final String taxonName;
    private final int toYear;

    private static TaxonYearlyPopulationData typdRusEvro;

    private EvalCountryTaxon(String taxonName, int toYear) throws Exception
    {
        this.taxonName = taxonName;
        this.toYear = toYear;

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

        new CheckProgressiveAvailable(tdsPopulation).check();
        

        /* ===================== Естественное движение ===================== */

        tdsVitalRates = tdsPopulation.dup();

        /* ===================== Правки ===================== */

        if (taxonName.equals("русские губернии Европейской России и Кавказа, кроме Черноморской"))
        {
            // no-op
        }
        else if (taxonName.equals("Империя") || taxonName.equals("СССР-1991") || taxonName.equals("РСФСР-1991"))
        {
            /* пересчёт численности населения для Дагестана */
            new AdjustTerritories(tdsPopulation).fixDagestan();

            /* не включать Дагестан в подсчёт рождаемости и смертности */
            excludeFromVitalRates("Дагестанская обл.");
        }
        else if (taxonName.equals("Империя") || taxonName.equals("СССР-1991"))
        {
            if (Util.False)
            {
                // ###
                useStabilized("Закаспийская обл.", 1911, 1913);
                useStabilized("Семиреченская обл.", 1912, 1914);
                useStabilized("Сыр-Дарьинская обл.", 1908);
                useStabilized("Ферганская обл.", 1912);
                useStabilized("Карсская обл.", 1907, 1913);
                useStabilized("Тифлисская", 1903, 1914);
            }

            // ### особо: Самаркандской 
            // ### особо: Семипалатинской 
            // ### особо: Уральской област
            // ### особо: Бакинская губерния с Баку: 
            // ### особо: Кутаисская губерния с Батумской областью
            
            excludeFromVitalRates("Елисаветпольская");
        }
        else
        {
            throw new Exception("Неверный таксон " + taxonName);
        }

        /* ===================== Суммирование по таксону ===================== */

        tmPopulation = MergeTaxon.mergeTaxon(tdsPopulation, taxonName, WhichYears.AllSetYears);
        tmVitalRates = MergeTaxon.mergeTaxon(tdsVitalRates, taxonName, WhichYears.AllSetYears);

        /* ===================== Учёт военных потерь и эмиграции ===================== */

        if (taxonName.equals("Империя"))
        {
            // ### потери 1905
            // ### потери 1914
            // ### черноморск 1300
        }
        else if (taxonName.equals("СССР-1991"))
        {
            // ### потери 1905
            // ### потери 1914
            // ### черноморск 1300
        }
        else if (taxonName.equals("РСФСР-1991"))
        {
            // ### потери 1905
            // ### потери 1914
            // ### черноморск 1300 + 1600
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
    
    /* ================================================================================================ */

    /*
     * Вычислить численность населения губерний и областей на начало 1896 года (прогрессивная оценка)
     */
    private void eval_1896(TerritoryDataSet tds)
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
                in += innerMigration.saldo(tname, 1896);
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
}
