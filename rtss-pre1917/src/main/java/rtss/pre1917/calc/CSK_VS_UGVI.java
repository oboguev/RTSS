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
 * Сопоставить численность населения по ЦСК и по УГВИ
 */
public class CSK_VS_UGVI
{
    public static void main(String[] args)
    {
        try
        {
            new CSK_VS_UGVI().calc();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private void calc() throws Exception
    {
        TerritoryDataSet tdsCSK = new LoadData().loadEzhegodnikRossii(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
        TerritoryDataSet tdsUGVI = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);

        Territory tmCSK = MergeTaxon.mergeTaxon(tdsCSK, "Империя", WhichYears.AllSetYears);
        Territory tmUGVI = MergeTaxon.mergeTaxon(tdsUGVI, "Империя", WhichYears.AllSetYears);

        for (int year = 1904; year <= 1915; year++)
        {
            TerritoryYear tyCSK = tmCSK.territoryYear(year);
            TerritoryYear tyUGVI = tmUGVI.territoryYear(year);

            Util.out(String.format("%d %,d %,d", year, tyCSK.population.total.both, tyUGVI.population.total.both));
        }
    }

}
