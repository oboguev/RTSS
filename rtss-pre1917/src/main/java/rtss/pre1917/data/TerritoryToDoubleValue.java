package rtss.pre1917.data;

import java.util.Map;

import rtss.pre1917.merge.MergeCities;
import rtss.pre1917.merge.MergeDescriptor;
import rtss.pre1917.merge.MergePost1897Regions;
import rtss.util.Util;

public class TerritoryToDoubleValue
{
    private final String what;
    private final Map<String, Double> values;
    
    public TerritoryToDoubleValue(String what, Map<String, Double> values)
    {
        this.what = what;
        this.values = values;
        Util.unused(this.what);
    }
    
    public Double getValue(String tname)
    {
        if (tname.equals(Taxon.Астраханская_кочевники))
            return 0.0;

        if (tname.equals(Taxon.Астраханская_оседлое))
            tname = "Астраханская";
        
        Double v = values.get(tname);
        if (v != null)
            return v;
        
        for (MergeDescriptor md : MergeCities.MergeCitiesDescriptors)
        {
            if (tname.equals(md.combined) && md.parent != null)
            {
                v = values.get(md.parent);
                if (v != null)
                    return v;
            }
            
            for (String child : md.children)
            {
                if (child != null && tname.equals(child))
                {
                    v = values.get(md.parent);
                    if (v != null)
                        return v;
                }
            }
        }
        
        for (MergeDescriptor md : MergePost1897Regions.MergePost1897Descriptors)
        {
            if (tname.equals(md.combined) && md.parent != null)
            {
                v = values.get(md.parent);
                if (v != null)
                    return v;
            }
            
            for (String child : md.children)
            {
                if (child != null && tname.equals(child))
                {
                    v = values.get(md.parent);
                    if (v != null)
                        return v;
                }
            }
        }
        
        return null;
    }
}
