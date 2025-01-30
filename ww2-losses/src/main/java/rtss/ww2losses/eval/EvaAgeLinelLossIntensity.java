package rtss.ww2losses.eval;

import rtss.data.selectors.Gender;

/*
 * Найти величину loss_intensity, при которой проводка возрастной линии от середины 1941 года
 * до начала 1946 года даёт ожидаемый остаток населения в начале 1946 года.
 */
public class EvaAgeLinelLossIntensity
{
    private final SteerAgeLine steer;

    public EvaAgeLinelLossIntensity(SteerAgeLine steer)
    {
        this.steer = steer;
    }

    /*
     * Вычислить интенсивность потерь, при которой к началу 1946 года будет достигнут ожидаемый
     * остаток возрастной линии.
     * 
     * initial_age_ndays = начальный возраст в середине 1941 года
     * gender = пол
     * initial_population = начальная численность населения возрастной линии в середине 1941 года
     * final_population = начальная численность населения возрастной линии в начале 1946 года
     * 
     * Военные потери в полугодии вычисляются как ac_xxx * initial_population * loss_intensity.     
     */
    public double evalLossIntensity(
            int initial_age_ndays,
            Gender gender,
            double initial_population,
            double final_population) throws Exception
    {
        double a1 = 0;
        double div1 = divergence(initial_age_ndays, gender, initial_population, final_population, a1);
        if (div1 <= 0)
            return 0;
        
        double a2 = 2.0;
        double div2 = divergence(initial_age_ndays, gender, initial_population, final_population, a2);
        if (div2 >= 0)
            throw new Exception("внутренняя ошибка");
        
        /*
         * Искать а между а1 и a2, покуда div не приблизится к нулю
         */
        for(int pass = 0;;)
        {
            if (pass++ > 10000)
                throw new Exception("поиск не сходится");
            
            double a = (a1 + a2) /2;
            if (Math.abs(div2 - div1) < final_population * 0.0001)
                return a;
            double div = divergence(initial_age_ndays, gender, initial_population, final_population, a);
            if (div == 0)
            {
                return a;
            }
            else if (div < 0)
            {
                a2 = a;
            }
            else if (div > 0)
            {
                a1 = a;
            }
        }
    }

    private double divergence(
            int initial_age_ndays,
            Gender gender,
            double initial_population,
            double final_population,
            double loss_intensity) throws Exception
    {
        double remainder = steer.steer(
                                       initial_age_ndays,
                                       gender,
                                       initial_population,
                                       loss_intensity);

        return remainder - final_population;
    }
}
