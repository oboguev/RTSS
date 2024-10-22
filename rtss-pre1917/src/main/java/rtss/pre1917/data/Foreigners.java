package rtss.pre1917.data;

import java.util.HashMap;
import java.util.Map;

import rtss.pre1917.merge.MergeCities;


/*
 * Число и расселение иностранных подданных по переписи 1897 года.
 * 
 *        держава -> (губерния -> число)
 */
public class Foreigners
{
    /* indexed by country name */
    private Map<String, ByTerritory> contry2data = new HashMap<>();
    private boolean readonly = false;
    
    /* number of this country foreigners, by territory*/
    static public class ByTerritory extends HashMap<String,Long>
    {
        private static final long serialVersionUID = 1L;
        private final Foreigners foreigners;
        
        public ByTerritory(Foreigners foreigners)
        {
            this.foreigners = foreigners;
        }
        
        @Override
        public Long put(String name, Long value) throws IllegalArgumentException 
        {
            if (containsKey(name))
                throw new IllegalArgumentException("Duplicate value for " + name);
            
            if (foreigners.readonly)
                throw new IllegalStateException("Object is sealed as readonly");
            
            return super.put(name, value);
        }
        
        private void removeCities()
        {
            MergeCities.removeCities(this);
        }
        
        public long total()
        {
            long v = 0;
            for (String tname : keySet())
                v += get(tname);
            return v;
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
            v = new ByTerritory(this);
            contry2data.put(countryName, v);
        }
        
        return v;
    }
    
    /*
     * Общее число иностранцев подданства @countryName в России
     */
    public long totalForForeignContry(String countryName)
    {
        return forCountry(countryName).total();
    }

    /*
     * Число иностранцев подданства @countryName в губернии или области @tname
     */
    public long forForeignContryAndTerritory(String countryName, String tname)
    {
        ByTerritory byt = forCountry(countryName);
        Long v = byt.get(tname);
        if (v == null)
        {
            String pname = MergeCities.combined2parent(tname);
            v = byt.get(pname);
        }
        
        if (v == null)
            v = 0L;

        return v;
    }
    
    public void build()
    {
        // убрать города
        for (ByTerritory byt : contry2data.values())
            byt.removeCities();
        
        readonly = true;
    }
}