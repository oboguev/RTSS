package rtss.pre1917.war;

import java.util.HashMap;
import java.util.Map;

import rtss.pre1917.data.Taxon;
import rtss.pre1917.merge.MergeCities;
import rtss.pre1917.merge.MergeDescriptor;
import rtss.pre1917.merge.MergePost1897Regions;
import rtss.util.Util;

public class WarLossShare
{
    private Map<String, Double> tn2pct = new HashMap<>();

    public void set(String tname, double pct)
    {
        if (tn2pct.containsKey(tname))
            throw new IllegalArgumentException("Duplicate value for " + tname);
        tn2pct.put(tname, pct);
    }

    public Double getLossPercentageVsEmpireForTeritory(String tname)
    {
        if (tname.equals(Taxon.Астраханская_кочевники))
            return 0.0;
            
        if (tname.equals(Taxon.Астраханская_оседлое))
            tname = "Астраханская";

        Double pct = tn2pct.get(tname);
        if (pct != null)
            return pct;

        for (MergeDescriptor md : MergeCities.MergeCitiesDescriptors)
        {
            if (tname.equals(md.combined))
                return tn2pct.get(md.parent);
        }

        for (MergeDescriptor md : MergePost1897Regions.MergePost1897Descriptors)
        {
            if (tname.equals(md.combined))
            {
                Double sum = null;

                for (String child : md.parentWithChildren())
                {
                    Double p = tn2pct.get(child);
                    if (p != null)
                    {
                        if (sum == null)
                            sum = p;
                        else
                            sum += p;
                    }
                }

                if (sum != null)
                    return sum;
            }
        }

        switch (tname)
        {
        case "Сахалин":
        case "Выборгская":
            return 0.0;
            // return null;
        }

        Util.err("No war loss data for " + tname);

        return null;
    }
}
