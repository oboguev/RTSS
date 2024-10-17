package rtss.pre1917.eval;

import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.data.migration.TotalMigration;

public class CalcGrowthRate
{
    private final Territory t;
    private final Territory tCensus1897;
    private final TotalMigration totalMigration;

    /*
     * t contains births and deaths
     * xt contains 1897 census data
     */
    public CalcGrowthRate(Territory t, Territory tCensus1897, TotalMigration totalMigration)
    {
        this.t = t;
        this.tCensus1897 = tCensus1897;
        this.totalMigration = totalMigration;
    }

    private final double PROMILLE = 1000.0;

    public double calcNaturalGrowthRate(int ytarget) throws Exception
    {
        double a1 = 1.0 / PROMILLE;
        double a2 = 30.0 / PROMILLE;

        int iterations = 0;

        for (;;)
        {
            if (iterations++ > 1000)
                throw new Exception("Unable to locate crossover point: max iterations");

            double a = (a1 + a2) / 2;

            if (a2 - a1 < 0.01 / PROMILLE)
                return a * PROMILLE;

            double f = diff_p(a, ytarget);
            double f1 = diff_p(a1, ytarget);
            double f2 = diff_p(a2, ytarget);

            if (f1 == 0)
            {
                return a1 * PROMILLE;
            }
            else if (f2 == 0)
            {
                return a2 * PROMILLE;
            }
            else if (f == 0)
            {
                return a * PROMILLE;
            }

            if (f1 > 0 && f2 < 0)
            {
                if (f > 0)
                    a1 = a;
                else
                    a2 = a;
            }
            else if (f1 < 0 && f2 > 0)
            {
                if (f < 0)
                    a1 = a;
                else
                    a2 = a;
            }
            else
            {
                throw new Exception("Unable to locate crossover point");
            }
        }
    }

    /* ================================================================== */

    private double diff_p(double a, int ytarget) throws Exception
    {
        return p1(a, ytarget) - p2(a, ytarget);
    }

    private double p1(double a, int ytarget)
    {
        TerritoryYear ty = t.territoryYearOrNull(ytarget);
        return (ty.births.total.both - ty.deaths.total.both) / a;
    }

    private double p2(double a, int ytarget) throws Exception
    {
        // best estimate of population at the start of 1897 (Jan 1)
        double p = population_1897_Jan1(a);

        // population at the the start of "year"
        for (int year = 1898;; year++)
        {
            p *= (1 + a);
            p += totalMigration.saldo(t.name, year - 1);
            if (year == ytarget)
                return p;
        }
    }

    public long population_1897_Jan1(double a) throws Exception
    {
        // population at the time of 1897 census (Jan 28)
        TerritoryYear tc = tCensus1897.territoryYearOrNull(1897);
        long p = tc.population.total.both;

        // best estimate of population at the start of 1897 (Jan 1)
        return p - Math.round(increase(1897, a) * 27.0 / 365.0);
    }

    // прирост населения за год
    public long increase(int year, double a) throws Exception
    {
        TerritoryYear tc = tCensus1897.territoryYearOrNull(year);
        double in = a * tc.population.total.both;
        in += totalMigration.saldo(t.name, year);
        return Math.round(in);
    }

    public long increaseUGVI(int year) throws Exception
    {
        TerritoryYear ty = t.territoryYearOrNull(year);
        long in = ty.births.total.both - ty.deaths.total.both;
        in += totalMigration.saldo(t.name, year);
        return in;
    }
}
