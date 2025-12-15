package rtss.losses199x.util;

import java.util.Map;

import rtss.data.population.struct.PopulationByLocality;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.rosbris.RosBrisDeathRates;
import rtss.rosbris.RosBrisPopulationExposureForDeaths;
import rtss.rosbris.RosBrisTerritory;
import rtss.util.Util;

public class ActualDeaths
{
    public double[] getRosBrisActualDeaths(int y1, int y2, RosBrisTerritory territory, Gender gender) throws Exception
    {
        double[] v1 = getRosBrisActualDeaths(y1, y2, territory, Locality.URBAN, gender);
        double[] v2 = getRosBrisActualDeaths(y1, y2, territory, Locality.RURAL, gender);
        return Util.add(v1, v2);
    }

    public double[] getRosBrisActualDeaths(int y1, int y2, RosBrisTerritory territory, Locality locality) throws Exception
    {
        double[] v1 = getRosBrisActualDeaths(y1, y2, territory, locality, Gender.MALE);
        double[] v2 = getRosBrisActualDeaths(y1, y2, territory, locality, Gender.FEMALE);
        return Util.add(v1, v2);
    }

    public double[] getRosBrisActualDeaths(int y1, int y2, RosBrisTerritory territory) throws Exception
    {
        double[] v1 = getRosBrisActualDeaths(y1, y2, territory, Locality.URBAN);
        double[] v2 = getRosBrisActualDeaths(y1, y2, territory, Locality.RURAL);
        return Util.add(v1, v2);
    }

    public double[] getRosBrisActualDeaths(int y1, int y2, RosBrisTerritory territory, Locality locality, Gender gender) throws Exception
    {
        double[] values = new double[y2 - y1 + 1];

        RosBrisPopulationExposureForDeaths.use2021census(true);
        RosBrisDeathRates.use2021census(true);

        for (int year = y1; year <= y2; year++)
        {
            PopulationByLocality p = RosBrisPopulationExposureForDeaths.getPopulationByLocality(territory, year);
            RosBrisDeathRates rates = RosBrisDeathRates.loadMX(territory, year);
            values[year - y1] = calcDeaths(p, rates, locality, gender);
        }

        return values;
    }

    private double calcDeaths(PopulationByLocality p, RosBrisDeathRates rates, Locality locality, Gender gender) throws Exception
    {
        double v = 0;

        for (int age = 0; age <= PopulationByLocality.MAX_AGE; age++)
        {
            double mx = rates.mx(locality, gender, age);
            double pop = p.get(locality, gender, age);
            v += pop * mx;
        }

        return v;
    }

    public void print(int y1, int y2, RosBrisTerritory territory) throws Exception
    {
        double[] um = getRosBrisActualDeaths(y1, y2, territory, Locality.URBAN, Gender.MALE);
        double[] rm = getRosBrisActualDeaths(y1, y2, territory, Locality.RURAL, Gender.MALE);

        double[] uf = getRosBrisActualDeaths(y1, y2, territory, Locality.URBAN, Gender.FEMALE);
        double[] rf = getRosBrisActualDeaths(y1, y2, territory, Locality.RURAL, Gender.FEMALE);
        
        Util.out("");
        Util.out("Число смертей согласно РосБРиС на территории " + territory);
        Util.out("");
        Util.out(String.format("%s %9s %9s %9s %9s", "YEAR", "URBAN-M", "RURAL-M", "URBAN-F", "RURAL-F"));
        Util.out("");

        for (int year = y1; year <= y2; year++)
        {
            int k = year - y1;
            Util.out(String.format("%s %9s %9s %9s %9s", year, f2s(um[k]), f2s(rm[k]), f2s(uf[k]), f2s(rf[k])));
        }
        
        Util.out("");
        Util.out("Число смертей в России согласно Росстату (Демографический ежегодник России), включая в 2014-2015 гг. Крым");
        Util.out("");
        Map<Integer,Double> actual = TableValues.actualDeaths(y1, y2);        
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
