package rtss.pre1917.tools.show;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.util.Util;

public class ShowAllAreasValuesRaw_CSK50
{
    public static void main(String[] args)
    {
        Util.out("Сырые данные изданий ЦСК без коррекции по 50 европейским губерниям, ");
        Util.out("включая в их состав крупные города,");
        Util.out("и без слияния губерний и областей образованных после 1897 года с губерниями, из которых они были выделены.");
        Util.out("");
        Util.out("Структура строки:");
        Util.out("");
        Util.out("- год");
        Util.out("- численность населения на начало года по ЦСК");
        Util.out("- численность населения на середину года по ЦСК");
        Util.out("- число рождений (по ЦСК)");
        Util.out("- число смертей (по ЦСК)");

        try
        {
            new ShowAllAreasValuesRaw_CSK50().do_main();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private TerritoryDataSet tdsCSK;
    private static final char nbsp = '\u00A0';

    private void do_main() throws Exception
    {
        tdsCSK = new LoadData().loadEvroChast(LoadOptions.APPLY_PATCHES,
                                              LoadOptions.DONT_MERGE_CITIES,
                                              LoadOptions.DONT_MERGE_POST1897_REGIONS,
                                              LoadOptions.DONT_ADJUST_FEMALE_BIRTHS,
                                              LoadOptions.DONT_VERIFY);

        for (String tname : Util.sort(tdsCSK.keySet()))
        {
            if (Taxon.isComposite(tname))
                continue;
            Territory t = tdsCSK.get(tname);

            Util.out("");
            Util.out(tname);
            Util.out("");
            Util.out("год   чж-нач    чж-сер     чр      чс");
            Util.out("==== ========= ========= ======= =======");

            for (int year : t.years())
            {
                TerritoryYear ty = t.territoryYearOrNull(year);
                Util.out(String.format("%4d %s %s %s %s",
                                       year,
                                       i2s(ty.population.total.both, 9),
                                       i2s(ty.midyear_population.total.both, 9),
                                       i2s(ty.births.total.both, 7),
                                       i2s(ty.deaths.total.both, 7)));
            }
        }
    }

    private String i2s(Long v, int width)
    {
        String s = "";
        if (v != null)
            s = String.format("%,d", v);
        while (s.length() < width)
            s = nbsp + s;
        return s;
    }
}
