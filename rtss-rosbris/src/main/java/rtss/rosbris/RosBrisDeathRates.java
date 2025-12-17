package rtss.rosbris;

import java.util.HashMap;
import java.util.Map;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.MortalityInfo;
import rtss.data.mortality.MortalityUtil;
import rtss.data.mortality.SingleMortalityTable;
import rtss.data.population.struct.Population;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.rosbris.core.RosBrisDataSet;
import rtss.rosbris.core.RosBrisDataSet.DataEntry;

public class RosBrisDeathRates
{
    private static RosBrisDataSet ds_2012_2022;
    private static RosBrisDataSet ds_1989_2014;
    private static RosBrisDataSet ds_2015_2022;
    private static boolean use2021census = true;

    public static void use2021census(boolean b)
    {
        use2021census = true;
    }

    private static RosBrisDataSet forYear(int year) throws Exception
    {
        if (year >= 2012 && year <= 2022 && use2021census)
        {
            if (ds_2012_2022 == null)
                ds_2012_2022 = RosBrisDataSet.load("RosBRIS/DRa/DRa2012-2022.txt");

            return ds_2012_2022.selectEq("Year", year);
        }
        else if (year >= 1989 && year <= 2014)
        {
            if (ds_1989_2014 == null)
                ds_1989_2014 = RosBrisDataSet.load("RosBRIS/DRa/DRa1989-2014.txt");

            return ds_1989_2014.selectEq("Year", year);
        }
        else if (year >= 2015 && year <= 2022)
        {
            if (ds_2015_2022 == null)
                ds_2015_2022 = RosBrisDataSet.load("RosBRIS/DRa/DRa2015-2022.txt");

            return ds_2015_2022.selectEq("Year", year);
        }
        else
        {
            throw new RuntimeException("Invalid year selector");
        }
    }

    public static RosBrisDeathRates loadMXForTerritory(RosBrisTerritory territory) throws Exception
    {
        return loadMX(territory, null);
    }

    public static RosBrisDeathRates loadMXForYear(Integer year) throws Exception
    {
        return loadMX(null, year);
    }

