package rtss.data;

import rtss.util.Util;

public class DoubleArray
{
    @SuppressWarnings("unused")
    private int maxage;
    private ValueConstraint vc;
    private Double[] values;

    @SuppressWarnings("unused")
    private DoubleArray()
    {
    }

    public DoubleArray(int maxage, ValueConstraint vc)
    {
        this.maxage = maxage;
        this.vc = vc;
        this.values = new Double[maxage + 1];
    }

    public DoubleArray(DoubleArray a)
    {
        this.maxage = a.maxage;
        this.vc = a.vc;
        this.values = a.values.clone();
    }

    public DoubleArray clone()
    {
        return new DoubleArray(this);
    }

    public Double[] get() throws Exception
    {
        return values;
    }

    public Double get(int age) throws Exception
    {
        if (values[age] == null)
            throw new Exception("Missing data for age " + age);
        return values[age];
    }

    public boolean containsKey(int age)
    {
        return values[age] != null;
    }

    public void put(int age, double v) throws Exception
    {
        set(age, v);
    }

    public void set(int age, double v) throws Exception
    {
        values[age] = Util.validate(v);
        checkValueRange(values[age]);
    }

    public void add(int age, double v) throws Exception
    {
        Double dv = values[age];
        if (dv == null)
            throw new Exception("Missing data for age " + age);

        if (dv != null)
            v += dv;

        checkValueRange(v);

        values[age] = v;
    }

    public void sub(int age, double v) throws Exception
    {
        Double dv = values[age];

        if (dv != null)
        {
            v = dv - v;
        }
        else
        {
            throw new Exception("Missing data for age " + age);
            // v = -v;
        }

        checkValueRange(v);

        values[age] = v;
    }

    /*
     * Выборка [age1 ... age2].
     * 
     * Нецелое значение года означает, что население выбирается только от/до этой возрастной точки.
     * Так age2 = 80.0 означает, что население с возраста 80.0 лет исключено. 
     * Аналогично, age2 = 80.5 означает, что включена половина населения в возрасте 80 лет,
     * а население начиная с возраста 81 года исключено целиком. 
     */
    public DoubleArray selectByAge(double age1, double age2) throws Exception
    {
        DoubleArray selection = clone();

        int age1_floor = (int) Math.round(Math.floor(age1));
        int age2_floor = (int) Math.round(Math.floor(age2));

        int age1_ceil = (int) Math.round(Math.ceil(age1));
        int age2_ceil = (int) Math.round(Math.ceil(age2));

        /*
         * Обрезать нижний участок 
         */
        for (int age = 0; age < age1_floor; age++)
        {
            if (selection.values[age] != null)
                selection.values[age] = 0.0;
        }

        if (age1_ceil != age1_floor)
        {
            double fraction = age1_ceil - age1;
            if (selection.values[age1_floor] != null)
                selection.values[age1_floor] *= fraction;
        }

        /*
         * Обрезать верхний участок 
         */
        for (int age = age2_ceil; age < values.length; age++)
        {
            if (selection.values[age] != null)
                selection.values[age] = 0.0;
        }

        if (age2_ceil != age2_floor)
        {
            double fraction = age2 - age2_floor;
            if (selection.values[age2_floor] != null)
                selection.values[age2_floor] *= fraction;
        }

        return selection;
    }

    /*
     * Вернуть результат вычитания @this - @p
     */
    public DoubleArray sub(DoubleArray p) throws Exception
    {
        if (vc != p.vc)
            throw new IllegalArgumentException("массивы разнотипны");

        if (maxage != p.maxage)
            throw new IllegalArgumentException("массивы разнотипны");

        DoubleArray res = clone();

        for (int age = 0; age < values.length; age++)
        {
            if (res.values[age] != null || p.values[age] != null)
                res.values[age] -= p.values[age];
        }

        return res;
    }

    /*
     * Вернуть результат вычитания @this + @p
     */
    public DoubleArray add(DoubleArray p) throws Exception
    {
        if (vc != p.vc)
            throw new IllegalArgumentException("массивы разнотипны");

        if (maxage != p.maxage)
            throw new IllegalArgumentException("массивы разнотипны");

        DoubleArray res = clone();

        for (int age = 0; age < values.length; age++)
        {
            if (res.values[age] != null || p.values[age] != null)
                res.values[age] += p.values[age];
        }

        return res;
    }


    private void checkValueRange(double v) throws Exception
    {
        Util.validate(v);

        switch (vc)
        {
        case POSITIVE:
            if (v <= 0)
                throw new IllegalArgumentException("Value is negative or zero");
            break;

        case NON_NEGATIVE:
            if (v < 0)
                throw new IllegalArgumentException("Value is negative");
            break;

        case NONE:
        default:
            break;
        }
    }

    public double[] asUnboxedArray() throws Exception
    {
        double[] d = new double[values.length];
        for (int age = 0; age < values.length; age++)
        {
            Double v = values[age];
            if (v == null)
                throw new Exception("Mising value for age " + age);
            d[age] = v;
        }
        return d;
    }
}
