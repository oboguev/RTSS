package rtss.pre1917.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import rtss.util.Util;

public class CensusCategories  
{
    private Map<String, CensusCategoryValues> tname2value = new HashMap<>();
    private boolean readonly = false;
    
    public void add(String tname, CensusCategoryValues value) throws Exception
    {
        checkWritable();
        if (tname2value.containsKey(tname))
            throw new Exception("Duplicate value: " + tname);
        tname2value.put(tname, value);
    }
    
    public CensusCategoryValues get(String tname)
    {
        return get(tname, true);
    }
    
    public CensusCategoryValues get(String tname, boolean verbose)
    {
        CensusCategoryValues  cv = tname2value.get(tname);
        if (cv == null && verbose)
            Util.err("CensusCategories: no value for " + tname);
        return cv;
    }
    
    public void seal()
    {
        readonly = true;
    }
    
    private void checkWritable() throws Exception
    {
        if (readonly)
            throw new Exception("CensusCategories is readonly");
    }
    
    public Set<String> keySet()
    {
        return Collections.unmodifiableSet(tname2value.keySet()); 
    }
}
