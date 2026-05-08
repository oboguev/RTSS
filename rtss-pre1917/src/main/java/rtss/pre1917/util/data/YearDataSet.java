package rtss.pre1917.util.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtss.pre1917.data.Territory;

public class YearDataSet
{
    private Map<Integer, YearData> yds = new HashMap<>();

    public YearDataSet(Territory t) throws Exception
    {
        this(t, t.minYear(0), t.maxYear(0));
    }

    public YearDataSet(Territory t, int minYear) throws Exception
    {
        this(t, minYear, t.maxYear(0));
    }

    public YearDataSet(Territory t, int minYear, int maxYear) throws Exception
    {
        for (int year = minYear; year <= maxYear; year++)
        {
            yds.put(year, new YearData(t.territoryYearOrNull(year)));
        }
    }

    public YearData forYear(int year)
    {
        return yds.get(year);
    }

    public List<Integer> years()
    {
        List<Integer> list = new ArrayList<>(yds.keySet());
        Collections.sort(list);
        return list;
    }

    public int minYear()
    {
        return years().get(0);
    }

    public int maxYear()
    {
        List<Integer> list = years();
        return list.get(list.size() - 1);
    }

    public YearData nextPartialYear()
    {
        return forYear(maxYear()).nextPartialYear();
    }

    public YearDataSummary getSummary(int y1, int y2)
    {
        YearDataSummary summary = new YearDataSummary();

        for (int year = y1; year <= y2; year++)
        {
            summary.add(forYear(year));
        }

        return summary.getSummary();
    }
}
