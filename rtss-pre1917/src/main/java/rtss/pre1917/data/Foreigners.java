package rtss.pre1917.data;

import java.util.HashMap;
import java.util.Map;

public class Foreigners
{
    /* indexed by country name */
    private Map<String, ByTerritory> contry2data = new HashMap<>();
    
    /* number of this country foreigners, by territory*/
    static public class ByTerritory extends HashMap<String,Long>
    {
        private static final long serialVersionUID = 1L;
        
        @Override
        public Long put(String name, Long value) throws IllegalArgumentException 
        {
            if (containsKey(name))
                throw new IllegalArgumentException("Duplicate value for " + name);
            return super.put(name, value);
        }
    }
    
    public ByTerritory forCountry(String countryName)
    {
        switch (countryName)
        {
        case "Австровенгрия":
        case "Австро-Венгрия":
            countryName = "Австрия";
            break;

        case "Англия":
        case "Великобритания":
            countryName = "Британия";
            break;

        case "Иран":
            countryName = "Персия";
            break;
        }
        
        ByTerritory v = contry2data.get(countryName);
        
        if (v == null)
        {
            v = new ByTerritory();
            contry2data.put(countryName, v);
        }
        
        return v;
    }
    
    public long getForForeignContryAndTerritory(String countryName, String tname)
    {
        ByTerritory v = forCountry(countryName);
        // ### map name
        return v.get(tname);
    }
}
