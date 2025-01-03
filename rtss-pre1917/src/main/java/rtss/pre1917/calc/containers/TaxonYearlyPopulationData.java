package rtss.pre1917.calc.containers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.util.WeightedAverage;
import rtss.util.Util;

public class TaxonYearlyPopulationData extends HashMap<Integer, TaxonYearData>
{
    private static final long serialVersionUID = 1L;
    private final String taxonName;

    public final TerritoryDataSet tdsPopulation;
    public final TerritoryDataSet tdsVitalRates;
    public final TerritoryDataSet tdsCSK;
    public final int toYear;

    public TaxonYearlyPopulationData(String taxonName,
            TerritoryDataSet tdsPopulation,
            TerritoryDataSet tdsVitalRates,
            TerritoryDataSet tdsCSK,
            int toYear)
    {
        this.taxonName = taxonName;
        this.tdsPopulation = tdsPopulation;
        this.tdsVitalRates = tdsVitalRates;
        this.tdsCSK = tdsCSK;
        this.toYear = toYear;
    }

    public TaxonYearlyPopulationData print()
    {
        Util.out("Численность населения в границах " + taxonName);
        Util.out("рождаемость, смертность, естественный прирост, ест. + мех. изменение численности");
        Util.out("в нормировке на население на начало года");
        Util.out("");

        List<Integer> years = Util.sort(keySet());
        int lastPartialYear = years.get(years.size() - 1);

        for (int year : years)
        {
            TaxonYearData yd = get(year);

            if (year != lastPartialYear)
            {
                double ngr = yd.cbr - yd.cdr;
                Util.out(String.format("%d %,d %.1f %.1f %.1f %,d",
                                       year, yd.population, yd.cbr, yd.cdr, ngr,
                                       yd.population_increase));
            }
            else
            {
                Util.out(String.format("%d %,d", year, yd.population));
            }
        }

        Util.out("");
        printRateChange("CBR");
        printRateChange("CDR");
        printRateChange("NGR");

        Util.out("");
        Util.out("То же в нормировке на среднегодовое население");
        Util.out("");

        for (int year : years)
        {
            TaxonYearData yd = get(year);

            if (year != lastPartialYear)
            {
                double ngr_middle = yd.cbr_middle - yd.cdr_middle;
                Util.out(String.format("%d %,d %.1f %.1f %.1f %,d",
                                       year, yd.population, yd.cbr_middle, yd.cdr_middle, ngr_middle,
                                       yd.population_increase));
            }
            else
            {
                Util.out(String.format("%d %,d", year, yd.population));
            }
        }

        return this;
    }

    private double rate(String which, int year)
    {
        TaxonYearData yd = get(year);
        switch (which)
        {
        case "CBR":
            return yd.cbr;
        case "CDR":
            return yd.cdr;
        case "NGR":
            return yd.cbr - yd.cdr;
        default:
            throw new RuntimeException("Invalid selector");
        }
    }

    private void printRateChange(String which)
    {
        WeightedAverage wa1 = new WeightedAverage();
        wa1.add(rate(which, 1896), 1.0);
        wa1.add(rate(which, 1897), 1.0);
        wa1.add(rate(which, 1899), 1.0);
        wa1.add(rate(which, 1900), 1.0);

        WeightedAverage wa2 = new WeightedAverage();
        wa2.add(rate(which, 1911), 1.0);
        wa2.add(rate(which, 1912), 1.0);
        wa2.add(rate(which, 1913), 1.0);

        double r1 = wa1.doubleResult();
        double r2 = wa2.doubleResult();
        double dr = r2 - r1;
        double pct = 100.0 * dr / r1;

        Util.out(String.format("Изменение в %s на %.1f (%.1f%%)", which, dr, pct));
    }

    /* ======================================================================================= */

    public static class PopulationDifference implements Comparable<PopulationDifference>
    {
        public final String tname;
        public final long diff;
        public final double pct;

        public PopulationDifference(String tname, long diff, double pct)
        {
            this.tname = tname;
            this.diff = diff;
            this.pct = pct;
        }

        public PopulationDifference(String tname, long v1, long v2)
        {
            this(tname, v1 - v2, (100.0 * (v1 - v2)) / v2);
        }

        @Override
        public int compareTo(PopulationDifference o)
        {
            long d = o.diff - this.diff;
            if (d > 0)
                return 1;
            else if (d < 0)
                return -1;
            else
                return 0;
        }
    }

    public TaxonYearlyPopulationData printDifferenceWithUGVI()
    {
        List<PopulationDifference> list = new ArrayList<>();

        for (String tname : tdsPopulation.keySet())

        {
            if (!Taxon.isComposite(tname))
            {
                Territory t = tdsPopulation.get(tname);
                TerritoryYear ty = t.territoryYearOrNull(toYear);
                list.add(new PopulationDifference(tname, ty.population.total.both, ty.progressive_population.total.both));
            }
        }

        print(list, "Превышение по УГВИ");

        return this;
    }

    public TaxonYearlyPopulationData printDifferenceWithCSK()
    {
        List<PopulationDifference> list = new ArrayList<>();

        for (String tname : tdsPopulation.keySet())

        {
            if (!Taxon.isComposite(tname))
            {
                TerritoryYear ty = tdsPopulation.get(tname).territoryYearOrNull(toYear);
                TerritoryYear tyCSK = tdsCSK.get(tname).territoryYearOrNull(toYear);
                list.add(new PopulationDifference(tname, tyCSK.population.total.both, ty.progressive_population.total.both));
            }
        }

        print(list, "Превышение по ЦСК");

        return this;
    }

    private void print(List<PopulationDifference> list, String title)
    {
        Util.out("");
        Util.out(title + ":");
        Util.out("");

        Collections.sort(list);

        for (PopulationDifference pd : list)
            Util.out(String.format("    \"%s\" %,d %.1f", pd.tname, pd.diff, pd.pct));
    }
}