package rtss.pre1917.calc;

import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryNames;
import rtss.pre1917.data.TerritoryYear;

public class AdjustTerritories
{
    private final TerritoryDataSet tds;
    private TerritoryDataSet tdsCSK;

    public AdjustTerritories(TerritoryDataSet tds)
    {
        this.tds = tds;
    }

    public AdjustTerritories setCSK(TerritoryDataSet tdsCSK)
    {
        this.tdsCSK = tdsCSK;
        return this;
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

    /*
     * Исправление для Самаркандской области.
     * Использовать расчёт УГВИ (1896-1901), затем ЦСК (1904-1914).
     * Численность в 1902 и 1903 г. интерполировать между 1901 и 1904. 
     */
    public void fixSamarkand() throws Exception
    {
        final String tname = "Самаркандская обл.";
        TerritoryNames.checkValidTerritoryName(tname);

        Territory t = tds.get(tname);
        if (t == null)
            return;
        
        Territory tCSK = tdsCSK.get(tname);

        for (int year = 1896; year <= 1901; year++)
        {
            TerritoryYear ty = t.territoryYearOrNull(year);
            ty.progressive_population.total.both = ty.population.total.both;
        }

        for (int year = 1904; year <= 1914; year++)
        {
            TerritoryYear ty = t.territoryYearOrNull(year);
            TerritoryYear tyCSK = tCSK.territoryYearOrNull(year);
            ty.progressive_population.total.both = tyCSK.population.total.both;
        }

        interpolate_progressive_population(t, 1901, 1904);
    }

    /*
     * Исправление для Уральской области.
     * Использовать прогрессивный расчёт (1896-1903), затем расчёт ЦСК (1905-1914), среднее для 1904.
     */
    public void fixUralskaia() throws Exception
    {
        final String tname = "Уральская обл.";
        TerritoryNames.checkValidTerritoryName(tname);

        Territory t = tds.get(tname);
        if (t == null)
            return;

        Territory tCSK = tdsCSK.get(tname);

        for (int year = 1904; year <= 1914; year++)
        {
            TerritoryYear ty = t.territoryYearOrNull(year);
            TerritoryYear tyCSK = tCSK.territoryYearOrNull(year);
            if (year == 1904)
            {
                ty.progressive_population.total.both = (tyCSK.population.total.both + ty.progressive_population.total.both) / 2;
            }
            else
            {
                ty.progressive_population.total.both = tyCSK.population.total.both;
            }
        }
    }
    
    /*
     * Исправление для Бакинской губернии с Баку.
     * 
     * 1. Предварительно устранить в оценке УГВИ осцилляции путём экспоненциального интерполирования (с постоянным годовым темпом роста) 
     *    численности населения между значениями 1903 и 1914 годов.
     *    
     * 2. Погодовое усреднение двух оценок: прогрессивной и УГВИ.
     */
    public void fixBakinskaiaWithBaku() throws Exception
    {
        final String tname = "Бакинская с Баку";
        TerritoryNames.checkValidTerritoryName(tname);

        Territory t = tds.get(tname);
        if (t == null)
            return;
        
        interpolate_population(t, 1903, 1914);
        
        for (int year = 1896; year <= 1914; year++)
        {
            TerritoryYear ty = t.territoryYearOrNull(year);
            ty.progressive_population.total.both = (ty.population.total.both + ty.progressive_population.total.both) / 2;
        }
    }

    /* ===================================================================================== */

    private void interpolate_progressive_population(Territory t, int y1, int y2)
    {
        int nyears = y2 - y1;

        TerritoryYear ty1 = t.territoryYearOrNull(y1);
        TerritoryYear ty2 = t.territoryYearOrNull(y2);

        double a = ty2.progressive_population.total.both;
        a /= ty1.progressive_population.total.both;

        a = Math.pow(a, 1.0 / nyears);
        double pop = ty1.progressive_population.total.both;

        for (int y = y1 + 1; y < y2; y++)
        {
            pop *= a;
            TerritoryYear ty = t.territoryYearOrNull(y);
            ty.progressive_population.total.both = Math.round(pop);
        }
    }

    private void interpolate_population(Territory t, int y1, int y2)
    {
        int nyears = y2 - y1;

        TerritoryYear ty1 = t.territoryYearOrNull(y1);
        TerritoryYear ty2 = t.territoryYearOrNull(y2);

        double a = ty2.population.total.both;
        a /= ty1.population.total.both;

        a = Math.pow(a, 1.0 / nyears);
        double pop = ty1.population.total.both;

        for (int y = y1 + 1; y < y2; y++)
        {
            pop *= a;
            TerritoryYear ty = t.territoryYearOrNull(y);
            ty.population.total.both = Math.round(pop);
        }
    }
}
