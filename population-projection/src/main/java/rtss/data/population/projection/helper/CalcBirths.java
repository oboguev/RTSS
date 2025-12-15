package rtss.data.population.projection.helper;

import org.apache.commons.lang3.mutable.MutableDouble;

import rtss.data.asfr.AgeSpecificFertilityRates;
import rtss.data.population.projection.ForwardPopulation;
import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

public class CalcBirths
{
    /*    
     * Интерполировать возрастные линии и для каждого дня (c учётом старения) вычислить женское насееление, 
     * приложить ASFR и получить число рождений в данный день 
     */
    public static double[] eval_day_births(PopulationContext pctx1, PopulationContext pctx2, int ndays, Locality locality,
            AgeSpecificFertilityRates asfr, MutableDouble fertile_female_population) throws Exception
    {
        double[] day_births = new double[ndays];
        fertile_female_population.setValue(0);

        for (int ndage = 0; ndage <= pctx1.MAX_DAY; ndage++)
        {
            int ndage2 = ndage + ndays;

            if (ndage2 > pctx2.MAX_DAY)
                continue;

            if (ForwardPopulation.day2year(ndage2) < asfr.minAge())
                continue;

            if (ForwardPopulation.day2year(ndage) > asfr.maxAge())
                continue;

            double[] fp = interpolate_population_ageline(pctx1.getDay(locality, Gender.FEMALE, ndage),
                                                         pctx2.getDay(locality, Gender.FEMALE, ndage2),
                                                         ndays);
            fertile_female_population.add(Util.sum(fp));

            add_births(day_births, fp, ndays, asfr, ndage);
        }
        
        fertile_female_population.setValue(fertile_female_population.getValue() / ndays);
        
        return day_births;
    }

    /*
     * Экспоненциально интерполировать население возрастной микролинии 
     * от p[0] = p1
     * до p[ndays] = p2
     * Возвращает массиа p[0 ... ndays-1] 
     */
    private static double[] interpolate_population_ageline(double p1, double p2, int ndays)
    {
        double[] p = new double[ndays];

        Util.validate(p1);
        Util.validate(p2);

        if (p1 == 0 && p2 == 0)
            return p;

        if (p1 == 0 && p2 != 0)
            throw new RuntimeException("нулевое население");

        Util.assertion(p1 >= 0 && p2 >= 0);

        double a = Math.pow(p2 / p1, 1.0 / ndays);
        p[0] = p1;
        for (int k = 1; k <= ndays - 1; k++)
            p[k] = a * p[k - 1];

        return p;
    }

    private static void add_births(double[] day_births, double[] fp, int ndays, AgeSpecificFertilityRates asfr, int ndage0)
    {
        for (int nd = 0; nd < ndays; nd++)
        {
            int age_years = ForwardPopulation.day2year(ndage0 + nd);
            day_births[nd] += fp[nd] * asfr.forAge(age_years) / (1000.0 * ForwardPopulation.DAYS_PER_YEAR);
        }
    }
}
