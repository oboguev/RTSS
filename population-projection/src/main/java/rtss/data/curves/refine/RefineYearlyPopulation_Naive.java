package rtss.data.curves.refine;

import rtss.data.bin.Bin;
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
    /* 
     * Характерное модельное соотношение убыли среднегодового населения для возрастов 0-4,
     * при переходе от возраста (x) к возрасту (x + 1), где x = [0...4].
     * Взято по таблицам смертности Госкомстата для СССР 1926-1927, 1939-1939 и 1958-1959 гг.
     * 
     * Распределение весьма близко между годами:
     * 
     *     1926 = 53.612, 22.581, 11.097, 7.223, 5.487 
     *     1938 = 62.054, 20.411,  9.147, 5.275, 3.113
     *     1958 = 64.474, 17.232,  8.530, 5.790, 3.974
     *     
     */
    private static final double[] attrition04_1926 = { 8706, 3667, 1802, 1173, 891 };
    private static final double[] attrition04_1938 = { 10963, 3606, 1616, 932, 550 };
    private static final double[] attrition04_1958 = { 1882, 503, 249, 169, 116 };

    /*
     * То же для убыли в возрастах 5-9:
     * 
     *     1926 = 31.194, 23.981, 18.735, 14.801, 11.288
     *     1938 = 30.145, 23.602, 18.736, 15.045, 12.472
     *     1958 = 23.622, 21.850, 20.276, 17.913, 16.339
     */
    private static final double[] attrition59_1926 = { 666, 512, 400, 316, 241 };
    private static final double[] attrition59_1938 = { 539, 422, 335, 269, 223 };
    private static final double[] attrition59_1958 = { 120, 111, 103, 91, 83 };

    public static double[] refine(Bin[] bins, String title, double[] p, Integer yearHint) throws Exception
    {
        /*
         * Не использовать, т.к. иногда вызывает надлом, напр. в случае РСФСР-MALE-1931,
         * т.к. распределение 5-летних средних не соответствует картине возрастного убывания.
         * Причина объяснена выше.
         */
        if (Util.True)
            return p;

        Util.unused(attrition04_1926, attrition04_1938, attrition04_1958);
        Util.unused(attrition59_1926, attrition59_1938, attrition59_1958);

        double[] p0 = p;

        if (bins[0].widths_in_years != 5 || p[0] <= p[5])
            return p;

        double original_sum04 = Util.sum(Util.splice(p, 0, 4));
        if (original_sum04 / 5 <= p[5])
            return p;

        if (bins[1].widths_in_years != 5 || p[5] <= p[10])
            return p;

        double original_sum59 = Util.sum(Util.splice(p, 5, 9));
        if (original_sum59 / 5 <= p[10])
            return p;

        final double[] attrition04 = select_attrition04(yearHint);
        final double[] attrition59 = select_attrition59(yearHint);

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
                double sum59 = Util.sum(Util.splice(p510, 5 - 5, 9 - 5));

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
                double sum04 = Util.sum(Util.splice(p05, 0, 4));

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

    private static double[] select_attrition04(Integer yearHint)
    {
        if (yearHint == null)
            yearHint = 1938;

        if (yearHint >= 1943)
            return attrition04_1958;
        else if (yearHint >= 1933)
            return attrition04_1938;
        else
            return attrition04_1926;
    }

    private static double[] select_attrition59(Integer yearHint)
    {
        if (yearHint == null)
            yearHint = 1938;

        if (yearHint >= 1943)
            return attrition59_1958;
        else if (yearHint >= 1933)
            return attrition59_1938;
        else
            return attrition59_1926;
    }
}
