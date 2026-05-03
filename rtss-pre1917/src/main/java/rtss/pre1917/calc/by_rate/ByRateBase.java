package rtss.pre1917.calc.by_rate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rtss.math.algorithms.MathUtil;
import rtss.pre1917.calc.EvalCountryTaxon;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.util.Util;

public abstract class ByRateBase
{
    private static class TerritoryResult implements Comparable<TerritoryResult>
    {
        public final String tname;
        public final double rate;

        public TerritoryResult(String tname, double rate)
        {
            this.tname = tname;
            this.rate = rate;
        }

        @Override
        public int compareTo(TerritoryResult o)
        {
            return (int) Math.signum(o.rate - this.rate);
        }
    }

    private List<TerritoryResult> results = new ArrayList<>();

    protected static final double PROMILLE = 1000.0;

    protected void eval() throws Exception
    {
        TerritoryDataSet tdsEmpire = EvalCountryTaxon.getFinalEmpirePopulationSet(false);

        for (String tname : tdsEmpire.keySet())
        {
            if (Taxon.isComposite(tname) || tname.equals("Черноморская"))
                continue;

            Territory t = tdsEmpire.get(tname);
            if (!t.hasValidVitalRate)
                continue;

            double rate = 0;
            int nyears = 0;

            for (int year = 1896; year <= 1913; year++)
            {
                if (year == 1898 || year == 1905 || year == 1910)
                    continue;
                
                TerritoryYear ty = t.territoryYearOrNull(year);
                if (ty.births.total.both != null && ty.deaths.total.both != null)
                {
                    double cbr = t.calc_mid_CBR_total_both(year);
                    double cdr = t.calc_mid_CDR_total_both(year);
                    rate += rate(cbr, cdr);
                    nyears++;
                }
                
                if (Util.False)
                {
                    long pop = ty.progressive_population.total.both;

                    TerritoryYear ty2 = t.territoryYear(year + 1);
                    long pop2 = ty2.progressive_population.total.both;

                    pop = MathUtil.log_average(pop, pop2);
                    
                    double cbr = (PROMILLE * denull(ty.births.total.both)) / pop;
                    double cdr = (PROMILLE * denull(ty.deaths.total.both)) / pop;
                    
                    rate += rate(cbr, cdr);
                    nyears++;
                }
            }

            results.add(new TerritoryResult(tname, rate / nyears));
        }

        Collections.sort(results);

        for (TerritoryResult r : results)
        {
            Util.out(String.format("\"%s\" %.1f", r.tname, r.rate));
        }
    }

    private long denull(Long v)
    {
        return v != null ? v : 0;
    }

    abstract double rate(double cbr, double cdr);
}
