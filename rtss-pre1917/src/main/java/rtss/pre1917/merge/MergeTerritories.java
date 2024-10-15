package rtss.pre1917.merge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.util.Util;

public class MergeTerritories
{
    private TerritoryDataSet territories;

    public MergeTerritories(TerritoryDataSet territories)
    {
        this.territories = territories;
    }
    
    public void merge(List<MergeDescriptor> mds) throws Exception
    {
        for (MergeDescriptor md : mds)
            merge(md.combined, md.parent, md.childrenAsArray());
    }

    /*
     * Если @srcname == null, то сливается с существующим @dstname.
     */
    private void merge(String dstname, String srcname, String... cities) throws Exception
    {
        if (srcname == null)
        {
            Territory dst = territories.get(dstname);
            if (dst == null)
                territories.put(dstname, dst = new Territory(dstname));

            merge(dst, cities);
        }
        else
        {
            Territory src = territories.get(srcname);

            if (src != null)
            {
                src = src.dup(dstname);
                merge(src, cities);
                territories.put(dstname, src);
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
            TerritoryYear tysrc = src.territoryYearOrNull(year);
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
