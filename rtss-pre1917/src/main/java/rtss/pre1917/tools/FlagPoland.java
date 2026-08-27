package rtss.pre1917.tools;

import rtss.data.selectors.BirthDeath;
import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.util.Util;

/*
 * Пометить сильные уклонения в числе рождений или смертей в губернии Царства Польского
 * по УГВИ сравнительно с Трудами Варшавского Статистического комитета
 */
public class FlagPoland
{
    public static void main(String[] args)
    {
        try
        {
            Util.out("Сравненение числа рождений и смертей в губерниях Царства Польского");
            Util.out("по Варш. Стат. Комитету и по УГВИ");
            Util.out("");
            new FlagPoland().flagPoland(BirthDeath.BIRTH);
            Util.out("");
            new FlagPoland().flagPoland(BirthDeath.DEATH);

            Util.out("");
            Util.out("** Done");
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private TerritoryDataSet tds;
    private TerritoryDataSet tdsPoland;

    private void flagPoland(BirthDeath bd) throws Exception
    {
        this.tds = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY,
                                           LoadOptions.EVAL_SPLIT_ASTRAKHAN,
                                           LoadOptions.EVAL_PROGRESSIVE_ASTRAKHAN_ONLY);

        this.tdsPoland = new LoadData().loadPoland(LoadOptions.NONE);

        for (String tname : tdsPoland.keySet())
        {
            Territory tPoland = tdsPoland.get(tname);
            Territory t = tds.get(tname);

            for (int year : tPoland.years())
            {
                TerritoryYear tyPoland = tPoland.territoryYearOrNull(year);
                TerritoryYear ty = t.territoryYearOrNull(year);

                if (ty != null && tyPoland != null && bd == BirthDeath.BIRTH)
                {
                    if (ty.births.total.both != null && tyPoland.births.total.both != null && Util.differ(ty.births.total.both, tyPoland.births.total.both, 0.02))
                    {
                        long diff = ty.births.total.both - tyPoland.births.total.both;
                        double pct = 100 * diff / (double) tyPoland.births.total.both;
                        Util.out(String.format("%s %4d BIRTHS (ВСК vs. УГВИ) %,d %,d diff = %,d (%.1f%%)", tname, year, tyPoland.births.total.both, ty.births.total.both, diff, pct));
                    }
                }

                if (ty != null && tyPoland != null && bd == BirthDeath.DEATH)
                {
                    if (ty.deaths.total.both != null && tyPoland.deaths.total.both != null && Util.differ(ty.deaths.total.both, tyPoland.deaths.total.both, 0.02))
                    {
                        long diff = ty.deaths.total.both - tyPoland.deaths.total.both;
                        double pct = 100 * diff / (double) tyPoland.deaths.total.both;
                        Util.out(String.format("%s %4d DEATHS (ВСК vs. УГВИ) %,d %,d diff = %,d (%.1f%%)", tname, year, tyPoland.deaths.total.both, ty.deaths.total.both, diff, pct));
}                }
            }
        }
    }
}
