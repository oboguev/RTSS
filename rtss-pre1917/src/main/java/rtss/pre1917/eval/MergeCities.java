package rtss.pre1917.eval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;

public class MergeCities
{
    private TerritoryDataSet territories;

    public MergeCities(TerritoryDataSet territories)
    {
        this.territories = territories;
    }

    public void merge() throws Exception
    {
        merge("Московская с Москвой", "Московская", "г. Москва");
        merge("Санкт-Петербургская с Санкт-Петербургом", "Санкт-Петербургская", "г. Санкт-Петербург");
        merge("Варшавская с Варшавой", "Варшавская", "г. Варшава");
        merge("Херсонская с Одессой", "Херсонская", "г. Николаев", "г. Одесса");
        merge("Таврическая", null, "г. Севастополь");
        merge("Бакинская", null, "г. Баку");
        merge("Область войска Донского", null, "Ростовское и./Д град.");
    }

    private void merge(String dstname, String srcname, String... cities) throws Exception
    {
        Territory dst = territories.get(dstname);

        if (srcname == null)
        {
            merge(dst, cities);
        }
        else
        {
            Territory src = territories.get(srcname);
            if (src != null)
            {
                src = src.dup();
                merge(src, cities);
                merge(dst, src);
            }
            territories.remove(srcname);
        }
        
        for (String city : cities)
            territories.remove(city);
    }

    private void merge(Territory t, String... cities) throws Exception
    {
        for (String city : cities)
        {
            Territory ct = territories.get(city);
            if (ct != null)
                merge(t, ct);
        }
    }

    private void merge(Territory dst, Territory src) throws Exception
    {
        Set<Integer> yxs = new HashSet<>(dst.years());
        yxs.addAll(src.years());
        List<Integer> years = new ArrayList<>(yxs);
        Collections.sort(years);
        
        for (int year : years)
        {
            TerritoryYear tydst = dst.territoryYearOrNull(year);
            TerritoryYear tysrc = dst.territoryYearOrNull(year);
            if (tysrc == null)
            {
                // do nothing
            }
            else if (tydst == null)
            {
                dst.copyYear(tysrc);
            }
            else
            {
                tydst.merge(tysrc);
            }
        }
    }
}
