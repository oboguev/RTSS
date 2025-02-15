package rtss.data.curves.refine;

import rtss.data.bin.Bin;
import rtss.data.curves.refine.RefineYearlyPopulationModel.AttritionModel;
import rtss.data.selectors.Gender;
import rtss.util.Util;

/*
 * Сделать ход годового распределения численности населения в возрастах 0-4
 * более напоминающим реалистический. 
 * 
 * Эта наивная реализция не работает:
 * ход убывания может быть сделан соответствующим модельным темпам убыли
 * отдельно для диапазонов 0-4 и 5-9, но требование точного сохранения сумм для двух корзин
 * приводит к тому, что кривая в точке стыковки диапазонов становится негладкой.
 * Т.к. распределение 5-летних средних не соответствует картине возрастного убывания.
 */
public class RefineYearlyPopulation_Naive
{
    public static double[] refine(Bin[] bins, String title, double[] p, Integer yearHint, Gender gender) throws Exception
    {
        /*
         * Не использовать, т.к. иногда вызывает надлом, напр. в случае РСФСР-MALE-1931,
         * т.к. распределение 5-летних средних не соответствует картине возрастного убывания.
         * Причина объяснена выше.
         */
        if (Util.True)
            return p;

        double[] p0 = p;

        if (bins[0].widths_in_years != 5 || p[0] <= p[5])
            return p;

        double original_sum04 = Util.sum_range(p, 0, 4);
        if (original_sum04 / 5 <= p[5])
            return p;

        if (bins[1].widths_in_years != 5 || p[5] <= p[10])
            return p;

        double original_sum59 = Util.sum_range(p, 5, 9);
        if (original_sum59 / 5 <= p[10])
            return p;

        AttritionModel model = RefineYearlyPopulationModel.select_model(yearHint, gender);
        final double[] attrition04 = model.attrition04();
        final double[] attrition59 = model.attrition59();

        /* ------------------------------------------------------------- */

        if (Util.True)
        {
            double a1 = 0;
            double a2 = 2 * original_sum59;

            for (int pass = 0;;)
            {
                if (pass++ > 10_000)
                    throw new Exception("RefineYearlyPopulation не сходится [5-9]");

                double a = (a1 + a2) / 2;
                double[] p510 = calc59(Util.splice(p, 5, 10), Util.normalize(attrition59, a));
                double sum59 = Util.sum_range(p510, 5 - 5, 9 - 5);

                if (Util.same(sum59, original_sum59))
                {
                    if (p == p0)
                        p = Util.dup(p);
                    Util.insert(p, p510, 5);
                    break;
                }
                else if (sum59 > original_sum59)
                {
                    a2 = a;
                }
                else
                {
                    a1 = a;
                }
            }
        }

        /* ------------------------------------------------------------- */

        if (Util.True)
        {
            if (original_sum04 / 5 <= p[5])
            {
                // Util.err("RefineYearlyPopulation bail out: " + title);
                return p0;
            }

            double a1 = 0;
            double a2 = 2 * original_sum04;

            for (int pass = 0;;)
            {
                if (pass++ > 10_000)
                    throw new Exception("RefineYearlyPopulation не сходится [0-4]");

                double a = (a1 + a2) / 2;
                double[] p05 = calc04(Util.splice(p, 0, 5), Util.normalize(attrition04, a));
                double sum04 = Util.sum_range(p05, 0, 4);

                if (Util.same(sum04, original_sum04))
                {
                    if (p == p0)
                        p = Util.dup(p);
                    Util.insert(p, p05, 0);
                    break;
                }
                else if (sum04 > original_sum04)
                {
                    a2 = a;
                }
                else
                {
                    a1 = a;
                }
            }
        }

        return p;
    }

    private static double[] calc04(double[] p05, double[] increments)
    {
        p05 = Util.dup(p05);
        for (int age = 4; age >= 0; age--)
            p05[age] = p05[age + 1] + increments[age];
        return p05;
    }

    private static double[] calc59(double[] p510, double[] increments)
    {
        p510 = Util.dup(p510);
        for (int age = 9; age >= 5; age--)
            p510[age - 5] = p510[age - 5 + 1] + increments[age - 5];
        return p510;
    }
}
