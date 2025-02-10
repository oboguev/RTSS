package rtss.data.population.calc;

import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;

public class RebalanceUrbanRural
{
    /*
     * Перебалансировать городское и сельское население таким обрзом, чтобы доля городского населения
     * составила  @urbanShare (0.0 ... 1.0)
     */
    public static PopulationByLocality rebalanceUrbanRural(PopulationByLocality p, double urbanShare) throws Exception
    {
       if (urbanShare < 0 || urbanShare > 1 || !p.hasRuralUrban())
           throw new IllegalArgumentException();
       
        double all = p.sum(Locality.TOTAL, Gender.BOTH, 0, Population.MAX_AGE);
        double newUrban = urbanShare * all;
        double newRural = all - newUrban;
        
        Population urban = RescalePopulation.scaleTo(p.forLocality(Locality.URBAN), newUrban);
        Population rural = RescalePopulation.scaleTo(p.forLocality(Locality.RURAL), newRural);
        
        return new PopulationByLocality(urban, rural);
    }
}
