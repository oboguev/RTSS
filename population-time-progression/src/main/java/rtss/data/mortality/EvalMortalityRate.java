package rtss.data.mortality;

import rtss.data.population.PopulationByLocality;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;

/**
 * Вычислить годовой уровень смертности при данной таблице смертности 
 * и данной возрастной структуре населения 
 */
public class EvalMortalityRate
{
    private static final int MAX_AGE = CombinedMortalityTable.MAX_AGE;
    
    public static double eval(final CombinedMortalityTable mt, final PopulationByLocality p) throws Exception
    {
        double total_pop = p.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);
        double total_deaths = 0;
        
        if (p.hasRuralUrban())
        {
            for (int age = 0; age <= MAX_AGE; age++)
            {
                total_deaths += deaths(mt, p, Locality.URBAN, Gender.MALE, age);
                total_deaths += deaths(mt, p, Locality.URBAN, Gender.FEMALE, age);
                total_deaths += deaths(mt, p, Locality.RURAL, Gender.MALE, age);
                total_deaths += deaths(mt, p, Locality.RURAL, Gender.FEMALE, age);
            }
        }
        else
        {
            for (int age = 0; age <= MAX_AGE; age++)
            {
                total_deaths += deaths(mt, p, Locality.TOTAL, Gender.MALE, age);
                total_deaths += deaths(mt, p, Locality.TOTAL, Gender.FEMALE, age);
            }
        }
        
        return 1000 * total_deaths / total_pop;
    }
    
    private static double deaths(
            final CombinedMortalityTable mt, 
            final PopulationByLocality p, 
            Locality locality, 
            Gender gender, 
            int age) throws Exception
    {
        double v = p.get(Locality.TOTAL, Gender.MALE, age);
        MortalityInfo mi = mt.get(Locality.TOTAL, Gender.MALE, age);
        return mi.qx * v;
    }
}
