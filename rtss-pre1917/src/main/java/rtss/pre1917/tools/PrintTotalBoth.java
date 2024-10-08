package rtss.pre1917.tools;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.util.Util;

public class PrintTotalBoth
{
    public static void main(String[] args)
    {
        try
        {
            TerritoryDataSet tds = new LoadData().loadCensus1897(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
            for (String tname : Util.sort(tds.keySet()))
            {
                TerritoryYear ty = tds.territoryYearOrNull(tname, 1897);
                Util.out(String.format("%s %,d", tname, ty.population.total.both));
            }
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }
}
