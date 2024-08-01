package rtss.pre1917.data;

import java.util.HashMap;
import java.util.Map;

public class Territory
{
    public String name;
    public Map<Integer, TerritoryYear> year2value = new HashMap<>();
    
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
}
