package rtss.pre1917.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Territory
{
    public final String name;
    private Map<Integer, TerritoryYear> year2value = new HashMap<>();

    public Territory(String name)
    {
        this.name = name;
    }

    public TerritoryYear territoryYear(int year)
    {
        TerritoryYear ty = year2value.get(year);

        if (ty == null)
        {
            ty = new TerritoryYear(this, year);
            year2value.put(year, ty);
        }

        return ty;
    }
    
    public TerritoryYear territoryYearOrNull(int year)
    {
        return year2value.get(year);
    }
    
    public void copyYear(TerritoryYear ty)
    {
        ty = ty.dup();
        ty.territory = this;
        year2value.put(ty.year, ty);
    }

    public boolean hasYear(int year)
    {
        return year2value.containsKey(year);
    }

    public Territory dup()
    {
        return dup(name);
    }
    
    public Territory dup(String name)
    {
        Territory t = new Territory(name);
        
        for (int year : year2value.keySet())
        {
            TerritoryYear ty = year2value.get(year);
            ty = ty.dup();
            ty.territory = t;
            t.year2value.put(year, ty);
        }
        
        return t;
    }
    
    public List<Integer> years()
    {
        List<Integer> list = new ArrayList<>(year2value.keySet());
        Collections.sort(list);
        return list;
    }
    
    public int minYear(int dflt)
    {
        int res = -1;
        
        for (int year : years())
        {
            if (res == -1)
                res = year;
            else
                res = Math.min(res, year);
        }
        
        if (res == -1)
            res = dflt;
        
        return res;
    }
    
    public int maxYear(int dflt)
    {
        int res = -1;
        
        for (int year : years())
        {
            if (res == -1)
                res = year;
            else
                res = Math.max(res, year);
        }
        
        if (res == -1)
            res = dflt;
        
        return res;
    }
    
    public String toString()
    {
        return name;
    }
}
