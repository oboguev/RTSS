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
    private TerritoryDataSet tds;

    public static void main(String[] args)
    {
        try
        {
            new ShowData().show_1();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    /* ============================================================================================== */

    private void show_1() throws Exception
    {
        // tds = new LoadData().loadEvroChast(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
        // show(tds, "50 губерний Европейской России", "population.total.both", 1897, 1914);
        // show(tds, "Архангельская", "births.total.both", 1897, 1914);
        // show(tds, "Ярославская", "deaths.total.both", 1897, 1914);

        // tds = new LoadData().loadEzhegodnikRossii(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
        // show(tds, "50 губерний Европейской России", "population.total.both", 1904, 1914);

        tds = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
        show(tds, "Астраханская", "population.total.both", 1897, 1915);
        show(tds, "Владимирская", "population.total.both", 1897, 1915);
        show(tds, "Вологодская", "population.total.both", 1897, 1915);
        show(tds, "Вятская", "population.total.both", 1897, 1915);
        show(tds, "Курская", "population.total.both", 1897, 1915);
        show(tds, "Олонецкая", "population.total.both", 1897, 1915);
        show(tds, "Пермская", "population.total.both", 1897, 1915);
        show(tds, "Саратовская", "population.total.both", 1897, 1915);
        show(tds, "Смоленская", "population.total.both", 1897, 1915);
        show(tds, "Ставропольская", "population.total.both", 1897, 1915);
        show(tds, "Тобольская", "population.total.both", 1897, 1915);
        show(tds, "Ярославская", "population.total.both", 1897, 1915);
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
