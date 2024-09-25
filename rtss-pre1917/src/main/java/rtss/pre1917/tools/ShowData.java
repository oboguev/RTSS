package rtss.pre1917.tools;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.util.FieldValue;
import rtss.util.Util;

public class ShowData
{
    public static void main(String[] args)
    {
        try
        {
            // TerritoryDataSet tds = new LoadData().loadEvroChast(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
            // new ShowData().show(tds, "50 губерний Европейской России", "population.total.both", 1897, 1914);
            // new ShowData().show(tds, "Архангельская", "births.total.both", 1897, 1914);
            // new ShowData().show(tds, "Ярославская", "deaths.total.both", 1897, 1914);
            
            // TerritoryDataSet tds = new LoadData().loadEzhegodnikRossii(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
            // new ShowData().show(tds, "50 губерний Европейской России", "population.total.both", 1904, 1914);
            
            TerritoryDataSet tds = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY, LoadOptions.DONT_MERGE_CITIES);
            new ShowData().show(tds, "Астраханская", "population.total.both", 1897, 1915);
            new ShowData().show(tds, "Владимирская", "population.total.both", 1897, 1915);
            new ShowData().show(tds, "Вологодская", "population.total.both", 1897, 1915);
            new ShowData().show(tds, "Вятская", "population.total.both", 1897, 1915);
            new ShowData().show(tds, "Курская", "population.total.both", 1897, 1915);
            new ShowData().show(tds, "Олонецкая", "population.total.both", 1897, 1915);
            new ShowData().show(tds, "Пермская", "population.total.both", 1897, 1915);
            new ShowData().show(tds, "Саратовская", "population.total.both", 1897, 1915);
            new ShowData().show(tds, "Смоленская", "population.total.both", 1897, 1915);
            new ShowData().show(tds, "Ставропольская", "population.total.both", 1897, 1915);
            new ShowData().show(tds, "Тобольская", "population.total.both", 1897, 1915);
            new ShowData().show(tds, "Ярославская", "population.total.both", 1897, 1915);
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private void show(TerritoryDataSet tds, String tname, String selector, int y1, int y2) throws Exception
    {
        Util.out("");
        Util.out(String.format("%s for %s %d-%d", selector, tname, y1, y2));
        Util.out("");

        for (int year = y1; year <= y2; year++)
        {
            Long v = null;

            Territory t = tds.get(tname);
            if (t != null)
            {
                TerritoryYear ty = t.territoryYear(year);
                if (ty != null)
                    v = FieldValue.getLong(ty, selector);
            }

            // Util.out(String.format("%d %,d", year, v));
            Util.out(String.format("%,d", v));
        }
    }
}
