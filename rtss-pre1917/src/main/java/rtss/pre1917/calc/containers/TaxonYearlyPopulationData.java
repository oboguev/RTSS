package rtss.pre1917.calc.containers;

import java.util.HashMap;
import java.util.List;

import rtss.pre1917.data.TerritoryDataSet;
import rtss.util.Util;

public class TaxonYearlyPopulationData extends HashMap<Integer, TaxonYearData>
{
    private static final long serialVersionUID = 1L;
    private final String taxonName;

    public final TerritoryDataSet tdsPopulation;
    public final TerritoryDataSet tdsVitalRates;
    public final TerritoryDataSet tdsCSK;

    public TaxonYearlyPopulationData(String taxonName,
            TerritoryDataSet tdsPopulation,
            TerritoryDataSet tdsVitalRates,
            TerritoryDataSet tdsCSK)
    {
        this.taxonName = taxonName;
        this.tdsPopulation = tdsPopulation;
        this.tdsVitalRates = tdsVitalRates;
        this.tdsCSK = tdsCSK;
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
}