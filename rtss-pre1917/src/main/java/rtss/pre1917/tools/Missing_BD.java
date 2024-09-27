package rtss.pre1917.tools;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.util.Util;

public class Missing_BD
{
    public static void main(String[] args)
    {
        try
        {
            TerritoryDataSet tds = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY);

            for (String tname : Util.sort(tds.keySet()))
            {
                if (Taxon.isComposite(tname))
                    continue;

                for (int year = 1896; year <= 1914; year++)
                {
                    String v = hasBD(tds, tname, year);
                    if (v != null)
                        Util.out(String.format("Нет данных о движении для %d %s (%s)", year, tname, v));
                }
            }
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private static String hasBD(TerritoryDataSet tds, String tname, int year)
    {
        Territory t = tds.get(tname);
        if (t == null)
            return "no territory";

        TerritoryYear ty = t.territoryYear(year);
        if (ty == null)
            return "no territory year";

        if (ty.births.total.both == null && ty.deaths.total.both == null)
            return "no births+deaths";
        else if (ty.births.total.both == null)
            return "no births";
        else if (ty.deaths.total.both == null)
            return "no deaths";
        else
            return null;
    }
}
