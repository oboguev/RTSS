package rtss.pre1917.calc;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.merge.MergeTaxon;
import rtss.pre1917.merge.MergeTaxon.WhichYears;
import rtss.pre1917.util.FieldValue;
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
            new CSK_VS_UGVI().calc_population("Империя");
            new CSK_VS_UGVI().calc_population("50 губерний Европейской России");
            new CSK_VS_UGVI().calc_movement_50("births.total.both");
            new CSK_VS_UGVI().calc_movement_50("deaths.total.both");
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private void calc_population(String txname) throws Exception
    {
        Util.out("");
        Util.out(String.format("Численность населения по ЦСК и по УГВИ для территории: %s", txname));
        Util.out("");
        
        TerritoryDataSet tdsCSK = new LoadData().loadEzhegodnikRossii(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
        TerritoryDataSet tdsUGVI = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);

        Territory tmCSK = MergeTaxon.mergeTaxon(tdsCSK, txname, WhichYears.AllSetYears);
        Territory tmUGVI = MergeTaxon.mergeTaxon(tdsUGVI, txname, WhichYears.AllSetYears);

        for (int year = 1904; year <= 1915; year++)
        {
            TerritoryYear tyCSK = tmCSK.territoryYear(year);
            TerritoryYear tyUGVI = tmUGVI.territoryYear(year);

            Util.out(String.format("%d %,d %,d", year, tyCSK.population.total.both, tyUGVI.population.total.both));
        }
    }

    private void calc_movement_50(String field) throws Exception
    {
        final String txname = "50 губерний Европейской России"; 

        Util.out("");
        Util.out(String.format("Движение населения по ЦСК и по УГВИ для территории: %s, поле %s ", txname, field));
        Util.out("");
        
        TerritoryDataSet tdsEP = new LoadData().loadEvroChast(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
        Territory tmEP50 = MergeTaxon.mergeTaxon(tdsEP, txname, WhichYears.AllSetYears);

        TerritoryDataSet tdsUGVI = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
        Territory tmUGVI = MergeTaxon.mergeTaxon(tdsUGVI, txname, WhichYears.AllSetYears);
        
        for (int year = 1897; year <= 1914; year++)
        {
            TerritoryYear tyUGVI = tmUGVI.territoryYear(year);
            TerritoryYear tyCSK = tmEP50.territoryYear(year);
            
            Long vUGVI = FieldValue.getLong(tyUGVI, field);
            Long vCSK = FieldValue.getLong(tyCSK, field);
            
            Util.out(String.format("%d %,d %,d", year, vCSK, vUGVI));
        }
    }
}
