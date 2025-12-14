package rtss.rosbris;

import java.util.HashMap;
import java.util.Map;

import rtss.data.population.struct.Population;
import rtss.data.selectors.Locality;
import rtss.rosbris.core.RosBrisDataSet;
import rtss.rosbris.core.RosBrisDataSet.DataEntry;

public class RosBrisFertilityRates
{
    private static RosBrisDataSet ds_2012_2023;
    private static RosBrisDataSet ds_1989_2014;
    private static RosBrisDataSet ds_2015_2022;
    private static boolean use2021census = true;

    public static void use2021census(boolean b)
    {
        use2021census = true;
    }

    private static RosBrisDataSet forYear(int year) throws Exception
    {
        if (year >= 2012 && year <= 2022 && use2021census || year == 2023)
        {
            if (ds_2012_2023 == null)
                ds_2012_2023 = RosBrisDataSet.load("RosBRIS/BRa/BRa2012-2023.txt");

            return ds_2012_2023.selectEq("Year", year);
        }
        else if (year >= 1989 && year <= 2014)
        {
            if (ds_1989_2014 == null)
                ds_1989_2014 = RosBrisDataSet.load("RosBRIS/BRa/BRa1989-2014.txt");

            return ds_1989_2014.selectEq("Year", year);
        }
        else if (year >= 2015 && year <= 2022)
        {
            if (ds_2015_2022 == null)
                ds_2015_2022 = RosBrisDataSet.load("RosBRIS/BRa/BRa2015-2022.txt");

            return ds_2015_2022.selectEq("Year", year);
        }
        else
        {
            throw new RuntimeException("Invalid year selector");
        }
    }

    public static RosBrisFertilityRates loadFertilityRatesForTerritory(Integer territory) throws Exception
    {
        return loadFertilityRates(territory, null);
    }

    public static RosBrisFertilityRates loadFertilityRatesForYear(Integer year) throws Exception
    {
        return loadFertilityRates(null, year);
    }

    public static RosBrisFertilityRates loadFertilityRates(Integer territory, Integer year) throws Exception
    {
        RosBrisFertilityRates dr = new RosBrisFertilityRates();
        dr.territory = territory;
        dr.year = year;

        RosBrisDataSet ds = forYear(year);

        if (territory != null)
            ds = ds.selectEq("Reg", territory);

        if (year != null)
            ds = ds.selectEq("Year", year);

        for (DataEntry de : ds.entries())
        {
            ValueKey vk = new ValueKey();
            vk.territory = de.asInt("Reg");
            vk.year = de.asInt("Year");
            vk.locality = decodeLocality(de.asString("Group"));

            Double[] values = new Double[Population.MAX_AGE + 1];

            for (int age = 0; age <= Population.MAX_AGE; age++)
            {
                String key = "Bra" + age;
                if (de.has(key))
                    values[age] = de.asDouble(key);
            }

            if (dr.map.containsKey(vk))
                throw new Exception("Duplicate data records");

            dr.map.put(vk, values);
        }

        return dr;
    }

    /* =============================================================================================== */

    private Map<ValueKey, Double[]> map = new HashMap<>();
    private Integer territory;
    private Integer year;
    
    private double scale = 1e-6;

    public Double fertilityRate(int territory, int year, Locality locality, int age)
    {
        Double[] va = map.get(new ValueKey(territory, year, locality));
        return (va == null || age >= va.length || va[age] == null) ? null : scale * va[age];
    }

    public Double fertilityRate(Locality locality, int age)
    {
        Double[] va = map.get(new ValueKey(territory, year, locality));
        return (va == null || age >= va.length || va[age] == null) ? null : scale * va[age];
    }

    public Double fertilityRateForTerritory(int territory, Locality locality, int age)
    {
        Double[] va = map.get(new ValueKey(territory, year, locality));
        return (va == null || age >= va.length || va[age] == null) ? null : scale * va[age];
    }

    public Double fertilityRateForYear(int year, Locality locality, int age)
    {
        Double[] va = map.get(new ValueKey(territory, year, locality));
        return (va == null || age >= va.length || va[age] == null) ? null : scale * va[age];
    }

    /* =============================================================================================== */

    private static class ValueKey
    {
        public int territory;
        public int year;
        public Locality locality;

        public ValueKey()
        {
        }

        public ValueKey(int territory, int year, Locality locality)
        {
            this.territory = territory;
            this.year = year;
            this.locality = locality;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            ValueKey that = (ValueKey) o;

            if (territory != that.territory)
                return false;
            if (year != that.year)
                return false;
            if (locality != that.locality)
                return false;
            return true;
        }

        @Override
        public int hashCode()
        {
            int result = territory;
            result = 31 * result + year;
            result = 31 * result + (locality != null ? locality.hashCode() : 0);
            return result;
        }
    }

    /* =============================================================================================== */

    private static Locality decodeLocality(String code)
    {
        switch (code)
        {
        case "T":
            return Locality.TOTAL;
        case "U":
            return Locality.URBAN;
        case "R":
            return Locality.RURAL;
        default:
            throw new RuntimeException("Invalid locality code " + code);
        }
    }
}
