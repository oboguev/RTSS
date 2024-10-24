package rtss.pre1917.tools;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.merge.MergeTaxon;
import rtss.pre1917.merge.MergeTaxon.WhichYears;
import rtss.util.Util;

/*
 * Пометить сильные уклонения в числе рождений или смертей в губернии от соседних лет
 */
public class FlagOutliers
{
    public static void main(String[] args)
    {
        try
        {
            new FlagOutliers("births", 0.28).flagOutliers();
            new FlagOutliers("deaths", 0.28).flagOutliers();

            Util.out("** Done");
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private final TerritoryDataSet tds;
    private final String what;
    private final double threshold;
    private final boolean ShowCompositeDivergences = Util.False;

    private FlagOutliers(String what, double threshold) throws Exception
    {
        this.what = what;
        this.threshold = threshold;
        this.tds = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY);
    }

    private void flagOutliers() throws Exception
    {
        for (String tname : Util.sort(tds.keySet()))
        {
            if (!Taxon.isComposite(tname))
            {
                if (flagOutliers(tname))
                    Util.out("");
            }
        }
    }

    private boolean flagOutliers(String tname) throws Exception
    {
        boolean res = false;
        for (int year = 1896; year <= 1916; year++)
        {
            boolean x = flagOutliers(tname, year);
            res = res || x;
        }
        return res;
    }

    private boolean flagOutliers(String tname, int year) throws Exception
    {
        int y1 = year;
        int y2 = year + 1;

        if (Util.False)
        {
            if (y1 == 1905 || y2 == 1905)
                return false;
        }

        if (exclude(tname, year))
            return false;

        Territory t = tds.get(tname);
        TerritoryYear ty1 = t.territoryYearOrNull(y1);
        TerritoryYear ty2 = t.territoryYearOrNull(y2);
        if (ty1 == null || ty2 == null)
            return false;

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
            return false;

        if (v1 < 0 || v2 < 0)
            throw new Exception("Negative value");

        long vmin = Math.min(v1, v2);
        long vmax = Math.max(v1, v2);

        if (vmin == 0)
            throw new Exception("Zero");

        double delta = (double) (vmax - vmin) / vmin;

        double suggested1 = 0;
        double suggested2 = 0;

        if (what.equals("births"))
        {
            suggested1 = denull(ty1.cbr);
            suggested2 = denull(ty2.cbr);
        }
        else if (what.equals("deaths"))
        {
            suggested1 = denull(ty1.cdr);
            suggested2 = denull(ty2.cdr);
        }

        /*
         * Уровни округлены до 1-го десятичного знака
         */
        if (suggested1 > 0)
            suggested1 += 0.045;

        if (suggested2 > 0)
            suggested2 += 0.045;

        suggested1 *= denull(ty1.population.total.both);
        suggested2 *= denull(ty2.population.total.both);

        suggested1 /= 1000.0;
        suggested2 /= 1000.0;

        if (delta > threshold)
        {
            Util.out(String.format("Расхождение %s для %d -> %d %s: %,d -> %,d, delta = %,d (%.1f%%), suggested: %,d %,d",
                                   what, y1, y2, tname, v1, v2, Math.abs(v1 - v2), delta * 100,
                                   Math.round(suggested1), Math.round(suggested2)));
            
            if (ShowCompositeDivergences)
                showCompositeDivergences(tname, year);
            
            return true;
        }
        else
        {
            return false;
        }
    }

    private double denull(Double v)
    {
        return (v == null) ? 0 : v;
    }

    private long denull(Long v)
    {
        return (v == null) ? 0L : v;
    }

    private boolean exclude(String tname, int year) throws Exception
    {
        switch (tname)
        {
        case "Елисаветпольская":
        case "Бакинская с Баку":
        case "Кутаисская с Батумской":
        case "Самаркандская обл.":
        case "Семипалатинская обл.":
        case "Уральская обл.":
            return true;
        }

        if (exclude(tname, year, "Закаспийская обл.", 1911, 1913))
            return true;

        if (exclude(tname, year, "Семиреченская обл.", 1912, 1914))
            return true;

        if (exclude(tname, year, "Сыр-Дарьинская обл.", 1908))
            return true;

        if (exclude(tname, year, "Ферганская обл.", 1912))
            return true;

        if (exclude(tname, year, "Тифлисская", 1903, 1914))
            return true;

        return false;
    }

    private boolean exclude(String tname, int year, String ex_tname, int ex_y) throws Exception
    {
        return exclude(tname, year, ex_tname, ex_y, ex_y);
    }

    private boolean exclude(String tname, int year, String ex_tname, int ex_y1, int ex_y2) throws Exception
    {
        if (tname.equals(ex_tname))
        {
            int y1 = year;
            int y2 = year + 1;

            if (!(y1 >= ex_y1 && y1 <= ex_y2) && !(y2 >= ex_y1 && y2 <= ex_y2))
                return true;
        }

        return false;
    }

    private void showCompositeDivergences(String tname, int year) throws Exception
    {
        for (String cname : Taxon.CompositeTaxons)
        {
            Territory ct = tds.get(cname);
            if (ct != null)
            {
                showCompositeDivergencesForYear(ct, tname, year);
                showCompositeDivergencesForYear(ct, tname, year + 1);
            }
        }
    }

    private void showCompositeDivergencesForYear(Territory ct, String tname, int year) throws Exception
    {
        TerritoryYear cty = ct.territoryYearOrNull(year);
        if (cty == null)
            return;

        Taxon tx = Taxon.of(ct.name, year, tds);
        if (tx == null)
            return;

        tx = tx.flatten(tds, year);
        if (!tx.territories.containsKey(tname))
            return;

        Territory mt = MergeTaxon.mergeTaxon(tds, ct.name, WhichYears.TaxonExistingYears);

        TerritoryYear mty = mt.territoryYearOrNull(year);
        if (mty == null)
            return;

        long listed = 0;
        long merged = 0;

        if (what.equals("births"))
        {
            if (cty.births.total.both == null)
                return;
            if (mty.births.total.both == null)
                return;

            listed = cty.births.total.both;
            merged = mty.births.total.both;
        }

        if (what.equals("deaths"))
        {
            if (cty.deaths.total.both == null)
                return;
            if (mty.deaths.total.both == null)
                return;
            listed = cty.deaths.total.both;
            merged = mty.deaths.total.both;
        }

        Util.out(String.format("    part of %s, для %d listed: %,d merged: %,d, merged minus listed: %,d",
                               ct.name, year, listed, merged, merged - listed));
    }
}
