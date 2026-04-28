package rtss.pre1917.util;

import rtss.pre1917.data.RateURValue;
import rtss.pre1917.data.RateValueByGender;
import rtss.pre1917.data.URValue.URValueWhich;

public class CalcRate
{
    public static RateURValue cbr(RateURValue births, RateURValue population)
    {
        RateURValue cbr = new RateURValue(null);
        cbr.total = cbr(births.total, population.total, cbr, URValueWhich.TOTAL);
        cbr.urban = cbr(births.urban, population.urban, cbr, URValueWhich.URBAN);
        cbr.rural = cbr(births.rural, population.rural, cbr, URValueWhich.RURAL);
        return cbr;
    }

    public static RateURValue cdr(RateURValue deaths, RateURValue population)
    {
        RateURValue cdr = new RateURValue(null);
        cdr.total = cdr(deaths.total, population.total, cdr, URValueWhich.TOTAL);
        cdr.urban = cdr(deaths.urban, population.urban, cdr, URValueWhich.URBAN);
        cdr.rural = cdr(deaths.rural, population.rural, cdr, URValueWhich.RURAL);
        return cdr;
    }
    
    public static RateValueByGender cbr(RateValueByGender births, RateValueByGender population, RateURValue urValue, URValueWhich which)
    {
        RateValueByGender cbr = new RateValueByGender(urValue, which);
        cbr.both = cbr(births.both, population.both);
        cbr.male = cbr(births.male, population.male);
        cbr.female = cbr(births.female, population.female);
        return cbr;
    }

    public static RateValueByGender cdr(RateValueByGender births, RateValueByGender population, RateURValue urValue, URValueWhich which)
    {
        RateValueByGender cdr = new RateValueByGender(urValue, which);
        cdr.both = cdr(births.both, population.both);
        cdr.male = cdr(births.male, population.male);
        cdr.female = cdr(births.female, population.female);
        return cdr;
    }

    public static Double cbr(Double births, Double population)
    {
        if (births == null || population == null)
            return null;
        
        double v = births / population;

        if (Double.isNaN(v) || Double.isInfinite(v))
            throw new RuntimeException("Floating-point overflow");
            
        return v;
    }

    public static Double cdr(Double deaths, Double population)
    {
        if (deaths == null || population == null)
            return null;

        double v = deaths / population;

        if (Double.isNaN(v) || Double.isInfinite(v))
            throw new RuntimeException("Floating-point overflow");
            
        return v;
    }
}
