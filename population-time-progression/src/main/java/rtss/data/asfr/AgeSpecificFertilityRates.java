package rtss.data.asfr;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.population.Population;
import rtss.data.population.PopulationByLocality;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;

public class AgeSpecificFertilityRates
{
    private Bin[] bins;
    
    public AgeSpecificFertilityRates(Bin... bins)
    {
        this.bins = bins; 
    }
    
    public double forAge(int age)
    {
        for (Bin bin : bins)
        {
            if (age >= bin.age_x1 && age <= bin.age_x2)
                return bin.avg;
        }

        return 0;
    }
    
    public double births(PopulationByLocality p) throws Exception
    {
        return births(p.forLocality(Locality.TOTAL));
    }

    public double births(Population p) throws Exception
    {
        double sum = 0;
        
        for (int age = Bins.firstBin(bins).age_x1; age <= Bins.lastBin(bins).age_x2; age++)
        {
            sum += p.get(Gender.FEMALE, age) * forAge(age) / 1000;
        }
        
        return sum;
    }
    
    public double birthRate(Population p) throws Exception
    {
        return 1000 * births(p) / p.sum(Gender.BOTH, 0, Population.MAX_AGE);
    }
}
