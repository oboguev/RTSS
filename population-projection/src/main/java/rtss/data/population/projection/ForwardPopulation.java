package rtss.data.population.projection;

import rtss.data.population.struct.Population;
import rtss.util.Loggable;

public class ForwardPopulation extends Loggable
{
    protected static final int MAX_AGE = Population.MAX_AGE;
    public final static double MaleFemaleBirthRatio = 1.06;
    
    protected static final int DAYS_PER_YEAR = 365;

    protected double observed_births = 0;
    protected double observed_deaths = 0;
    
    public double getObservedBirths()
    {
        return observed_births;
    }

    public double getObservedDeaths()
    {
        return observed_deaths;
    }
    
    public static int years2days(double years)
    {
        return (int) Math.round(years * DAYS_PER_YEAR);
    }
}
