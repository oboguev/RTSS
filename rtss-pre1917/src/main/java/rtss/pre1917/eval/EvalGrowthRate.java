package rtss.pre1917.eval;

import java.util.HashMap;
import java.util.Map;

import rtss.pre1917.data.InnerMigration;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;

/*
 * Пересчитать население за 1896-1915 гг. на основе сведений о естественом движении за промежуток, 
 * когда была достигнута удовлетворительная степень регистрации рождений и смертей, в предположении,
 * что в остальные годы уровень рождаемости и смертности был таков же, как в этом промежутке в среднем.
 */
public class EvalGrowthRate
{
    private final Map<String, Integer> tname_y1 = new HashMap<>();
    private final Map<String, Integer> tname_y2 = new HashMap<>();

    private final TerritoryDataSet tdsCensus1897;
    private final InnerMigration innerMigration;

    private final double PROMILLE = 1000.0;

    public EvalGrowthRate(TerritoryDataSet tdsCensus1897, InnerMigration innerMigration)
    {
        this.innerMigration = innerMigration;
        this.tdsCensus1897 = tdsCensus1897;
        define();
    }

    private void define()
    {
        define("Акмолинская обл.", 1910, 1914);
        define("Закаспийская обл.", 1911, 1913);
        define("Семиреченская обл.", 1912, 1914);
        define("Сыр-Дарьинская обл.", 1908);
        define("Тургайская обл.", 1911, 1914);
        define("Ферганская обл.", 1912);
        define("Терская обл.", 1912, 1914);
        define("Карсская обл.", 1907, 1913);
    }

    private void define(String tname, int year)
    {
        define(tname, year, year);
    }

    private void define(String tname, int y1, int y2)
    {
        tname_y1.put(tname, y1);
        tname_y2.put(tname, y2);
    }
    
    public boolean is_stable_year(String tname, int year)
    {
        if (tname_y1.containsKey(tname))
        {
            int y1 = tname_y1.get(tname);
            int y2 = tname_y2.get(tname);
            return (year >= y1 && year <= y2);
        }
        else
        {
            return false;
        }
    }

    /* =============================================================== */

    /*
     * Пересчитать население.
     * Если пересчёт произведён, возвращает новый объект типа Territory.
     * Иначе возвращает null. 
     */
    public Territory evalTerritory(Territory t) throws Exception
    {
        if (!tname_y1.containsKey(t.name))
            return null;

        Territory tCensus1897 = tdsCensus1897.get(t.name);
        if (tCensus1897 == null)
            throw new Exception("Missing 1897 census territory data for " + t.name);

        int y1 = tname_y1.get(t.name);
        int y2 = tname_y2.get(t.name);
        int nyears = y2 - y1 + 1;

        CalcGrowthRate cgr = new CalcGrowthRate(t, tCensus1897, innerMigration);

        double ngr = 0;
        for (int y = y1; y <= y2; y++)
        {
            double a = cgr.calcNaturalGrowthRate(y);
            ngr += a;
        }

        ngr = ngr / nyears;

        double ymult = 1 + ngr / PROMILLE;

        /* =========================================================================== */

        long p1897 = cgr.population_1897_Jan1(ngr/ PROMILLE);

        Territory xt = t.dup();
        xt.leaveOnlyTotalBoth();
        xt.territoryYear(1897).population.total.both = p1897;
        xt.territoryYear(1896).population.total.both = Math.round(p1897 / ymult);

        // движение за @year, т.е. от @year к @year + 1
        for (int year = 1897; year <= 1915; year++)
        {
            if (year >= y1 && year <= y2)
            {
                xt.territoryYear(year + 1).population.total.both = xt.territoryYear(year).population.total.both + cgr.increaseUGVI(year);
            }
            else
            {
                xt.territoryYear(year + 1).population.total.both = Math.round(ymult * xt.territoryYear(year).population.total.both);
            }
        }

        // средние уровни рождаемости и смертности в стабилизированном участке
        double cbr = 0;
        double cdr = 0;
        for (int year = y1; year <= y2; year++)
        {
            TerritoryYear ty = xt.territoryYearOrNull(year);
            cbr += (PROMILLE * ty.births.total.both) / ty.population.total.both;
            cdr += (PROMILLE * ty.deaths.total.both) / ty.population.total.both;
        }
        cbr /= nyears;
        cdr /= nyears;
        
        // пересчитать число рождений и смертей вне стабилизированого участка
        for (int year = 1896; year <= 1916; year++)
        {
            if (!(year >= y1 && year <= y2))
            {
                TerritoryYear ty = xt.territoryYearOrNull(year);
                ty.births.total.both = Math.round(ty.population.total.both * cbr / PROMILLE); 
                ty.deaths.total.both = Math.round(ty.population.total.both * cdr / PROMILLE); 
            }
        }

        return xt;
    }
}