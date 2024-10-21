package rtss.pre1917.data;

import java.util.HashMap;
import java.util.Map;

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
    
    public boolean containsKey(String tname)
    {
        return tname2value.containsKey(tname);
    }
    
    public CensusCategoryValues get(String tname)
    {
        return tname2value.get(tname);
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
}
