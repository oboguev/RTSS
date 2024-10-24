package rtss.pre1917.tools;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.util.Util;

/*
 * Пометить сильные уклонения в числе рождений или смертей от соседних лет
 */
public class FlagOutliers
{
    public static void main(String[] args)
    {
        try
        {
            new FlagOutliers("births", 0.2).flagOutliers();
            new FlagOutliers("deaths", 0.2).flagOutliers();
            
            Util.out("** Done");
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private final TerritoryDataSet tds = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY,
                                                                 LoadOptions.MERGE_CITIES,
                                                                 LoadOptions.EVAL_PROGRESSIVE,
                                                                 LoadOptions.ADJUST_FEMALE_BIRTHS,
                                                                 LoadOptions.FILL_MISSING_BD);
    private final String what;
    private final double threshold;

    private FlagOutliers(String what, double threshold) throws Exception
    {
        this.what = what;
        this.threshold = threshold;
    }

    private void flagOutliers() throws Exception
    {
        for (String tname : tds.keySet())
        {
            if (!Taxon.isComposite(tname))
                flagOutliers(tname);
        }
    }

    private void flagOutliers(String tname) throws Exception
    {
        for (int year = 1896; year <= 1916; year++)
            flagOutliers(tname, year);
    }

    private void flagOutliers(String tname, int year) throws Exception
    {
        int y1 = year;
        int y2 = year + 1;
        
        if (y1 == 1905 || y2 == 1905)
            return;

        Territory t = tds.get(tname);
        TerritoryYear ty1 = t.territoryYearOrNull(y1);
        TerritoryYear ty2 = t.territoryYearOrNull(y2);
        if (ty1 == null || ty2 == null)
            return;
        
        Long v1 = null;
        Long v2 = null;
        
        if (what.equals("births"))
        {
            v1 = ty1.births.total.both;
            v2 = ty2.births.total.both;
        }
        else if (what.equals("deaths"))
        {
            v1 = ty1.deaths.total.both;
            v2 = ty2.deaths.total.both;
        }
        else
        {
            throw new Exception("Invalid selector");
        }
        
        if (v1 == null || v2 == null)
            return;
        
        if (v1 < 0 || v2 < 0)
            throw new Exception("Negative value");
        
        long vmin = Math.min(v1, v2);
        long vmax = Math.max(v1, v2);
        
        if (vmin == 0)
            throw new Exception("Zero");
        
        double delta = (vmax - vmin) / vmin;
        
        if (delta > threshold)
        {
            Util.out(String.format("Расхождение %s для %d -> %d %s: %,d -> %,d, delta = %,d (%.1f%%)", 
                                   what, y1, y2, tname, v1, v2, Math.abs(v1 - v2), delta * 100));
        }
    }
}
