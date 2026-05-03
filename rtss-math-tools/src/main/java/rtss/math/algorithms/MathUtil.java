package rtss.math.algorithms;

public class MathUtil
{
    /*
     * Логарифмическое среднее двух величин.
     * Используется для оценки среднего (по периоду времени) значения для
     * величины растущей или убывающей экспоненциально, например населения. 
     */
    public static double log_average(double pstart, double pend)
    {
        if (!Double.isFinite(pstart) || !Double.isFinite(pend))
            throw new IllegalArgumentException("Population values must be finite");

        if (pstart < 0 || pend < 0)
            throw new IllegalArgumentException("Population values must be non-negative");

        if (pstart == 0.0 && pend == 0.0)
            return 0.0;

        if (pstart == 0.0 || pend == 0.0)
            throw new IllegalArgumentException("Exponential interpolation between zero and positive population is not well-defined");

        if (pstart == pend)
            return pstart;

        double delta = pend - pstart;

        /*
         * For close values, log(pend) - log(pstart) may lose precision.
         * Use a symmetric criterion based on the ratio: when the ratio is close to 1,
         * we use the high-precision log1p formula.
         *
         * We check if |log(pend/pstart)| < threshold, which is equivalent to:
         * exp(-threshold) < pend/pstart < exp(threshold)
         *
         * Using threshold ≈ 0.4055 gives exp(±0.4055) ≈ 1.5 and 0.667
         */
        double ratio = pend / pstart;

        double result;

        if (ratio > 0.667 && ratio < 1.5)
        {
            // High precision branch for values close to each other
            double rel = delta / pstart;
            result = delta / Math.log1p(rel);
        }
        else
        {
            // Standard formula for values far apart
            result = delta / (Math.log(pend) - Math.log(pstart));
        }
        
        if (Double.isNaN(result) || Double.isInfinite(result))        
            throw new IllegalArgumentException("Failed to calculate log average");
        
        double pmin = Math.min(pstart,  pend);
        double pmax = Math.max(pstart,  pend);
        
        if (!(result >= pmin && result <= pmax))
            throw new IllegalArgumentException("Ошибка вычисления среднего");
        
        return result;
    }

    public static long log_average(long pstart, long pend)
    {
        return Math.round(log_average((double) pstart, (double) pend));
    }
}
