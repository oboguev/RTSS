package rtss.pre1917.data.migration;

import java.util.HashMap;
import java.util.Map;

public class ImmigrationYear
{
    public int year;

    private Map<String,Long> country2amount = new HashMap<>();
    
    public void add(String country, long amount)
    {
        Long v = country2amount.get(country);
        
        if (v == null)
            v = 0L;
        
        country2amount.put(country, v + amount);
    }
    
    public long get(String country)
    {
        Long v = country2amount.get(country);
        
        if (v == null)
            v = 0L;
        
        return v;
    }
}
