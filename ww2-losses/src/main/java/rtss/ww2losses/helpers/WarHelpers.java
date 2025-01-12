package rtss.ww2losses.helpers;

import rtss.data.population.PopulationByLocality;
import rtss.data.population.Population;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

public class WarHelpers
{
    public static void validateDeficit(PopulationByLocality deficit) throws Exception
    {
        Population p = deficit.forLocality(Locality.TOTAL);
        validateDeficit(p, Gender.MALE); 
        validateDeficit(p, Gender.FEMALE); 
    }

    public static void validateDeficit(Population deficit, Gender gender) throws Exception
    {
        double negsum = 0;
        double sum = 0;
        
        for (int age = 0; age <= Population.MAX_AGE; age++)
        {
            double v = deficit.get(gender, age);
            sum += v;
            
            if (v == 0 && age <= 3)
                continue;
            
            if (v <= 0)
            {
                negsum += -v;
                Util.err(String.format("Отрицательный дефицит %s %d %,15.0f", gender.name(), age, -v));
            }
        }
        
        if (negsum > 0)
        {
            Util.err(String.format("Доля отрицательных значений в общем дефиците %s %.2f%%", gender.name(), 100 * negsum/sum));
            Util.err("");
        }
    }
}
