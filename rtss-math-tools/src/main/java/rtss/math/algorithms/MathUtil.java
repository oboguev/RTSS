package rtss.math.algorithms;

import rtss.util.CoreMathUtil;

public class MathUtil
{
    /*
     * Логарифмическое среднее двух величин.
     * Используется для оценки среднего (по периоду времени) значения для
     * величины растущей или убывающей экспоненциально, например населения. 
     */
    public static double log_average(double pstart, double pend)
    {
        return CoreMathUtil.log_average(pstart, pend);
    }

    public static long log_average(long pstart, long pend)
    {
        return CoreMathUtil.log_average(pstart, pend);
    }
    
    /*
     * nday -- номер дня в году (0 ... 364)
     * ndayPopulation -- численность населения в день nday
     * ngr -- годовой темп роста населения (в промилле)  
     */
    public static long yearStartPopulation(int nday, long ndayPopulation, double ngr)
    {
        return CoreMathUtil.yearStartPopulation(nday, ndayPopulation, ngr);
    }
}
