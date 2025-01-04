package rtss.data.population.forward;

import rtss.data.population.Population;

public class ForwardPopulation
{
    protected static final int MAX_AGE = Population.MAX_AGE;
    protected final double MaleFemaleBirthRatio = 1.06;

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
}
