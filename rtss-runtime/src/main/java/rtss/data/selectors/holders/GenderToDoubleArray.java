package rtss.data.selectors.holders;

import rtss.data.ValueConstraint;
import rtss.data.selectors.Gender;

/*
 * Map gender to double array and operate its values
 */
public class GenderToDoubleArray
{
    @SuppressWarnings("unused")
    private int maxage;
    private ValueConstraint vc;
    private Double[] male;
    private Double[] female;
    private Double[] both;

    @SuppressWarnings("unused")
    private GenderToDoubleArray()
    {
    }

    public GenderToDoubleArray(int maxage, ValueConstraint vc)
    {
        this.maxage = maxage;
        this.vc = vc;
        this.male = new Double[maxage + 1];
        this.female = new Double[maxage + 1];
        this.both = new Double[maxage + 1];
    }

    public Double[] get(Gender gender) throws Exception
    {
        switch (gender)
        {
        case MALE:
            return male;

        case FEMALE:
            return female;
        
        case BOTH:
            return both;
        
        default:
            throw new IllegalArgumentException();
        }
    }

    public Double get(Gender gender, int age) throws Exception
    {
        return get(gender)[age];
    }

    public void set(Gender gender, int age, double v) throws Exception
    {
        get(gender)[age] = v;
    }

    public void add(Gender gender, int age, double v) throws Exception
    {
        Double[] d = get(gender);
        Double dv = d[age];

        if (dv != null)
            v += dv;

        checkValueRange(v);

        d[age] = v;
    }

    public void sub(Gender gender, int age, double v) throws Exception
    {
        Double[] d = get(gender);
        Double dv = d[age];

        if (dv != null)
        {
            v = dv - v;
        }
        else
        {
            v = -v;
        }
        
        checkValueRange(v);
        
        d[age] = v;
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
