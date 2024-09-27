package rtss.pre1917.tools;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.util.Util;

public class ShowDeaths1898
{
    public static void main(String[] args)
    {
        try
        {
            TerritoryDataSet tds = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY);
            for (String tname : Util.sort(tds.keySet()))
            {
                Long v1 = dval(tds, tname, 1897);
                Long v2 = dval(tds, tname, 1898);
                Long v3 = dval(tds, tname, 1899);
                Util.out(String.format("\"%s\" %,d %,d %,d", tname, v1, v2, v3));
            }
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }
    
    private static Long dval(TerritoryDataSet tds, String tname, int year)
    {
        Territory t = tds.get(tname);
        if (t == null)
            return null;
        TerritoryYear ty = t.territoryYear(year);
        if (ty == null)
            return null;
        return ty.deaths.total.both;
    }
}
