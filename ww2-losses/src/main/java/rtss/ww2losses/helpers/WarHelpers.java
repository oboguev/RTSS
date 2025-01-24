package rtss.ww2losses.helpers;

import rtss.data.population.PopulationByLocality;
import rtss.data.population.forward.ForwardPopulation;
import rtss.data.population.Population;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

public class WarHelpers
{
    public static void validateDeficit(PopulationByLocality deficit) throws Exception
    {
        validateDeficit(deficit, null);
    }

    public static void validateDeficit(PopulationByLocality deficit, String notice) throws Exception
    {
        if (notice != null)
        {
            Util.err("");
            Util.err(notice);
            Util.err("");
        }

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
                if (v == 0)
                    Util.err(String.format("Отрицательный дефицит %s %d %,15.0f", gender.name(), age, 0.0));
                else
                    Util.err(String.format("Отрицательный дефицит %s %d %,15.0f", gender.name(), age, -v));
            }
        }

        if (negsum > 0)
        {
            Util.err(String.format("Доля отрицательных значений в общем дефиците %s %.2f%%", gender.name(), 100 * negsum / sum));
            Util.err("");
        }
    }

    /*
     * Распределить число рождений @nb2 на число дней @ndays.
     * Интенсивность рождений до интервала = @nb1; интенсивность рождений до интервала = @nb3.
     */
    public static double[] births(int ndays, double nb1, double nb2, double nb3) throws Exception
    {
        double[] births = new double[ndays];
        
        double a = (nb3 - nb1) / (ndays + 1);
        double b = nb3 - a * ndays;
        
        for (int k = 0; k < ndays; k++)
            births[k] = a * k  + b;
        
        births = Util.normalize(births, nb2);
        
        Util.checkValidNonNegative(births);
        
        return births;
    }

    private static final double MaleFemaleBirthRatio = ForwardPopulation.MaleFemaleBirthRatio;

    public static double[] male_births(double[] births) throws Exception
    {
        double factor = MaleFemaleBirthRatio / (1 + MaleFemaleBirthRatio);
        
        double[] v = new double[births.length];
        for (int k = 0; k < births.length; k++)
            v[k] = factor * births[k];
        return v;
    }

    public static double[] female_births(double[] births) throws Exception
    {
        double factor = 1.0 / (1 + MaleFemaleBirthRatio);

        double[] v = new double[births.length];
        for (int k = 0; k < births.length; k++)
            v[k] = factor * births[k];
        return v;
    }
}
