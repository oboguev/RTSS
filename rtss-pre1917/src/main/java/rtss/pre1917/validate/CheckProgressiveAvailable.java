package rtss.pre1917.validate;

import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.util.Util;

/*
 * Проверить, для каких областей (и лет) прогрессивное исчисление не вычислено
 */
public class CheckProgressiveAvailable
{
    private final TerritoryDataSet tds;
    
    public CheckProgressiveAvailable(TerritoryDataSet tds)
    {
        this.tds = tds;
    }
    
    public void check() throws Exception
    {
        check(-1);
    }
    
    public void check(int toYear) throws Exception
    {
        for (String tname : Util.sort(tds.keySet()))
        {
            if (Taxon.isComposite(tname))
                continue; 
            
            for (int year = 1896; year <= 1915; year++)
            {
                if (toYear > 0 && year > toYear)
                    break;
                
                if (!available(tname, year))
                    Util.err(String.format("Прогрессивный расчёт отсутствет для %s %d", tname, year));
            }
        }
    }
    
    private boolean available(String tname, int year)
    {
        Territory t = tds.get(tname);
        if (t == null)
            return false;
        
        TerritoryYear ty = t.territoryYearOrNull(year);
        if (ty == null)
            return false;
        
        if (ty.progressive_population.total.both == null)
            return false;
        if (ty.progressive_population.total.both == 0)
            return false;
        
        return true;
    }
}
