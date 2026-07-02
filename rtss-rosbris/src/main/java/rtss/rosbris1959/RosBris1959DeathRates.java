package rtss.rosbris1959;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtss.csv.CSVSmartReader;
import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.selectors.Gender;

public class RosBris1959DeathRates
{
    private List<Value> values = new ArrayList<>();

    public static class Value
    {
        public final int year;
        public final Gender gender;
        public final int cause;
        public final RosBris1959AgeGroup age;
        public final double value;

        public Value(int year, Gender gender, int cause, RosBris1959AgeGroup age, double value)
        {
            this.year = year;
            this.gender = gender;
            this.cause = cause;
            this.age = age;
            this.value = value;
        }
    }

    public static RosBris1959DeathRates load() throws Exception
    {
        RosBris1959DeathRates dr = new RosBris1959DeathRates();

        CSVSmartReader csv = CSVSmartReader.fromResource("RosBRIS.1959/DRc5a1959-1988.txt");

        Map<RosBris1959AgeGroup, Integer> age2col = new HashMap<>();

        int colYear = csv.column("Year");
        int colSex = csv.column("Sex");
        int colCause = csv.column("Cause");

        for (int nr = 0; nr < csv.rowCount(); nr++)
        {
            int year = csv.asInt(nr, colYear);
            int cause = csv.asInt(nr, colCause);

            String sex = csv.asString(nr, colSex);
            Gender gender = null;
            switch (sex)
            {
            case "M":
                gender = Gender.MALE;
                break;

            case "F":
                gender = Gender.FEMALE;
                break;

            default:
                throw new Exception("Malformatted file");

            }

            for (RosBris1959AgeGroup ag : RosBris1959AgeGroup.AllAgeGroups)
            {
                int col = csv.column("Drac" + ag.from);
                int ivalue = csv.asInt(nr, col);
                Value value = new Value(year, gender, cause, ag, ivalue / 1_000_000.0);
                dr.values.add(value);
            }
        }

        return dr;
    }

    public RosBris1959DeathRates forYear(int year)
    {
        RosBris1959DeathRates dr = new RosBris1959DeathRates();

        for (Value value : values)
        {
            if (value.year == year)
                dr.values.add(value);
        }

        return dr;
    }

    public RosBris1959DeathRates forGender(Gender gender)
    {
        RosBris1959DeathRates dr = new RosBris1959DeathRates();

        for (Value value : values)
        {
            if (value.gender == gender)
                dr.values.add(value);
        }

        return dr;
    }

    public RosBris1959DeathRates forCause(int cause)
    {
        RosBris1959DeathRates dr = new RosBris1959DeathRates();

        for (Value value : values)
        {
            if (value.cause == cause)
                dr.values.add(value);
        }

        return dr;
    }

    public RosBris1959DeathRates forAge(RosBris1959AgeGroup ag)
    {
        RosBris1959DeathRates dr = new RosBris1959DeathRates();

        for (Value value : values)
        {
            if (value.age.equals(ag))
                dr.values.add(value);
        }

        return dr;
    }

    public double value()
    {
        if (values.size() == 0)
            throw new IllegalArgumentException("No element");

        if (values.size() > 1)
            throw new IllegalArgumentException("More than one element");

        return values.get(0).value;
    }

    public Bin[] asBins() throws Exception
    {
        Value v0 = null;

        for (Value value : values)
        {
            if (v0 == null)
            {
                v0 = value;
            }
            else
            {
                if (v0.year != value.year || v0.gender != value.gender || v0.cause != value.cause)
                    throw new IllegalArgumentException("Mixed values");
            }
        }
        
        List<Bin> list = new ArrayList<>();
        
        for (RosBris1959AgeGroup ag : RosBris1959AgeGroup.AllAgeGroups)
        {
            Bin bin = new Bin(ag.from, ag.to, this.forAge(ag).value());
            list.add(bin);
        }

        return Bins.bins(list);
    }

}
