package rtss.pre1917.data.migration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import rtss.util.Util;

/*
 * Годовые данные иммиграции в Россию: число иммигрантов по странам
 */
public class ImmigrationYear
{
    public final int year;
    public final LumpImmigration lump = new LumpImmigration();
    
    private Map<String,Long> country2amount = new HashMap<>();
    
    public ImmigrationYear(int year)
    {
        this.year = year;
    }
    
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
    
    public long get_nonneg(String country) throws Exception
    {
        long v = get(country);
        if (v < 0 && Util.False) // ###@@@
            throw new Exception("Отрицательнная иммиграция для " + country + " в " + year);
        return v;
    }
    
    public Set<String> contries()
    {
        return country2amount.keySet();
    }
    
    public static class LumpImmigration
    {
        Long european;
        
        public Long persia;
        public Long turkey;
        
        // asian
        public Long china;
        public Long japan;
        
        public long sum()
        {
            return european + persia + turkey + china + japan;
        }
    }
}
