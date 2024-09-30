package rtss.pre1917.eval;

import java.util.HashMap;
import java.util.Map;

import rtss.pre1917.data.InnerMigration;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;

/*
 * Пересчитать население за 1896-1915 гг. на основе сведений о естественом движении за промежуток, 
 * когда была достигнута удовлетворительная степень регистрации рождений и смертей, в предположении,
 * что в остальный годы уровень рождаемости и смертности был таков же, как в этом промежутке
 * в среднем.
 */
public class EvalGrowthRate
{
    private final Map<String, Integer> tname_y1 = new HashMap<>();
    private final Map<String, Integer> tname_y2 = new HashMap<>();

    private final TerritoryDataSet tdsCensus1897;
    private final InnerMigration innerMigration;

    public EvalGrowthRate(TerritoryDataSet tdsCensus1897, InnerMigration innerMigration)
    {
        this.innerMigration = innerMigration;
        this.tdsCensus1897 = tdsCensus1897;
        define();
    }

    private void define()
    {
        define("Акмолинская обл.", 1906, 1914);
        define("Закаспийская обл.", 1911, 1913);
        define("Семиреченская обл.", 1912, 1914);
        define("Сыр-Дарьинская обл.", 1908);
        define("Тургайская обл.", 1911, 1914);
        define("Ферганская обл.", 1912);
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

        return null;
    }
}
