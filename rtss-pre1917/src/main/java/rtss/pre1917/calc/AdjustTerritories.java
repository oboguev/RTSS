package rtss.pre1917.calc;

import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;

public class AdjustTerritories
{
    private final TerritoryDataSet tds;
    
    public AdjustTerritories(TerritoryDataSet tds)
    {
        this.tds = tds;
    }
    
    /*
     * Исправление для Дагестана. 
     * Перебазировать оценку числености населения УГВИ от 1897 года по величине переписи (т.е. от прогрессивного расчёта на начало 1897 года), 
     * с соответствующим уменьшением величин для последующих лет,
     */
    public void fixDagestan() throws Exception
    {
       Territory t = tds.get("Дагестанская обл."); 
       TerritoryYear ty1897 = t.territoryYearOrNull(1897);
       long delta = ty1897.progressive_population.total.both - ty1897.population.total.both; 
       
       for (int year = 1896; year <= 1914; year++)
       {
           TerritoryYear ty = t.territoryYearOrNull(year);
           ty.population.total.both += delta;
       }
    }
}