    public static RosBrisDeathRates loadMX(RosBrisTerritory territory, Integer year) throws Exception
    {
        RosBrisDeathRates dr = new RosBrisDeathRates();
        dr.territory = territory;
        dr.year = year;

        RosBrisDataSet ds = forYear(year);

        if (territory != null)
            ds = ds.selectEq("Reg", territory.toString());

        if (year != null)
            ds = ds.selectEq("Year", year);

        for (DataEntry de : ds.entries())
        {
            ValueKey vk = new ValueKey();
            vk.territory = new RosBrisTerritory(de.asInt("Reg"));
            vk.year = de.asInt("Year");
            vk.locality = decodeLocality(de.asString("Group"));
            vk.gender = decodeGender(de.asString("Sex"));

            Double[] values = new Double[Population.MAX_AGE + 1];

            for (int age = 0; age <= Population.MAX_AGE; age++)
            {
                String key = "Dra" + age;
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
    private RosBrisTerritory territory;
    private Integer year;
    
    private double scale = 1e-6;

    public Double mx(RosBrisTerritory territory, int year, Locality locality, Gender gender, int age)
    {
        Double[] va = map.get(new ValueKey(territory, year, locality, gender));
        return (va == null || age >= va.length) ? null : scale * va[age];
    }

    public Double mx(Locality locality, Gender gender, int age)
    {
        Double[] va = map.get(new ValueKey(territory, year, locality, gender));
        return (va == null || age >= va.length) ? null : scale * va[age];
    }

    public Double mxForTerritory(RosBrisTerritory territory, Locality locality, Gender gender, int age)
    {
        Double[] va = map.get(new ValueKey(territory, year, locality, gender));
        return (va == null || age >= va.length) ? null : scale * va[age];
    }

    public Double mxForYear(int year, Locality locality, Gender gender, int age)
    {
        Double[] va = map.get(new ValueKey(territory, year, locality, gender));
        return (va == null || age >= va.length) ? null : scale * va[age];
    }

    /* =============================================================================================== */

    private static class ValueKey
    {
        public RosBrisTerritory territory;
        public int year;
        public Locality locality;
        public Gender gender;

        public ValueKey()
        {
        }

        public ValueKey(RosBrisTerritory territory, int year, Locality locality, Gender gender)
        {
            this.territory = territory;
            this.year = year;
            this.locality = locality;
            this.gender = gender;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            ValueKey that = (ValueKey) o;

            if (territory.code() != that.territory.code())
                return false;
            if (year != that.year)
                return false;
            if (locality != that.locality)
                return false;
            return gender == that.gender;
        }

        @Override
        public int hashCode()
        {
            int result = territory.code();
            result = 31 * result + year;
            result = 31 * result + (locality != null ? locality.hashCode() : 0);
            result = 31 * result + (gender != null ? gender.hashCode() : 0);
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

    private static Gender decodeGender(String code)
    {
        switch (code)
        {
        case "M":
            return Gender.MALE;
        case "F":
            return Gender.FEMALE;
        case "B":
            return Gender.BOTH;
        default:
            throw new RuntimeException("Invalid gender code " + code);
        }
    }

    /* =============================================================================================== */
    
    public static RosBrisDeathRates from(CombinedMortalityTable cmt, RosBrisTerritory territory, int year) throws Exception
    {
        RosBrisDeathRates dr = new RosBrisDeathRates();
        dr.territory = territory;
        dr.year = year;
        
        dr.from(cmt, Locality.TOTAL);

        if (!cmt.isTotalOnly())
        {
            dr.from(cmt, Locality.RURAL);
            dr.from(cmt, Locality.URBAN);
        }
        
        return dr;
    }
    
    private void from(CombinedMortalityTable cmt, Locality locality) throws Exception
    {
        from(cmt, locality, Gender.BOTH);
        from(cmt, locality, Gender.MALE);
        from(cmt, locality, Gender.FEMALE);
    }

    private void from(CombinedMortalityTable cmt, Locality locality, Gender gender) throws Exception
    {
        ValueKey vk = new ValueKey();
        vk.territory = territory;
        vk.year = year;
        vk.locality = locality;
        vk.gender = gender;

        Double[] values = new Double[Population.MAX_AGE + 1];
        
        for (int age = 0; age <= CombinedMortalityTable.MAX_AGE; age++)
        {
            MortalityInfo mi = cmt.get(locality, gender, age);
            double mx = MortalityUtil.qx2mx(mi.qx);
            values[age] = mx / scale;
        }

        if (map.containsKey(vk))
            throw new Exception("Duplicate data records");

        map.put(vk, values);
    }
    
    /* ========================================================================== */
    
    public CombinedMortalityTable toCombinedMortalityTable() throws Exception
    {
        CombinedMortalityTable cmt = CombinedMortalityTable.newEmptyTable();
        
        for (Locality locality : Locality.AllLocalities)
        {
            for (Gender gender : Gender.ThreeGenders)
                toCombinedMortalityTable(cmt, locality, gender);
        }
        
        return cmt;
    }
    
    private void toCombinedMortalityTable(CombinedMortalityTable cmt, Locality locality, Gender gender) throws Exception
    {
        double[] qx = new double[CombinedMortalityTable.MAX_AGE + 1];
        for (int age = 0; age <= CombinedMortalityTable.MAX_AGE; age++)
        {
            double mx = mx(locality, gender, age);
            qx[age] = MortalityUtil.mx2qx(mx);
        }
        
        String path = "РосБРиС territory=" + territory + "/" + year + " " + locality + " " + gender;
        
        SingleMortalityTable smt = SingleMortalityTable.from_qx(path, qx);
        cmt.setTable(locality, gender, smt);
    }
}
