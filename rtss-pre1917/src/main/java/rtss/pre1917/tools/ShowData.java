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
            
            TerritoryDataSet tds = new LoadData().loadEzhegodnikRossii(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
            new ShowData().show(tds, "50 губерний Европейской России", "population.total.both", 1904, 1914);
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private void show(TerritoryDataSet tds, String tname, String selector, int y1, int y2) throws Exception
    {
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

            Util.out(String.format("%d %,d", year, v));
        }
    }
}
