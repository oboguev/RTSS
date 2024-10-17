package rtss.pre1917.calc.by_rate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.calc.EvalCountryBase;
import rtss.pre1917.calc.FilterByTaxon;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.validate.CheckProgressiveAvailable;
import rtss.util.Util;

public class ByIncreaseOfGrowthRate extends EvalCountryBase
{
    public static void main(String[] args)
    {
        try
        {
            new ByIncreaseOfGrowthRate().eval();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    protected ByIncreaseOfGrowthRate() throws Exception
    {
        super("Империя", 1913);
    }

    private static class TerritoryResult implements Comparable<TerritoryResult>
    {
        public final String tname;
        public final double rate1;
        public final double rate2;
        public final double delta;

        public TerritoryResult(String tname, double rate1, double rate2)
        {
            this.tname = tname;
            this.rate1 = rate1;
            this.rate2 = rate2;
            this.delta = rate2 - rate1;
        }

        @Override
        public int compareTo(TerritoryResult o)
        {
            return (int) Math.signum(o.delta - this.delta);
        }
    }

    private List<TerritoryResult> results = new ArrayList<>();

    protected void eval() throws Exception
    {
        /* ===================== Численность населения ===================== */

        tdsPopulation = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY,
                                                LoadOptions.ADJUST_FEMALE_BIRTHS,
                                                LoadOptions.FILL_MISSING_BD,
                                                LoadOptions.MERGE_CITIES,
                                                LoadOptions.MERGE_POST1897_REGIONS,
                                                LoadOptions.EVAL_PROGRESSIVE);
        tdsPopulation.leaveOnlyTotalBoth();
        eval_1896(tdsPopulation);

        tdsPopulation = FilterByTaxon.filterByTaxon(taxonName, tdsPopulation);

        new CheckProgressiveAvailable(tdsPopulation).check(toYear + 1);

        /* ===================== Естественное движение ===================== */

        tdsVitalRates = tdsPopulation.dup();

        /* ===================== Правки ===================== */

        corrections();

        /* ===================== Результаты ===================== */

        for (String tname : tdsVitalRates.keySet())
        {
            if (Taxon.isComposite(tname) || tname.equals("Черноморская"))
                continue;

            Territory t = tdsVitalRates.get(tname);
            double r1 = rate(t, 1896, 1897, 1899, 1900);
            double r2 = rate(t, 1911, 1912, 1913);

            results.add(new TerritoryResult(tname, r1, r2));
        }

        Collections.sort(results);

        for (TerritoryResult r : results)
        {
            Util.out(String.format("\"%s\" %.2f %.1f %.1f", r.tname, r.delta, r.rate1, r.rate2));
        }
    }

    private double rate(Territory t, int... years)
    {
        double average = 0;
        int nyears = 0;

        for (int year : years)
        {
            average += rate(t, year);
            nyears++;
        }

        return average / nyears;
    }

    private double rate(Territory t, int year)
    {
        TerritoryYear ty = t.territoryYear(year);

        long pop_vital = ty.progressive_population.total.both;
        double cbr = (PROMILLE * denull(ty.births.total.both)) / pop_vital;
        double cdr = (PROMILLE * denull(ty.deaths.total.both)) / pop_vital;
        return cbr - cdr;
    }

    private long denull(Long v)
    {
        return v != null ? v : 0;
    }
}
