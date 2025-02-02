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

    public Double getNullable(int age) throws Exception
    {
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
    public DoubleArray selectByAgeYears(double age1, double age2) throws Exception
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
        return sub(p, null);
    }
    
    public DoubleArray sub(DoubleArray p, ValueConstraint rvc) throws Exception
    {
        if (vc != p.vc && rvc == null)
            throw new IllegalArgumentException("массивы разнотипны");

        if (maxage != p.maxage)
            throw new IllegalArgumentException("массивы разнотипны");

        DoubleArray res = clone();
        if (rvc != null)
            res.vc = rvc;

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
        return add(p, null);
    }
    
    public DoubleArray add(DoubleArray p, ValueConstraint rvc) throws Exception
    {
        if (vc != p.vc && rvc == null)
            throw new IllegalArgumentException("массивы разнотипны");

        if (maxage != p.maxage)
            throw new IllegalArgumentException("массивы разнотипны");

        DoubleArray res = clone();
        if (rvc != null)
            res.vc = rvc;

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

    public void fill(double v)
    {
        for (int age = 0; age <= maxage; age++)
            values[age] = v;
    }

    /*
     * Сдвинуть возрастное распределение на @years лет вверх
     */
    public DoubleArray moveUp(double years)
    {
        DoubleArray res = clone();

        int yfloor = (int) Math.floor(years);
        if (years < yfloor)
            throw new RuntimeException("Rounding error");

        /* move up by a whole number of years */
        if (yfloor != 0)
        {
            res.fill(0);

            for (int y = 0; y <= maxage; y++)
            {
                int yto = y + yfloor;
                if (yto <= maxage)
                    res.values[yto] = values[y];
            }
        }

        /* move up by a partial year */
        double dy = years - yfloor;
        if (dy > 0)
        {
            Double[] xv = new Double[maxage + 1];

            xv[0] = (1 - dy) * values[0];
            for (int y = 1; y <= maxage; y++)
                xv[y] = (1 - dy) * values[y] + dy * values[y - 1];
            
            res.values = xv;
        }

        return res;
    }

    /*
     * Сдвинуть возрастное распределение на @years лет вниз
     */
    public DoubleArray moveDown(double years)
    {
        DoubleArray res = clone();

        int yfloor = (int) Math.floor(years);
        if (years < yfloor)
            throw new RuntimeException("Rounding error");

        /* move down by a whole number of years */
        if (yfloor != 0)
        {
            res.fill(0);

            for (int y = 0; y <= maxage; y++)
            {
                int yto = y - yfloor;
                if (yto >= 0)
                    res.values[yto] = values[y];
            }
        }

        /* move down by a partial year */
        double dy = years - yfloor;
        if (dy > 0)
        {
            Double[] xv = new Double[maxage + 1];

            for (int y = 0; y < maxage; y++)
                xv[y] = (1 - dy) * values[y] + dy * values[y + 1];
            xv[maxage] = (1 - dy) * values[maxage];
            
            res.values = xv;
        }

        return res;
    }

    public void setValueConstraint(ValueConstraint vc)
    {
        this.vc = vc;
    }

    public ValueConstraint valueConstraint()
    {
        return vc;
    }
}
