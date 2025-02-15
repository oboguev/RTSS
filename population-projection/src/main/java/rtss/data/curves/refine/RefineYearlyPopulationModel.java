package rtss.data.curves.refine;

import rtss.data.selectors.Gender;
import rtss.util.Util;

public class RefineYearlyPopulationModel 
{
    /*
     * Характерное модельное соотношение убыли среднегодового населения для возрастов 0-4,
     * при переходе от возраста (x) к возрасту (x + 1), где x = [0...4].
     * Взято по таблицам смертности Госкомстата для СССР 1926-1927, 1939-1939 и 1958-1959 гг.
     * 
     * Распределение весьма близко между календарными годами (оба пола, убыль в возрастах 0-4):
     * 
     *     1926 = 53.612, 22.581, 11.097, 7.223, 5.487 
     *     1938 = 62.054, 20.411,  9.147, 5.275, 3.113
     *     1958 = 64.474, 17.232,  8.530, 5.790, 3.974
     *     
     * То же для убыли в возрастах 5-9:
     * 
     *     1926 = 31.194, 23.981, 18.735, 14.801, 11.288
     *     1938 = 30.145, 23.602, 18.736, 15.045, 12.472
     *     1958 = 23.622, 21.850, 20.276, 17.913, 16.339
     */
    
    private static final double[] attrition_m_1926 = {9092, 3742, 1807, 1165, 888, 670, 518, 410, 325, 248, 196, 170, 169, 184, 201};
    private static final double[] attrition_m_1938 = {11376, 3652, 1620, 926, 550, 550, 438, 352, 283, 234, 202, 183, 175, 174, 182};
    private static final double[] attrition_m_1958 = {1984, 502, 251, 180, 123, 131, 127, 119, 108, 100, 92, 89, 90, 90, 101};

    private static final double[] attrition_f_1926 = {8296, 3589, 1795, 1180, 894, 667, 504, 391, 306, 234, 188, 169, 179, 202, 217};
    private static final double[] attrition_f_1938 = {10525, 3557, 1614, 938, 552, 534, 416, 324, 257, 210, 182, 167, 164, 173, 185};
    private static final double[] attrition_f_1958 = {1776, 502, 249, 158, 107, 106, 97, 84, 73, 69, 64, 63, 64, 67, 71};

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

    public static double[] select_attrition014(Integer yearHint, Gender gender)
    {
        if (yearHint == null)
            yearHint = 1938;
        
        if (gender == Gender.MALE)
        {
            if (yearHint >= 1943)
                return attrition_m_1958;
            else if (yearHint >= 1933)
                return attrition_m_1938;
            else
                return attrition_m_1926;
        }
        else if (gender == Gender.FEMALE)
        {
            if (yearHint >= 1943)
                return attrition_f_1958;
            else if (yearHint >= 1933)
                return attrition_f_1938;
            else
                return attrition_f_1926;
        }
        else
        {
            throw new IllegalArgumentException("неверный указатель пола");
        }
    }

    public static double[] select_attrition04(Integer yearHint, Gender gender)
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

    public static double[] select_attrition59(Integer yearHint, Gender gender)
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

    public static double[] select_attrition09(Integer yearHint, Gender gender)
    {
        return Util.concat(select_attrition04(yearHint, gender), select_attrition59(yearHint, gender));
    }
}
