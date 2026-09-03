package rtss.pre1917.calc;

import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryNames;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.data.migration.TotalMigration;
import rtss.pre1917.eval.EvalProgressive;
import rtss.util.Util;

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
        final String tname = "Дагестанская обл.";
        TerritoryNames.checkValidTerritoryName(tname);

        Territory t = tds.get(tname);
        if (t == null)
            return;

        TerritoryYear ty1897 = t.territoryYearOrNull(1897);
        long delta = ty1897.progressive_population.total.both - ty1897.population.total.both;

        for (int year = 1896; year <= 1914; year++) // ###@@@
        {
            TerritoryYear ty = t.territoryYearOrNull(year);
            ty.population.total.both += delta;
        }
    }

    /*
     * Исправление для Самаркандской области.
     * Использовать расчёт УГВИ (1896-1901 экстраполированный на 1881-1901), затем ЦСК (1904-1915).
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

        for (int year = 1881; year <= 1901; year++)
        {
            TerritoryYear ty = t.territoryYearOrNull(year);
            ty.progressive_population.total.both = ty.population.total.both;
        }

        for (int year = 1904; year <= 1915; year++)
        {
            TerritoryYear ty = t.territoryYearOrNull(year);
            TerritoryYear tyCSK = tCSK.territoryYearOrNull(year);
            ty.progressive_population.total.both = tyCSK.population.total.both;
        }

        interpolate_progressive_population(t, 1901, 1904);

        for (int year = 1881; year <= 1915; year++)
        {
            TerritoryYear ty = t.territoryYearOrNull(year);
            ty.migration.total.both = TotalMigration.getTotalMigration().saldo_nullable(tname, year);
        }
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

        for (int year = 1881; year <= 1887; year++)
        {
            TerritoryYear ty = t.territoryYearOrNull(year);
            if (ty.population.total.both != null)
                throw new Exception("Unexpected: data for Бакинская с Баку");
        }

        long offset;
        {
            TerritoryYear ty = t.territoryYearOrNull(1888);
            offset = (ty.population.total.both + ty.progressive_population.total.both) / 2 - ty.progressive_population.total.both;
        }

        for (int year = 1888; year <= 1914; year++)
        {
            TerritoryYear ty = t.territoryYearOrNull(year);
            if (ty.population.total.both == null || ty.progressive_population.total.both == null)
                Util.noop();
            ty.progressive_population.total.both = (ty.population.total.both + ty.progressive_population.total.both) / 2;
        }

        for (int year = 1881; year <= 1887; year++)
        {
            TerritoryYear ty = t.territoryYearOrNull(year);
            ty.progressive_population.total.both += offset;
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

    /* ===================================================================================== */

    /*
     * Корректровка числа рождений и смертей в Сувалкской губернии в 1906-1913 годах
     */
    public void fixSuvalkskaia() throws Exception
    {
        final String SUVALKI = "Сувалкская";

        final String[] controls = { "Ковенская",
                                    "Виленская",
                                    "Ломжинская" };

        final int BASE_YEAR = 1904;
        final int FIRST_FIX_YEAR = 1906;
        final int LAST_FIX_YEAR = 1913;

        Territory suvalki = tds.get(SUVALKI);

        TerritoryYear base = suvalki.territoryYearOrNull(BASE_YEAR);
        if (base == null)
            throw new Exception("No data for Сувалкская, " + BASE_YEAR);

        if (base.births.total.both == null || base.deaths.total.both == null)
            throw new Exception("No births/deaths for Сувалкская, " + BASE_YEAR);

        long baseBirths = base.births.total.both;
        long baseDeaths = base.deaths.total.both;
        long basePopulation = cskPopulation(SUVALKI, BASE_YEAR);

        /*
         * Estimate log-linear trends of birth and death rates in the control provinces.
         * 1905 is deliberately excluded because it is a revolutionary / Russo-Japanese-war year.
         * 1904 is included as the base observation.
         */
        double birthSlope = controlRateLogSlope(controls, BASE_YEAR, LAST_FIX_YEAR, true);
        double deathSlope = controlRateLogSlope(controls, BASE_YEAR, LAST_FIX_YEAR, false);

        /*
         * Anchor the fitted trends exactly at Suwalki 1904.
         * We use only the regression slope here, not its intercept:
         *
         *   rate(t) = rate(1904) * exp(slope * (t - 1904))
         *
         * Therefore the correction preserves the observed 1904 level.
         */
        for (int year = FIRST_FIX_YEAR; year <= LAST_FIX_YEAR; year++)
        {
            TerritoryYear ty = suvalki.territoryYearOrNull(year);
            if (ty == null)
                throw new Exception("No data for Сувалкская " + year);

            long population = cskPopulation(SUVALKI, year);

            double populationRatio = (double) population / (double) basePopulation;

            double birthRateRatio = Math.exp(birthSlope * (year - BASE_YEAR));
            double deathRateRatio = Math.exp(deathSlope * (year - BASE_YEAR));

            long births = Math.round(baseBirths * populationRatio * birthRateRatio);
            long deaths = Math.round(baseDeaths * populationRatio * deathRateRatio);

            ty.births.total.both = births;
            ty.deaths.total.both = deaths;
        }
        
        /*
         * Пересчитать прогрессивный расчёт 
         */
        TerritoryDataSet tds2 = tds.dupSingleTerritory(SUVALKI);
        new EvalProgressive(tds2).evalProgressive();
        tds.put(SUVALKI, tds2.get(SUVALKI));
    }

    /*
     * Fit
     *
     *     log(controlRate(year) / controlRate(1904))
     *         = intercept + slope * (year - 1904)
     *
     * by ordinary least squares.
     *
     * Years used:
     *
     *     1904, 1906, 1907, ..., 1913
     *
     * 1905 is excluded.
     */
    private double controlRateLogSlope(
            String[] controls,
            int baseYear,
            int lastYear,
            boolean births) throws Exception
    {
        double baseRate = pooledControlRate(controls, baseYear, births);

        double sumX = 0;
        double sumY = 0;
        double sumXX = 0;
        double sumXY = 0;
        int n = 0;

        for (int year = baseYear; year <= lastYear; year++)
        {
            if (year == 1905)
                continue;

            double rate = pooledControlRate(controls, year, births);

            double x = year - baseYear;
            double y = Math.log(rate / baseRate);

            sumX += x;
            sumY += y;
            sumXX += x * x;
            sumXY += x * y;
            n++;
        }

        double denominator = n * sumXX - sumX * sumX;

        if (denominator == 0)
            throw new Exception("Cannot estimate control trend");

        return (n * sumXY - sumX * sumY) / denominator;
    }

    /*
     * Pooled rate for the control provinces:
     *
     *     sum(events) / sum(CSK population)
     *
     * A province is omitted for a year if the corresponding event count
     * is absent.  This matters in particular for Ломжинская in 1913.
     */
    private double pooledControlRate(
            String[] controls,
            int year,
            boolean births) throws Exception
    {
        long events = 0;
        long population = 0;
        int used = 0;

        for (String name : controls)
        {
            Territory t = tds.get(name);
            TerritoryYear ty = t.territoryYearOrNull(year);

            if (ty == null)
                continue;

            Long value = births ? ty.births.total.both
                                : ty.deaths.total.both;

            if (value == null)
                continue;

            events += value;
            population += cskPopulation(name, year);
            used++;
        }

        if (used == 0 || population == 0)
        {
            throw new Exception("No control data for " + year +
                                (births ? " births" : " deaths"));
        }

        return (double) events / (double) population;
    }

    /*
     * Population at the beginning of the year according to CSK.
     * These are used instead of the UGVI population figures because the
     * latter contain conspicuous year-to-year jumps in some provinces.
     */
    private long cskPopulation(String territory, int year) throws Exception
    {
        Territory t = tdsCSK.get(territory);
        TerritoryYear ty = t.territoryYearOrNull(year);
        return ty.population.total.both;
    }
}
