package rtss.losses199x.util;

import java.util.Map;

import rtss.data.population.struct.PopulationByLocality;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.rosbris.RosBrisFemalePopulationAverageForBirths;
import rtss.rosbris.RosBrisFertilityRates;
import rtss.util.Util;

public class ActualBirths
{
    public double[] getRosBrisActualBirths(int y1, int y2, int territory, Locality locality) throws Exception
    {
        double[] values = new double[y2 - y1 + 1];
        
        RosBrisFemalePopulationAverageForBirths.use2021census(true);
        RosBrisFertilityRates.use2021census(true);

        for (int year = y1; year <= y2; year++)
        {
            PopulationByLocality p = RosBrisFemalePopulationAverageForBirths.getPopulationByLocality(territory, year);
            RosBrisFertilityRates rates = RosBrisFertilityRates.loadFertilityRates(territory, year);
            values[year - y1] = calcBirths(p, rates, locality);
        }

        return values;
    }
    
    private double calcBirths(PopulationByLocality p, RosBrisFertilityRates rates, Locality locality) throws Exception
    {
        double v = 0;

        for (int age = 0; age <= PopulationByLocality.MAX_AGE; age++)
        {
            Double rate = rates.fertilityRate(locality, age);
            if (rate != null)
            {
                double pop = p.get(locality, Gender.FEMALE, age);
                v += pop * rate;
            }
        }

        return v;
    }

    public void print(int y1, int y2, int territory) throws Exception
    {
        double[] u = getRosBrisActualBirths(y1, y2, territory, Locality.URBAN);
        double[] r = getRosBrisActualBirths(y1, y2, territory, Locality.RURAL);

        Util.out("");
        Util.out("Число рождений согласно РосБРиС на территории " + territory);
        Util.out("");
        Util.out(String.format("%s %9s %9s", "YEAR", "URBAN", "RURAL"));
        Util.out("");

        for (int year = y1; year <= y2; year++)
        {
            int k = year - y1;
            Util.out(String.format("%s %9s %9s", year, f2s(u[k]), f2s(r[k])));
        }
        
        Util.out("");
        Util.out("Число рождений в России согласно Росстату (Демографический ежегодник России), включая в 2014-2015 гг. Крым");
        Util.out("");
        Map<Integer,Double> actual = TableValues.actualBirths(y1, y2);        
        for (int year = y1; year <= y2; year++)
        {
            Util.out(String.format("%s %9s", year, f2s(actual.get(year))));
        }
    }

    private String f2s(double v)
    {
        long lv = Math.round(v);
        return String.format("%,d", lv);
    }
}
