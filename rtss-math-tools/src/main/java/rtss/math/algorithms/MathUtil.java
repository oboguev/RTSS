package rtss.math.algorithms;

public class MathUtil
{
    /*
     * Логарифмическое среднее двух величин.
     * Используется для оценки среднего (по периоду времени) значения для
     * величины растущей экспоненциально, например населения. 
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
         * Use log1p((pend - pstart) / pstart) instead.
         */
        double rel = delta / pstart;

        if (Math.abs(rel) < 0.5)
        {
            return delta / Math.log1p(rel);
        }
        else
        {
            return delta / (Math.log(pend) - Math.log(pstart));
        }
    }

    public static long log_average(long pstart, long pend)
    {
        return Math.round(log_average((double) pstart, (double) pend));
    }
}
