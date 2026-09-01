package rtss.pre1917.tools;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.util.Util;

public class EmitChartsBirthDeathCount
{
    public static void main(String[] args)
    {
        try
        {
            Util.out("Patched:");
            Util.out("");
            new EmitChartsBirthDeathCount().do_main(LoadOptions.APPLY_PATCHES);

            Util.out("");
            Util.out("Unpatched:");
            Util.out("");
            new EmitChartsBirthDeathCount().do_main(LoadOptions.DONT_APPLY_PATCHES);
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private TerritoryDataSet tdsCSK;
    private TerritoryDataSet tdsUGVI;

    private void do_main(LoadOptions patched) throws Exception
    {
        tdsCSK = new LoadData().loadEvroChast(LoadOptions.MERGE_CITIES,
                                              LoadOptions.MERGE_POST1897_REGIONS,
                                              LoadOptions.DONT_VERIFY,
                                              patched,
                                              LoadOptions.DONT_ADJUST_FEMALE_BIRTHS);

        tdsUGVI = new LoadData().loadUGVI(LoadOptions.MERGE_CITIES,
                                          LoadOptions.MERGE_POST1897_REGIONS,
                                          LoadOptions.DONT_VERIFY,
                                          patched,
                                          LoadOptions.DONT_ADJUST_FEMALE_BIRTHS,
                                          LoadOptions.DONT_FILL_MISSING_BD,
                                          LoadOptions.DONT_EVAL_SPLIT_ASTRAKHAN,
                                          LoadOptions.EVAL_MERGE_ASTRAKHAN,
                                          LoadOptions.DONT_EVAL_PROGRESSIVE);

        for (String tname : Util.sort(tdsCSK.keySet()))
        {
            if (Taxon.isComposite(tname))
                continue;
            Territory tCSK = tdsCSK.get(tname);
            Territory tUGVI = tdsUGVI.get(tname);
            if (tUGVI == null)
                continue;

            for (int year : tCSK.years())
            {
                Long birthsUGVI = null;
                Long deathsUGVI = null;
                Long birthsCSK = null;
                Long deathsCSK = null;

                TerritoryYear tyCSK = tCSK.territoryYearOrNull(year);
                TerritoryYear tyUGVI = tUGVI.territoryYearOrNull(year);
                if (tyCSK != null)
                {
                    birthsCSK = tyCSK.births.total.both;
                    deathsCSK = tyCSK.deaths.total.both;
                }
                if (tyUGVI != null)
                {
                    birthsUGVI = tyUGVI.births.total.both;
                    deathsUGVI = tyUGVI.deaths.total.both;
                }

                if (birthsUGVI == null && birthsCSK != null && year != 1880)
                    Util.out(String.format("Births %s %d = %,d", tname, year, birthsCSK));

                if (deathsUGVI == null && deathsCSK != null && year != 1880)
                    Util.out(String.format("Deaths %s %d = %,d", tname, year, deathsCSK));
            }
        }
    }
}
