package rtss.pre1917.calc;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.InnerMigration;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.eval.MergeTaxon;
import rtss.pre1917.eval.MergeTaxon.WhichYears;
import rtss.util.Util;

public class Eval_RSFSR_1991
{
    public static void main(String[] args)
    {
        try
        {
            new Eval_RSFSR_1991().calc();
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
    
    private final InnerMigration innerMigration = new LoadData().loadInnerMigration();
    
    private final double PROMILLE = 1000.0;
    
    private Eval_RSFSR_1991() throws Exception
    {
    }

    private void calc() throws Exception
    {
        tdsPopulation = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY,
                                                LoadOptions.MERGE_CITIES,
                                                LoadOptions.ADJUST_FEMALE_BIRTHS,
                                                LoadOptions.FILL_MISSING_BD,
                                                LoadOptions.EVAL_PROGRESSIVE);
        tdsPopulation.leaveOnlyTotalBoth();
        eval_1896(tdsPopulation);
        
        tdsVitalRates = tdsPopulation.dup();

        /* пересчёт населения для Дагестана */
        new AdjustTerritories(tdsPopulation).fixDagestan();;

        /* не включать Дагестан в подсчёт рождаемости и смертности */
        Territory t = tdsVitalRates.get("Дагестанская обл.");
        tdsVitalRates.remove(t.name);
        
        tmPopulation = MergeTaxon.mergeTaxon(tdsPopulation, "РСФСР-1991", WhichYears.AllSetYears);
        tmVitalRates = MergeTaxon.mergeTaxon(tdsVitalRates, "РСФСР-1991", WhichYears.AllSetYears);
        
        // ### миграционный баланс ???
        
        Util.out("Численность населения в границах РСФСР-1991, рождаемость, смертность, естественный прирост, ест. + мех. изменение численности");
        Util.out("");

        for (int year = 1896; year <= 1914; year++)
        {
            long pop_total = tmPopulation.territoryYear(year).progressive_population.total.both;
            long pop_total_next = tmPopulation.territoryYear(year + 1).progressive_population.total.both;
            
            TerritoryYear ty = tmVitalRates.territoryYear(year);
            long pop_vital = ty.progressive_population.total.both;
            
            double cbr = (PROMILLE * ty.births.total.both) / pop_vital;
            double cdr = (PROMILLE * ty.deaths.total.both) / pop_vital;
            double ngr = cbr - cdr;
            
            Util.out(String.format("%d %,d %.1f %.1f %.1f %,d",
                                   year, pop_total, cbr, cdr, ngr,
                                   pop_total_next - pop_total));
        }

        Util.out(String.format("%d %,d", 1915, tmPopulation.territoryYear(1915).progressive_population.total.both));
    }
    
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
}
