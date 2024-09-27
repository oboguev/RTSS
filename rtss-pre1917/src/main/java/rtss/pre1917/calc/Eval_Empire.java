package rtss.pre1917.calc;

import java.util.HashMap;
import java.util.Map;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.eval.MergeTaxon;
import rtss.pre1917.eval.MergeTaxon.WhichYears;
import rtss.pre1917.data.Territory;
import rtss.util.Util;

/*
 * Определить численность, рождаемость и смертность населения 
 * в границах Российской Империи для 1896-1913 гг.
 */
public class Eval_Empire
{
    public static void main(String[] args)
    {
        try
        {
            new Eval_Empire().calc();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    Territory tmUGVI;

    /*
     * Годовое приращение населения, включая естественное + механическое
     */
    private Map<Integer, Long> year2growth = new HashMap<>();

    /*
     * Численность населения в начале года
     */
    private Map<Integer, Long> year2population = new HashMap<>();

    private void calc() throws Exception
    {
        calcYearlyNaturalGrowth();
        adjustForEmigration();
        seed1897();

        for (int year = 1898; year <= 1914; year++)
            year2population.put(year + 1, year2population.get(year) + year2growth.get(year));

        Util.out("Численность населения в границах Росийской Империи, рождаемость, смертность, естественный прирост, ест. + мех. изменение численности");
        Util.out("");

        for (int year = 1896; year <= 1913; year++)
        {
            long pop = year2population.get(year);
            TerritoryYear ty = tmUGVI.territoryYear(year);
            double cbr = (1000.0 * ty.births.total.both) / pop;
            double cdr = (1000.0 * ty.deaths.total.both) / pop;
            double ngr = cbr - cdr;
            Util.out(String.format("%d %,d %.1f %.1f %.1f %,d", 
                                   year, pop, cbr, cdr, ngr, 
                                   year2population.get(year + 1) - pop));
        }

        Util.out(String.format("%d %,d", 1914, year2population.get(1914)));
    }

    private void seed1897() throws Exception
    {
        TerritoryDataSet tdsCensus = new LoadData().loadCensus1897(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
        Territory tm = MergeTaxon.mergeTaxon(tdsCensus, "Империя", WhichYears.AllSetYears);
        TerritoryYear ty = tm.territoryYear(1897);

        long yincr = year2growth.get(1897);

        long in1 = Math.round(yincr * 27.0 / 365.0);
        long in2 = yincr - in1;

        year2population.put(1897, ty.population.total.both - in1);
        year2population.put(1898, ty.population.total.both + in2);

        year2population.put(1896, year2population.get(1897) - year2growth.get(1896));
    }

    private void calcYearlyNaturalGrowth() throws Exception
    {
        // вычислить естественное приращение
        TerritoryDataSet tdsUGVI = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY, 
                                                           LoadOptions.MERGE_CITIES, 
                                                           LoadOptions.ADJUST_BIRTHS,
                                                           LoadOptions.FILL_MISSING_BD);
        tmUGVI = MergeTaxon.mergeTaxon(tdsUGVI, "Империя", WhichYears.AllSetYears);

        for (int year = 1896; year <= 1914; year++)
        {
            TerritoryYear ty = tmUGVI.territoryYear(year);
            year2growth.put(year, ty.births.total.both - ty.deaths.total.both);
        }
    }

    private void adjustForEmigration() throws Exception
    {
        Map<Integer,Long> emigration = new LoadData().loadEmigration();

        for (int year = 1896; year <= 1914; year++)
            year2growth.put(year, year2growth.get(year) - emigration.get(year));
    }
}
