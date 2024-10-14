package rtss.pre1917.calc.containers;

import java.util.HashMap;
import java.util.List;

import rtss.util.Util;

public class TaxonYearlyPopulationData extends HashMap<Integer,TaxonYearData> 
{
    private static final long serialVersionUID = 1L;
    private final String taxonName;

    public TaxonYearlyPopulationData(String taxonName)
    {
        this.taxonName = taxonName;
    }
    
    public void print()
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
    }
}