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
     * Взяты по таблицам смертности Госкомстата для СССР 1926-1927, 1939-1939 и 1958-1959 гг.
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
    private static final double[] attrition04 = attrition04_1938;
    
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
    private static final double[] attrition59 = attrition59_1938;

    public static double[] refine(Bin[] bins, String title, double[] p) throws Exception
    {
        if (Util.True)
            return p;
        
        Util.unused(attrition04_1926, attrition04_1938, attrition04_1958);
        Util.unused(attrition59_1926, attrition59_1938, attrition59_1958);
        
        if (bins[0].widths_in_years != 5 || p[0] <= p[5])
            return p;

        double original_sum04 = Util.sum(Util.splice(p, 0, 4));
        if (original_sum04/5 <= p[5])
            return p;

        double a1 = 0;
        double a2 = 2 * original_sum04;

        for (int pass = 0;;)
        {
            if (pass++ > 10_000)
                throw new Exception("RefineYearlyPopulation не сходится");
            
            double a = (a1 + a2) / 2;
            double[] p05 = calc04(Util.splice(p, 0, 5), Util.normalize(attrition04, a));
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
