package rtss.pre1917.eval;

import rtss.pre1917.data.InnerMigration;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryYear;

public class CalcGrowthRate
{
    private final Territory t;
    private final Territory xt;
    private final InnerMigration innerMigration;

    /*
     * t contains births and deaths
     * xt contains progressive_population
     */
    public CalcGrowthRate(Territory t, Territory xt, InnerMigration innerMigration)
    {
        this.t = t;
        this.xt = xt;
        this.innerMigration = innerMigration;
    }

    public void CalcGrowthRate(int ytarget)
    {

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
        TerritoryYear xty = xt.territoryYearOrNull(1896);
        double p = xty.progressive_population.total.both;

        for (int year = 1896;; year++)
        {
            p *= (1 + a);
            p += innerMigration.inFlow(xt.name, year);
            p -= innerMigration.outFlow(xt.name, year);
            if (year + 1 == ytarget)
                break;
        }

        return p;
    }
}
