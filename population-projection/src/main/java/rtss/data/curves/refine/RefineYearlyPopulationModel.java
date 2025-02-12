package rtss.data.curves.refine;

import rtss.util.Util;

public class RefineYearlyPopulationModel 
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

    public static double[] select_attrition04(Integer yearHint)
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

    public static double[] select_attrition59(Integer yearHint)
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

    public static double[] select_attrition09(Integer yearHint)
    {
        return Util.concat(select_attrition04(yearHint), select_attrition59(yearHint));
    }
}
