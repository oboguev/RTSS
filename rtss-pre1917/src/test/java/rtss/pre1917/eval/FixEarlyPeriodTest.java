package rtss.pre1917.eval;

import java.util.HashSet;
import java.util.Set;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.tools.ShowAreaValues;
import rtss.util.Util;

public class FixEarlyPeriodTest
{
    public static void main(String[] args)
    {
        try
        {
            new FixEarlyPeriodTest().test("Приморская обл.", 1902, 1899);
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    protected final TerritoryDataSet tdsUGVI;
    protected final TerritoryDataSet tdsCensus1897;

    private FixEarlyPeriodTest(LoadOptions... options) throws Exception
    {
        Set<LoadOptions> xo = Set.of(options);

        tdsUGVI = new LoadData().loadUGVI(unite(xo,
                                                LoadOptions.DONT_VERIFY,
                                                LoadOptions.MERGE_CITIES,
                                                LoadOptions.EVAL_PROGRESSIVE,
                                                LoadOptions.ADJUST_FEMALE_BIRTHS,
                                                LoadOptions.FILL_MISSING_BD));
        tdsCensus1897 = new LoadData().loadCensus1897(unite(xo,
                                                            LoadOptions.DONT_VERIFY,
                                                            LoadOptions.MERGE_CITIES));
    }

    private Set<LoadOptions> unite(Set<LoadOptions> xo, LoadOptions... options)
    {
        xo = new HashSet<>(xo);
        for (LoadOptions opt : options)
            xo.add(opt);
        return xo;
    }

    private void test(String tname, int by, int dy) throws Exception
    {
        Territory t = tdsUGVI.get(tname);
        Territory tCensus = tdsCensus1897.get(tname);
        
        new ShowAreaValues().show(t, "BEFORE: ");
        
        Territory xt = new FixEarlyPeriod().fix(t, tCensus, by, dy);        

        new ShowAreaValues().show(xt, "AFTER: ");
    }
}
