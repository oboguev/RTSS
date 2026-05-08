package rtss.pre1917.util.data;

import rtss.math.algorithms.MathUtil;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.data.migration.TotalMigration;

public class YearData
{
    public static final double PROMILLE = 1000.0;

    int year;

    public Long population; // в начале года
    public Long births;
    public Long deaths;
    public Long migration;

    public Double cbr;
    public Double cdr;
    public Double ngr;

    public Long avg_population; // среднегодовое
    public Double cbr2;
    public Double cdr2;
    public Double ngr2;

    private YearData()
    {
    }
    
    public YearData(TerritoryYear ty) throws Exception
    {
        year = ty.year;
        population = ty.progressive_population.total.both;
        births = ty.births.total.both;
        deaths = ty.deaths.total.both;
        migration = ty.migration.total.both;
        if (migration == null)
        {
            final TotalMigration totalMigration = TotalMigration.getTotalMigration();
            migration = totalMigration.saldo(ty.territory.name, ty.year);
        }

        Long next_pop = nextYearPopulation();
        if (population != null && next_pop != null)
        {
            avg_population = MathUtil.log_average(population, next_pop);
        }

        cbr = rate(births, population);
        cdr = rate(deaths, population);
        ngr = (cbr != null && cdr != null) ? cbr - cdr : 0;

        cbr2 = rate(births, avg_population);
        cdr2 = rate(deaths, avg_population);
        ngr2 = (cbr2 != null && cdr2 != null) ? cbr2 - cdr2 : 0;
    }

    public Long nextYearPopulation()
    {
        if (population != null && births != null && deaths != null && migration != null)
        {
            return population + (births - deaths) + migration;
        }
        else
        {
            return null;
        }
    }

    public YearData nextPartialYear()
    {
        YearData yd = new YearData();
        yd.year = year + 1;
        yd.population = nextYearPopulation();
        return yd;
    }

    private Double rate(Long v, Long pop)
    {
        if (v == null || pop == null || pop == 0)
            return null;
        else
            return (v * PROMILLE) / pop;
    }
}
