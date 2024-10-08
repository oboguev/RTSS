package rtss.pre1917.data;

import rtss.pre1917.data.URValue.URValueWhich;
import rtss.util.Util;

public class ValueByGender
{
    public Long male;
    public Long female;
    public Long both;

    public final URValue urValue;
    public final URValueWhich which;

    public ValueByGender(URValue urValue, URValueWhich which)
    {
        this.urValue = urValue;
        this.which = which;
    }

    public Long get(Gender gender) throws Exception
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

    public void set(Gender gender, Long v) throws Exception
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

    public ValueByGender dup(URValue urValue)
    {
        ValueByGender x = new ValueByGender(urValue, which);

        x.male = this.male;
        x.female = this.female;
        x.both = this.both;

        return x;
    }

    public void merge(ValueByGender v)
    {
        if (v.isEmpty())
            return;

        if (isEmpty())
        {
            male = v.male;
            female = v.female;
            both = v.both;
            return;
        }

        boolean clearFM = oneNull(male, v.male) || oneNull(female, v.female);

        male = merge(male, v.male);
        female = merge(female, v.female);
        both = merge(both, v.both);

        if (clearFM)
        {
            male = null;
            female = null;
        }
    }

    private boolean isEmpty()
    {
        if (male == null && female == null && both == null)
            return true;
        if (is0(male) && is0(female) && is0(both))
            return true;
        return false;
    }

    private boolean is0(Long v)
    {
        return v != null && v == 0;
    }

    private Long merge(Long v1, Long v2)
    {
        if (v1 == null && v2 != null && v2 == 0)
        {
            return null;
        }
        else if (v1 != null && v1 == 0 && v2 == null)
        {
            return null;
        }
        else if (v1 != null && v2 != null)
        {
            return v1 + v2;
        }
        else if (v1 != null)
        {
            return v1;
        }
        else if (v2 != null)
        {
            return v2;
        }
        else
        {
            return null;
        }
    }

    private boolean oneNull(Long v1, Long v2)
    {
        if (v1 == null && v2 == null)
            return false;
        else if (v1 != null && v2 != null)
            return false;
        else
            return true;
    }

    public void recalcAsSum(ValueByGender v1, ValueByGender v2)
    {
        male = merge(v1.male, v2.male);
        female = merge(v1.female, v2.female);
        both = merge(v1.both, v2.both);
    }

    public static final double MaleFemaleBirthRatio = 1.06;

    public boolean adjustFemaleBirths()
    {
        if (male != null && female != null)
        {
            long min_female = Math.round(male / MaleFemaleBirthRatio);

            if (female < min_female)
            {
                long old_female = female;
                female = min_female;
                both = male + female;

                double pct = female - old_female;
                pct = 100.0 * pct / old_female;

                String msg = String.format("Исправлено число рождений девочек %d %s (%s) увеличено на %.1f%%",
                                           urValue.territoryYear.year,
                                           urValue.territoryYear.territory.name,
                                           which.toString(),
                                           pct);
                // Util.out(msg);
                Util.unused(msg);
                return true;
            }
        }

        return false;
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

    private String l2s(Long v)
    {
        if (v == null)
            return "null";
        else
            return String.format("%,d", v);
    }
}
