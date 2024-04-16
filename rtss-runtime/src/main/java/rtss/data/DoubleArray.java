package rtss.data;

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
        values[age] = v;
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
    
    private void checkValueRange(double v) throws Exception
    {
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
}
