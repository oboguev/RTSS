package rtss.pre1917.data;

import java.util.HashMap;
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
    
    public boolean hasYear(int year)
    {
        return year2value.containsKey(year);
    }

    public Territory dup()
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
}
