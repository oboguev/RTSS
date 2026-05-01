package rtss.pre1917.war;

import java.util.HashMap;
import java.util.Map;

public class WarLossShare
{
    private Map<String, Double> tn2pct = new HashMap<>();
    
    public void set(String tname, double pct)
    {
        if (tn2pct.containsKey(tname))
            throw new IllegalArgumentException("Duplicate value for " + tname);
        tn2pct.put(tname, pct);
    }
}
