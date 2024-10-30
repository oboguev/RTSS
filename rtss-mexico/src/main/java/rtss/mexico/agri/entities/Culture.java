package rtss.mexico.agri.entities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtss.util.Util;

/*
 * Сельскохозяйственная культура
 */
public class Culture implements Comparable<Culture>
{
    public final String name;
    public final String category;
    
    private final Map<Integer, CultureYear> y2cy = new HashMap<>();
    
    public final Map<String , CultureYear> averageCultureYears = new HashMap<>();

    public Culture(String name, String category) throws Exception
    {
        this.name = name;
        this.category = category;
    }
    
    public List<Integer> years()
    {
        return Util.sort(y2cy.keySet());
    }
    
    public CultureYear cultureYear(int year)
    {
        return y2cy.get(year); 
    }

    public CultureYear makeCultureYear(int year) throws Exception
    {
        if (y2cy.containsKey(year))
            throw new Exception("Duplicate year " + year + " for " + name);
        CultureYear cy = new CultureYear(this, year);
        y2cy.put(year, cy);
        return cy; 
    }

    public int compareTo(Culture o)
    {
        return this.name.compareTo(o.name);
    }
}
