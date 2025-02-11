package rtss.data.curves;

import rtss.data.bin.Bin;
import rtss.util.Util;

/*
 * Сделать ход годового распределения численности населения в возрастах 0-4
 * более напоминающим реалистический. 
 */
// ################# TEST
public class RefineYearlyPopulation
{
    /* 
     * Характерное соотношение убыли среднегодового населения для возрастов 0-4,
     * при переходе от возраста (x) к возрасту (x + 1), где x = [0...4].
     * Взято по таблице смертности ГКС для СССР 1939-1939 гг.
     */
    private static final double[] attrition = { 10963, 3606, 1616, 932, 550 };

    public static double[] refine(Bin[] bins, String title, double[] p) throws Exception
    {
        if (bins[0].widths_in_years != 5 || p[0] <= p[5])
            return p;

        double original_sum04 = Util.sum(Util.splice(p, 0, 4));

        double a1 = 0;
        double a2 = 2 * original_sum04;

        for (int pass = 0;;)
        {
            if (pass++ > 10_000)
                throw new Exception("RefineYearlyPopulation не сходится");
            
            double a = (a1 + a2) / 2;
            double[] p05 = calc04(Util.splice(p, 0, 5), Util.normalize(attrition, a));
            double sum04 = Util.sum(Util.splice(p05, 0, 4));

            if (Util.same(sum04, original_sum04))
            {
                p = Util.dup(p);
                Util.insert(p, p05, 0);
                return p;
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

    private static double[] calc04(double[] p05, double[] increments)
    {
        p05 = Util.dup(p05);
        for (int age = 4; age >= 0; age--)
            p05[age] = p05[age + 1] + increments[age];
        return p05;
    }
}
