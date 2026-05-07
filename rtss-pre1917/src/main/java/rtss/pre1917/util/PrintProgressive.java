package rtss.pre1917.util;

import rtss.math.algorithms.MathUtil;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.data.migration.MissingMigrationDataException;
import rtss.pre1917.data.migration.TotalMigration;
import rtss.pre1917.eval.EvalGrowthRate;
import rtss.util.Util;

public class PrintProgressive
{
    private final static double PROMILLE = 1000.0;
    private final static String nl = "\n";
    private final static char NBSP = 0xA0;
    private final static String NBSP_S = "" + NBSP;
    
    public static void print(Territory t) throws Exception
    {
        final TotalMigration totalMigration = TotalMigration.getTotalMigration();
        final EvalGrowthRate evalGrowthRate = new EvalGrowthRate(null);
        StringBuilder sb = new StringBuilder(); 
        
        sb.append("год      чн       чр     чс      мигр   р    с    еп      чн2     р2   с2   еп2 s" + nl);
        sb.append("==== ========= ======= ======= ======= ==== ==== ====  ========= ==== ==== ==== =" + nl);

        // ###addLastYear(t, totalMigration);

        for (int year : t.years())
        {
            if (year >= 1896)
            {
                TerritoryYear ty = t.territoryYear(year);

                Double cbr = rate(ty.births.total.both, ty.progressive_population.total.both);
                Double cdr = rate(ty.deaths.total.both, ty.progressive_population.total.both);
                Double ngr = (cbr != null && cdr != null) ? cbr - cdr : null;

                Double cbr2 = null;
                Double cdr2 = null;
                Double ngr2 = null;
                Long pop_middle = null;

                TerritoryYear ty2 = t.territoryYearOrNull(year + 1);
                if (ty2 != null &&
                    ty2.progressive_population != null &&
                    ty2.progressive_population.total != null &&
                    ty2.progressive_population.total.both != null)
                {
                    pop_middle = MathUtil.log_average(ty.progressive_population.total.both, ty2.progressive_population.total.both);
                    cbr2 = rate(ty.births.total.both, pop_middle);
                    cdr2 = rate(ty.deaths.total.both, pop_middle);
                    ngr2 = (cbr2 != null && cdr2 != null) ? cbr2 - cdr2 : null;
                }

                Long saldo = null;

                try
                {
                    saldo = totalMigration.saldo(t.name, year);
                }
                catch (MissingMigrationDataException ex)
                {
                    if (year < 1916)
                        throw ex;
                }

                boolean stable = evalGrowthRate.is_stable_year(t.name, year);

                line(sb, year,
                     ty.progressive_population.total.both,
                     ty.births.total.both,
                     ty.deaths.total.both,
                     saldo,
                     stable,
                     cbr, cdr, ngr,
                     pop_middle,
                     cbr2, cdr2, ngr2);
            }
        }
        
        Util.out(sb.toString());
    }

    private static void line(StringBuilder sb, int year, Long population, Long births, Long deaths, Long saldo, boolean stable,
            Double cbr, Double cdr, Double ngr,
            Long pop_middle, 
            Double cbr2, Double cdr2, Double ngr2)
    {
        String line = String.format("%4d %s %s %s %s %s %s %s %s %s %s %s",
                                    year,
                                    s_count(population, 9),
                                    s_count(births, 7),
                                    s_count(deaths, 7),
                                    s_count(saldo, 7),
                                    s_rate(cbr), s_rate(cdr), s_rate(ngr),
                                    s_count(pop_middle, 9),
                                    s_rate(cbr2), s_rate(cdr2), s_rate(ngr2),
                                    stable ? "*" : NBSP_S);

        sb.append(line + nl);
    }

    private static String s_count(Long value, int width)
    {
        return s_wide(value == null ? "" : String.format("%,d", value), width);
    }

    private static String s_rate(Double value)
    {
        return s_wide(value == null ? "" : String.format("%.1f", value), 4);
    }

    private static String s_wide(String s, int width)
    {
        while (s.length() < width)
            s = " " + s;
        return s;
    }

    private static Double rate(Long v, Long pop)
    {
        if (v == null || pop == null || pop == 0)
            return null;
        else
            return (v * PROMILLE) / pop;
    }
}
