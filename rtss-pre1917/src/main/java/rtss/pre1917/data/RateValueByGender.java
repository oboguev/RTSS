package rtss.pre1917.data;

import rtss.pre1917.data.URValue.URValueWhich;

public class RateValueByGender
{
    public Double male;
    public Double female;
    public Double both;

    public final RateURValue urValue;
    public final URValueWhich which;

    public RateValueByGender(RateURValue urValue, URValueWhich which)
    {
        this.urValue = urValue;
        this.which = which;
    }

    public Double get(Gender gender) throws Exception
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
            throw new Exception("Invalid selector");
        }
    }

    public void clear(Gender gender) throws Exception
    {
        set(gender, null);
    }

    public void set(Gender gender, Double v) throws Exception
    {
        switch (gender)
        {
        case MALE:
            male = v;
            break;

        case FEMALE:
            female = v;
            break;

        case BOTH:
            both = v;
            break;

        default:
            throw new Exception("Invalid selector");
        }
    }

    public RateValueByGender dup(RateURValue urValue)
    {
        RateValueByGender x = new RateValueByGender(urValue, which);

        x.male = this.male;
        x.female = this.female;
        x.both = this.both;

        return x;
    }

    public boolean hasOnlyBoth()
    {
        return male == null && female == null && both != null;
    }

    public void leaveOnlyBoth()
    {
        male = female = null;
    }

    public void clear()
    {
        both = male = female = null;
    }

    public String toString()
    {
        return String.format("M = %s, F = %s, B = %s",
                             l2s(male),
                             l2s(female),
                             l2s(both));
    }

    private String l2s(Double v)
    {
        if (v == null)
            return "null";
        else
            return String.format("%f", v);
    }
}
