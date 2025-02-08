package rtss.ww2losses.helpers;

import rtss.data.population.forward.ForwardPopulation;
import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Gender;
import rtss.util.Util;

/*
 * Вспомогательные функции
 */
public class WarHelpers
{
    public static void validateDeficit(PopulationContext deficit) throws Exception
    {
        String noNotice = null;
        validateDeficit(deficit, noNotice);
    }

    public static void validateDeficit(PopulationContext deficit, String notice) throws Exception
    {
        if (notice != null)
        {
            Util.err("");
            Util.err(notice);
            Util.err("");
        }

        validateDeficit(deficit, Gender.MALE);
        validateDeficit(deficit, Gender.FEMALE);
    }

    private static void validateDeficit(PopulationContext deficit, Gender gender) throws Exception
    {
        double negsum = 0;
        double sum = 0;

        for (int age = 0; age <= Population.MAX_AGE; age++)
        {
            double v = deficit.getYearValue(gender, age);
            sum += v;

            if (v == 0 && age <= 3)
                continue;

            if (v <= 0)
            {
                negsum += -v;
                if (v == 0)
                    Util.err(String.format("Отрицательный дефицит %s %-3d %,15.0f [1941 age: %d]", gender.name(), age, 0.0, age - 5));
                else
                    Util.err(String.format("Отрицательный дефицит %s %-3d %,15.0f [1941 age: %d]", gender.name(), age, -v, age - 5));
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
     * Интенсивность рождений до интервала = @nb1; интенсивность рождений после интервала = @nb3.
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
