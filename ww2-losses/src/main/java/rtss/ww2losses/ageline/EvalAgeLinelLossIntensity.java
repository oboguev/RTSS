package rtss.ww2losses.ageline;

import rtss.data.selectors.Gender;
import rtss.util.Util;
import rtss.ww2losses.model.Automation;
import rtss.ww2losses.struct.HalfYearEntries;
import rtss.ww2losses.struct.HalfYearEntry;

/*
 * Найти величину loss_intensity, при которой проводка возрастной линии от середины 1941 года
 * до начала 1946 года даёт ожидаемый остаток населения в начале 1946 года.
 */
public class EvalAgeLinelLossIntensity
{
    private final SteerAgeLine steer;

    public EvalAgeLinelLossIntensity(SteerAgeLine steer)
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
     * Миграция полагается нулевой.     
     */
    public double evalPreliminaryLossIntensity(
            int initial_age_ndays,
            Gender gender,
            double initial_population,
            double final_population,
            HalfYearEntries<HalfYearEntry> immigration_halves) throws Exception
    {
        final double tolerance = 1.0e-5;

        Util.assertion(initial_population >= 0 && final_population >= 0);

        double a1 = 0;
        double div1 = divergence(initial_age_ndays, gender, initial_population, final_population, a1, immigration_halves);
        if (div1 == 0)
            return 0;

        if (div1 > 0)
        {
            double a2 = 2.0;
            double div2 = divergence(initial_age_ndays, gender, initial_population, final_population, a2, immigration_halves);
            if (div2 >= 0)
                throw new Exception("внутренняя ошибка");

            /*
             * Искать а между а1 и a2, покуда div не приблизится к нулю
             */
            for (int pass = 0;;)
            {
                double a = (a1 + a2) / 2;
                if (Math.abs(div2 - div1) < final_population * tolerance)
                    return checksign(a, 1);

                double div = divergence(initial_age_ndays, gender, initial_population, final_population, a, immigration_halves);
                if (div == 0)
                {
                    return checksign(a, 1);
                }
                else if (div < 0)
                {
                    a2 = a;
                    div2 = div;
                }
                else if (div > 0)
                {
                    a1 = a;
                    div1 = div;
                }

                if (pass++ > 10000)
                    throw new Exception("поиск не сходится");
            }
        }
        else // if (div1 < 0)
        {
            double a2 = -2.0;
            double div2 = divergence(initial_age_ndays, gender, initial_population, final_population, a2, immigration_halves);

            if (div2 <= 0 && Automation.isAutomated())
            {
                a2 = -3.0;
                div2 = divergence(initial_age_ndays, gender, initial_population, final_population, a2, immigration_halves);
            }

            if (div2 <= 0)
                throw new Exception("внутренняя ошибка");

            /*
             * Искать а между а1 и a2, покуда div не приблизится к нулю
             */
            for (int pass = 0;;)
            {
                double a = (a1 + a2) / 2;
                if (Math.abs(div2 - div1) < final_population * tolerance)
                    return checksign(a, -1);

                double div = divergence(initial_age_ndays, gender, initial_population, final_population, a, immigration_halves);
                if (div == 0)
                {
                    return checksign(a, -1);
                }
                else if (div < 0)
                {
                    a1 = a;
                    div1 = div;
                }
                else if (div > 0)
                {
                    a2 = a;
                    div2 = div;
                }

                if (pass++ > 10000)
                    throw new Exception("поиск не сходится");
            }
        }
    }

    private double divergence(
            int initial_age_ndays,
            Gender gender,
            double initial_population,
            double final_population,
            double loss_intensity,
            HalfYearEntries<HalfYearEntry> immigration_halves) throws Exception
    {
        double remainder = steer.steerPreliminary(
                                                  initial_age_ndays,
                                                  gender,
                                                  initial_population,
                                                  loss_intensity,
                                                  null,
                                                  immigration_halves);

        return remainder - final_population;
    }

    private double checksign(double a, int sign) throws Exception
    {
        if (sign > 0)
        {
            Util.assertion(a > 0);
            ;
        }
        else if (sign < 0)
        {
            Util.assertion(a < 0);
        }
        else // if (sign == 0)
        {
            Util.assertion(a == 0);
        }

        return a;
    }

    /* ============================================================================================ */

    /*
     * Вычислить интенсивность миграции, при которой к началу 1946 года будет достигнут ожидаемый
     * остаток возрастной линии.
     * 
     * initial_age_ndays = начальный возраст в середине 1941 года
     * gender = пол
     * initial_population = начальная численность населения возрастной линии в середине 1941 года
     * final_population = начальная численность населения возрастной линии в начале 1946 года
     * loss_intensity = интенсивность потерь
     * 
     * Военные потери в полугодии вычисляются как ac_xxx * initial_population * loss_intensity.     
     */
    public double evalMigrationIntensity(
            int initial_age_ndays,
            Gender gender,
            double initial_population,
            double final_population,
            double loss_intensity) throws Exception
    {

        final double tolerance = 1.0e-6;

        Util.assertion(initial_population >= 0 && final_population >= 0);

        double a1 = 0;
        double div1 = divergence(initial_age_ndays, gender, initial_population, final_population, loss_intensity, a1);
        if (div1 == 0)
            return 0;

        if (div1 > 0)
        {
            double a2 = 2.0;
            double div2 = divergence(initial_age_ndays, gender, initial_population, final_population, loss_intensity, a2);
            if (div2 >= 0)
                throw new Exception("внутренняя ошибка");

            /*
             * Искать а между а1 и a2, покуда div не приблизится к нулю
             */
            for (int pass = 0;;)
            {
                double a = (a1 + a2) / 2;
                if (Math.abs(div2 - div1) < final_population * tolerance)
                    return checksign(a, 1);

                double div = divergence(initial_age_ndays, gender, initial_population, final_population, loss_intensity, a);
                if (div == 0)
                {
                    return checksign(a, 1);
                }
                else if (div < 0)
                {
                    a2 = a;
                    div2 = div;
                }
                else if (div > 0)
                {
                    a1 = a;
                    div1 = div;
                }

                if (pass++ > 10000)
                    throw new Exception("поиск не сходится");
            }
        }
        else // if (div1 < 0)
        {
            double a2 = -2.0;
            double div2 = divergence(initial_age_ndays, gender, initial_population, final_population, loss_intensity, a2);
            if (div2 <= 0)
                throw new Exception("внутренняя ошибка");

            /*
             * Искать а между а1 и a2, покуда div не приблизится к нулю
             */
            for (int pass = 0;;)
            {
                double a = (a1 + a2) / 2;
                if (Math.abs(div2 - div1) < final_population * tolerance)
                    return checksign(a, -1);

                double div = divergence(initial_age_ndays, gender, initial_population, final_population, loss_intensity, a);
                if (div == 0)
                {
                    return checksign(a, -1);
                }
                else if (div < 0)
                {
                    a1 = a;
                    div1 = div;
                }
                else if (div > 0)
                {
                    a2 = a;
                    div2 = div;
                }

                if (pass++ > 10000)
                    throw new Exception("поиск не сходится");
            }
        }
    }

    private double divergence(
            int initial_age_ndays,
            Gender gender,
            double initial_population,
            double final_population,
            double loss_intensity,
            double immigration_intensity) throws Exception
    {
        double remainder = steer.steerPreliminary(
                                                  initial_age_ndays,
                                                  gender,
                                                  initial_population,
                                                  loss_intensity,
                                                  immigration_intensity,
                                                  null);

        return final_population - remainder;
    }
}
