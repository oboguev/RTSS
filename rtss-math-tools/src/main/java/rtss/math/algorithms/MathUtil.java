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

        double pmin = Math.min(pstart, pend);
        double pmax = Math.max(pstart, pend);

        if (!(result >= pmin && result <= pmax))
            throw new IllegalArgumentException("Ошибка вычисления среднего");

        return result;
    }

    public static long log_average(long pstart, long pend)
    {
        return Math.round(log_average((double) pstart, (double) pend));
    }

    
    /*
     * nday -- номер дня в году (0 ... 364)
     * ndayPopulation -- численность населения в день nday
     * ngr -- годовой темп роста населения (в промилле)  
     */
    public static long yearStartPopulation(int nday, long ndayPopulation, double ngr)
    {
        if (nday < 0 || nday > 364)
            throw new IllegalArgumentException("nday must be in range 0..364");
        if (ndayPopulation < 0)
            throw new IllegalArgumentException("ndayPopulation must be non-negative");

        // ngr is annual growth rate in per mille.
        // Example: ngr = 12.5 means +12.5‰ per year = +1.25% per year.
        double annualGrowthFactor = 1.0 + ngr / 1000.0;

        if (annualGrowthFactor <= 0.0)
            throw new IllegalArgumentException("Annual growth factor must be positive");

        final double DAYS_IN_YEAR = 365.0;
        double dayFraction = nday / DAYS_IN_YEAR;

        // population(day) = population(start) * annualGrowthFactor^(nday / 365)
        double startPopulation = ndayPopulation / Math.pow(annualGrowthFactor, dayFraction);

        return Math.round(startPopulation);
    }
}
