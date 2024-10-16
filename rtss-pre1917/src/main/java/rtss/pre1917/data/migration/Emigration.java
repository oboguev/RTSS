package rtss.pre1917.data.migration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.util.Util;

public class Emigration
{
    public Set<Integer> keySet()
    {
        // ###
        return null;
    }
    
    public EmigrationYear get(int year)
    {
        // ###
        return null;
    }
    
    /* ================================================================ */
    
    /* количество эмигрантов для губернии и года */
    private Map<String,Double> tname2value = new HashMap<>();
    
    private String key(String tname, int year)
    {
        return year + " @ " + tname;
    }
    
    private void addValue(String tname, int year, double value)
    {
        String key = key(tname, year);
        Double v = tname2value.get(key);
        if (v == null)
            v = 0.0;
        tname2value.put(key, v + value);
    }
    
    /* ================================================================ */
    
    @SuppressWarnings("unused")
    private static final long serialVersionUID = 1L;
    
    private Map<Integer,EmigrationYear> y2yd = new HashMap<>();
    
    public void setYearData(EmigrationYear yd) throws Exception
    {
        if (y2yd.containsKey(yd.year))
            throw new Exception("Duplicate year");
        y2yd.put(yd.year, yd);
        
    }
    
    private TerritoryDataSet tdsCensus;
    private Map<String, Double> jews; 
    
    public void build() throws Exception
    {
        jews = new LoadData().loadJews();
        tdsCensus = new LoadData().loadCensus1897(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);

        for (int year : Util.sort(y2yd.keySet()))
        {
            EmigrationYear yd = y2yd.get(year);
            build(yd);
        }
    }

    private void build(EmigrationYear yd) throws Exception
    {
        
        Util.noop();
        // ###
    }
}
