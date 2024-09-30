package rtss.pre1917.eval;

import rtss.pre1917.data.InnerMigration;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryYear;

public class CalcGrowthRate
{
    private final Territory t;
    private final Territory tCensus1897;
    private final InnerMigration innerMigration;

    /*
     * t contains births and deaths
     * xt contains 1897 census data
     */
    public CalcGrowthRate(Territory t, Territory tCensus1897, InnerMigration innerMigration)
    {
        this.t = t;
        this.tCensus1897 = tCensus1897;
        this.innerMigration = innerMigration;
    }

    public void CalcGrowthRate(int ytarget)
    {
        // ###
    }

    private double diff_p(double a, int ytarget)
    {
        return p1(a, ytarget) - p2(a, ytarget);
    }

    private double p1(double a, int ytarget)
    {
        TerritoryYear ty = t.territoryYearOrNull(ytarget);
        return (ty.births.total.both - ty.deaths.total.both) / a;
    }

    private double p2(double a, int ytarget)
    {
        TerritoryYear tc = tCensus1897.territoryYearOrNull(1897);

        // population at the start of 1897
        double p = tc.population.total.both;
        long in = tc.births.total.both - tc.deaths.total.both;
        in += innerMigration.inFlow(t.name, 1897);
        in -= innerMigration.outFlow(t.name, 1897);
        p -= Math.round(in * 27.0 / 356.0);

        // population at the the start of "year"
        for (int year = 1898;; year++)
        {
            p *= (1 + a);
            p += innerMigration.inFlow(t.name, year - 1);
            p -= innerMigration.outFlow(t.name, year - 1);
            if (year == ytarget)
                return p;
        }
    }
}
