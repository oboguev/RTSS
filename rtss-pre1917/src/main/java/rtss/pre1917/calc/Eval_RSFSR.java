package rtss.pre1917.calc;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.util.Util;

/*
 * Определить численность, рождаемость и смертность населения 
 * в границах РСФСР-1991 для 1896-1913 гг.
 */
public class Eval_RSFSR
{
    public static void main(String[] args)
    {
        try
        {
            new Eval_RSFSR().calc();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private void calc() throws Exception
    {
        TerritoryDataSet tdsUGVI = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES, LoadOptions.ADJUST_BIRTHS);
    }
}
