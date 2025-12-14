package rtss.data.population.projection.helper;

import rtss.data.asfr.AgeSpecificFertilityRates;
import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Locality;

public class CalcBirths
{
    /*    
     * Интерполировать возрастные линии и для каждого дня (c учётом старения) вычислить женское насееление, 
     * приложить ASFR и получить число рождений в данный день 
     */
    public static double[] eval_day_births(PopulationContext pctx1, PopulationContext pctx2, int ndays, Locality locality, AgeSpecificFertilityRates asfr)
    {
        double[] day_births = new double[ndays];
        
        for (int ndage = 0; ndage <= pctx1.MAX_DAY; ndage++)
        {
            
        }
        // ######
        return day_births;
    }
}
