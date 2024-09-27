package rtss.pre1917.calc;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.eval.MergeTaxon;
import rtss.pre1917.eval.MergeTaxon.WhichYears;
import rtss.util.Util;

/*
 * Вычислить расхождение между публиковавшимся УГВИ естественным приростом 
 * и годовым изменением численности населения 
 */
public class EvalDivergence_UGVI
{

    public static void main(String[] args)
    {
        try
        {
            new EvalDivergence_UGVI().calc();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private void calc() throws Exception
    {
        Util.out("Естественный прирост и прирост населения Империи");
        Util.out("");
        
        TerritoryDataSet tdsUGVI = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
        Territory tm = MergeTaxon.mergeTaxon(tdsUGVI, "Империя", WhichYears.AllSetYears);
        
        for (int year = 1892; year <= 1914; year++)
        {
            TerritoryYear ty = tm.territoryYear(year);
            TerritoryYear ty_next = tm.territoryYear(year + 1);
            long incrPOP = ty_next.population.total.both - ty.population.total.both; 

            long incrEP = ty.births.total.both - ty.deaths.total.both;
            
            Util.out(String.format("%d %,d %,d", year, incrEP, incrPOP));
        }
    }
    
}
