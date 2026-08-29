package rtss.pre1917.tools;

import rtss.data.selectors.BirthDeath;
import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.util.Util;

/*
 * Сравнить числа рождений и смертей по УГВИ и по ЦСК
 */
public class FlagUGVIvsCSK
{
    public static void main(String[] args)
    {
        try
        {
            Util.out("Сравнение числа рождений и смертей по УГВИ и по ЦСК");
            Util.out("");
            new FlagUGVIvsCSK().flagUnder(BirthDeath.BIRTH);
            Util.out("");
            new FlagUGVIvsCSK().flagUnder(BirthDeath.DEATH);

            Util.out("");
            Util.out("** Done");
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private TerritoryDataSet tdsCSK;
    private TerritoryDataSet tdsUGVI;

    private void flagUnder(BirthDeath bd) throws Exception
    {
        tdsCSK = new LoadData().loadEvroChast(LoadOptions.MERGE_CITIES,
                                              LoadOptions.MERGE_POST1897_REGIONS,
                                              LoadOptions.DONT_ADJUST_FEMALE_BIRTHS,
                                              LoadOptions.DONT_VERIFY);

        tdsUGVI = new LoadData().loadUGVI(LoadOptions.MERGE_CITIES,
                                          LoadOptions.MERGE_POST1897_REGIONS,
                                          LoadOptions.DONT_VERIFY,
                                          LoadOptions.APPLY_PATCHES,
                                          LoadOptions.DONT_ADJUST_FEMALE_BIRTHS,
                                          LoadOptions.DONT_FILL_MISSING_BD,
                                          LoadOptions.DONT_EVAL_SPLIT_ASTRAKHAN,
                                          LoadOptions.EVAL_MERGE_ASTRAKHAN,
                                          LoadOptions.DONT_EVAL_PROGRESSIVE);

        for (String tname : tdsCSK.keySet())
        {
            if (Taxon.isComposite(tname))
                continue;

            flagUnder(tname, bd, 1881, 1914);
        }
    }

    private void flagUnder(String tname, BirthDeath bd, int y1, int y2) throws Exception
    {
        for (int year = y1; year <= y2; year++)
        {
            TerritoryYear tyCSK = tdsCSK.get(tname).territoryYearOrNull(year);
            TerritoryYear tyUGVI = tdsUGVI.get(tname).territoryYearOrNull(year);

            Long vCSK = null;
            Long vUGVI = null;

            switch (bd)
            {
            case BIRTH:
                vCSK = tyCSK.births.total.both;
                vUGVI = tyUGVI.births.total.both;
                break;

            case DEATH:
                vCSK = tyCSK.deaths.total.both;
                vUGVI = tyUGVI.deaths.total.both;
                break;
            }
            
            if (vUGVI == null && year == 1914)
                continue;

            if (vCSK == null || vUGVI == null)
                throw new Exception("Null data for " + tname + " " + year + " " + bd.toString());

            if (Util.differ(vCSK, vUGVI, 0.02))
            {
                double pct = 100.0 * (vUGVI - vCSK) / (double) vCSK;
                Util.out(String.format("Расхождение УГВИ vs CSK %s %d %s %,d %,d (%.1f%%)", tname, year, bd.name(), vUGVI, vCSK, pct));
            }
        }
    }
}
