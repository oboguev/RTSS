package rtss.pre1917.data.migration;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.CensusCategories;
import rtss.pre1917.data.Foreigners;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.merge.MergeDescriptor;
import rtss.pre1917.merge.MergePost1897Regions;
import rtss.util.Util;

/*
 * Иммиграция в Россию извне границ России
 */
public class Immigration
{
    /* ================================== FETCH DATA ================================== */

    /*
     * Число иммигрантов извне границ России поселившихся в губернии или области @tname в год @year 
     */
    public long immigrants(String tname, int year) throws Exception
    {
        String key = key(tname, year);

        Double v = tname2amount.get(key);

        if (v == null)
        {
            v = 0.0;

            MergeDescriptor md = MergePost1897Regions.find(tname);

            if (md != null)
            {
                for (String xtn : md.parentWithChildren())
                    v += immigrants(xtn, year);
            }
            else if (union("Холмская", "Сахалин", "Камчатская", "Батумская").contains(tname))
            {
                // leave zero
            }
            else
            {
                throw new Exception(String.format("Нет данных об имиграции в %s в %d году", tname, year));
            }
        }

        return Math.round(v);
    }

    /* ================================== INNER DATA ================================== */

    /* количество эмигрантов для губернии и года */
    private Map<String, Double> tname2amount = new HashMap<>();

    private boolean sealed = false;

    private String key(String tname, int year)
    {
        return year + " @ " + tname;
    }

    private void addAmount(String tname, int year, double value)
    {
        String key = key(tname, year);
        Double v = tname2amount.get(key);
        if (v == null)
            v = 0.0;
        tname2amount.put(key, v + value);
    }

    private void checkWritable() throws Exception
    {
        if (sealed)
            throw new Exception("Emigration instance is readonly");
    }

    /* ================================== CONSTRUCTION ================================== */

    private Map<Integer, ImmigrationYear> y2yd = new HashMap<>();

    public void setYearData(ImmigrationYear yd) throws Exception
    {
        checkWritable();

        if (y2yd.containsKey(yd.year))
            throw new Exception("Duplicate year");
        y2yd.put(yd.year, yd);

    }

    private TerritoryDataSet tdsCensus;
    private CensusCategories censusCategories;
    private Foreigners foreigners;

    public void build() throws Exception
    {
        checkWritable();

        tdsCensus = new LoadData().loadCensus1897(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
        censusCategories = new LoadData().loadCensusCategories();
        foreigners = new LoadData().loadForeigners();

        for (int year : Util.sort(y2yd.keySet()))
        {
            ImmigrationYear yd = y2yd.get(year);
            build(yd);
            validate(yd);
        }

        sealed = true;
    }

    private void build(ImmigrationYear yd) throws Exception
    {
        // ###
    }

    private void validate(ImmigrationYear yd) throws Exception
    {
        // ###
    }

    /* ================================== UTIL ================================== */

    private Collection<String> union(Object... objects) throws Exception
    {
        Set<String> xs = new HashSet<>();

        for (Object o : objects)
        {
            if (o instanceof String)
            {
                xs.add((String) o);
            }
            else if (o instanceof Set)
            {
                for (Object o2 : (Set<?>) o)
                    xs.add((String) o2);
            }
            else
            {
                throw new IllegalArgumentException("Neither a string nor a set of strings");
            }
        }

        return xs;
    }
}
