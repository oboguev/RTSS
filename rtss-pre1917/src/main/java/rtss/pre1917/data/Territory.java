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
        ty = ty.dup(this);
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
            ty = ty.dup(t);
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

    public void adjustFemaleBirths()
    {
        for (TerritoryYear ty : year2value.values())
            ty.adjustFemaleBirths();
    }

    public void leaveOnlyTotalBoth()
    {
        for (TerritoryYear ty : year2value.values())
            ty.leaveOnlyTotalBoth();
    }
    
    /*
     * Изменить progressive_population.total.both начиная с года (@year + 1) и во все последующие годы на величину @delta.
     * Соответствует дополнительному приросту (или потерям) населения за год @year. 
     */
    public void cascadeAdjustProgressivePopulation(int year, long delta)
    {
        for (int y : years())
        {
            TerritoryYear ty = this.territoryYearOrNull(y);
            if (ty != null && y >= year + 1 && ty.progressive_population.total.both != null)
                ty.progressive_population.total.both += delta;
        }
    }
}
