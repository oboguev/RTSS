package rtss.pre1917.tools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.calc.EvalCountryTaxon;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.util.Util;

public class Ours_vs_CSK_vs_UGVI
{
    public static void main(String[] args)
    {
        try
        {
            TerritoryDataSet tdsCSK = new LoadData().loadEzhegodnikRossii(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES,
                                                                          LoadOptions.MERGE_POST1897_REGIONS);
            TerritoryDataSet tdsUGVI = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES, LoadOptions.MERGE_POST1897_REGIONS);
            TerritoryDataSet tdsOurs = EvalCountryTaxon.getFinalEmpirePopulationSet();

            new Ours_vs_CSK_vs_UGVI().printDifference("ЦСК минус наша", tdsCSK, tdsOurs);
            new Ours_vs_CSK_vs_UGVI().printDifference("УГВИ минус наша", tdsUGVI, tdsOurs);
        }
        catch (Exception ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private void printDifference(String title, TerritoryDataSet tds, TerritoryDataSet tdsOurs)
    {
        final int year = 1913;

        Util.out("");
        Util.out("**********************************************************");
        Util.out("");
        Util.out(title);
        Util.out("");

        List<TerritoryDifference> diffs = new ArrayList<>();

        for (String tname : tdsOurs.keySet())
        {
            TerritoryYear ty = tds.territoryYearOrNull(tname, year);
            TerritoryYear tyOurs = tdsOurs.territoryYearOrNull(tname, year);

            long p = ty.population.total.both;
            long pOurs = tyOurs.progressive_population.total.both;

            TerritoryDifference td = new TerritoryDifference();
            diffs.add(td);

            td.name = tname;
            td.diff = p - pOurs;
            td.pct = (100.0 * td.diff) / pOurs;
        }

        diffs.sort(Comparator.comparingLong((TerritoryDifference td) -> td.diff).reversed());

        for (TerritoryDifference td : diffs)
        {
            Util.out(String.format("\"%s\" %,d %f", td.name, td.diff, td.pct));
        }
    }

    public static class TerritoryDifference
    {
        public String name;
        public long diff;
        public double pct;
    }
}
