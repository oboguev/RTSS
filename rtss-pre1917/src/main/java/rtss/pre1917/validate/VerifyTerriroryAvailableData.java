package rtss.pre1917.validate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.util.Util;

/*
 * Проверить, что для территорий имеются данные, и начиная с какого года
 */
public class VerifyTerriroryAvailableData
{
    public static void main(String[] args)
    {
        try
        {
            new VerifyTerriroryAvailableData().do_main();
        }
        catch (Exception ex)
        {
            Util.err("** Exception");
            ex.printStackTrace();
        }
    }

    private void do_main() throws Exception
    {
        TerritoryDataSet tds = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY,
                                                       LoadOptions.MERGE_CITIES,
                                                       LoadOptions.MERGE_POST1897_REGIONS);
        Taxon taxon = Taxon.of("Империя", 1913, tds);
        taxon = taxon.flatten(tds, 1913);
        List<String> tnames = new ArrayList<String>(taxon.territories.keySet());
        Collections.sort(tnames);

        for (String tname : tnames)
            explore(tds, tname);
    }

    private void explore(TerritoryDataSet tds, String tname)
    {
        Territory t = tds.get(tname);

        if (t == null)
        {
            Util.out(String.format("%s missing", tname));
            return;
        }

        List<Integer> years = t.years();
        int y0 = years.get(0);
        String gaps = gaps(years);
        if (y0 == 1881 && gaps.equals("none"))
        {
            // Util.out(String.format("%s full", tname));
        }
        else if (gaps.equals("none"))
        {
            Util.out(String.format("%s from %d", tname, years.get(0)));
        }
        else 
        {
            Util.out(String.format("%s from %d gaps: %s", tname, years.get(0), gaps));
        }
    }

    private String gaps(List<Integer> years)
    {
        if (years == null || years.size() < 2)
        {
            return "none";
        }

        List<Integer> sortedYears = years.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        if (sortedYears.size() < 2)
        {
            return "none";
        }

        StringBuilder gaps = new StringBuilder();

        for (int i = 1; i < sortedYears.size(); i++)
        {
            int previous = sortedYears.get(i - 1);
            int current = sortedYears.get(i);

            for (int year = previous + 1; year < current; year++)
            {
                if (!gaps.isEmpty())
                    gaps.append(' ');

                gaps.append(year);
            }
        }

        return gaps.isEmpty() ? "none" : gaps.toString();
    }
}
