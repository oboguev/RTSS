package rtss.pre1917.tools;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.util.Util;

public class Show1898
{
    public static void main(String[] args)
    {
        try
        {
            TerritoryDataSet tds = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY, LoadOptions.DONT_MERGE_CITIES);
            // TerritoryDataSet tds = new LoadData().loadEvroChast(LoadOptions.DONT_VERIFY, LoadOptions.DONT_MERGE_CITIES);
            
            for (String tname : Util.sort(tds.keySet()))
            {
                Long v1 = val(tds, tname, 1897);
                Long v2 = val(tds, tname, 1898);
                Long v3 = val(tds, tname, 1899);
                
                if (v1 == null && v3 == null)
                    continue;
                if (v2 == null)
                    continue;

                double ave = 0;
                if (v1 == null)
                    ave = v2;
                else if (v3 == null)
                    ave = v1;
                else
                    ave = (v1 + v3) / 2.0;
                
                double r = 100.0 * v2 / ave;
                
                // Util.out(String.format("\"%s\" %,d %,d %,d", tname, v1, v2, v3));
                
                Util.out(String.format("\"%s\" %.1f", tname, r));
            }
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }
    
    private static Long val(TerritoryDataSet tds, String tname, int year)
    {
        Territory t = tds.get(tname);
        if (t == null)
            return null;
        TerritoryYear ty = t.territoryYear(year);
        if (ty == null)
            return null;
        // return ty.deaths.total.both;
        return ty.births.total.both;
    }
}
