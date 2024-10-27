package rtss.pre1917.calc.containers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
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

        return this;
    }

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