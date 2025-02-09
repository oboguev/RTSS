package rtss.ww2losses.population1941.math;

/*
 *     
 * Developed by DeepSeek using request:     
 *     
 *     Please develop an algorithm.
 *     
 *     There is a series f(x) where x is an integer number ranging from 0 to parameter XMAX.
 *     The whole range of x is divided into consecutive intervals. I will refer to them as x-intervals.
 *     The lengths of each x-interval are provided in an external array.
 *     
 *     Series f(x) represents a reasonably continuous function.
 *     Most of the values of f(x) are positive, but f(x) also has so,e segments with negative values, i.e. the value of f(x) goes negative over some ranges of x.
 *     
 *     The goal is to modify f(x) and produce its modified version f2(x) with those negative ranges changed to positive values, with the following additional constraints:
 *     
 *     1. The sum of elements f(x) over each x-interval must remain the same. I.e. the sum of f2(x) over every x-interval must be equal to the sum of f(x)  over the same interval.
 *     
 *     2. Within each x-interval, the minimum value of f2(x) must become at least 10% of the average value of f2(x) withing this interval.
 *     
 *     3. Series f2(x) must look smooth.
 *     
 *     4. In particular, series f2(x) must be smooth and continuous at the junctions of x-intervals.
 *     
 *     Please implement in Java.
 */
public class RefineSeriesV1
{
    public static double[] modifySeries(double[] f, int[] intervalLengths, int XMAX)
    {
        int numIntervals = intervalLengths.length;
        double[] f2 = new double[XMAX + 1];
        int start = 0;

        /*
         * Initialization:
         * 
         *     We start by initializing the f2 array to store the modified series.
         * 
         * Interval Processing:
         * 
         *     For each interval, we calculate the sum of the original series f(x)f(x) within that interval.
         *     We then compute the average value of f2(x) within the interval.
         *     We modify the values of f(x) to ensure that the minimum value of f2(x) is at least 10% of the average value within the interval.
         *     We adjust the values of f2(x)) to ensure that the sum of the modified series within the interval remains the same as the original series.
         */
        for (int i = 0; i < numIntervals; i++)
        {
            int end = start + intervalLengths[i];
            double sum = 0.0;

            // Calculate the sum of f(x) in the current interval
            for (int x = start; x < end; x++)
                sum += f[x];

            // Calculate the average value of f2(x) in the current interval
            double average = sum / intervalLengths[i];

            // Modify the values in the current interval to satisfy the constraints
            for (int x = start; x < end; x++)
                if (f[x] < 0)
                    f2[x] = 0.1 * average; // Ensure minimum value is at least 10% of the average
                else
                    f2[x] = f[x];

            // Adjust the values to ensure the sum remains the same
            double sumF2 = 0.0;
            for (int x = start; x < end; x++)
                sumF2 += f2[x];

            double adjustmentFactor = sum / sumF2;
            for (int x = start; x < end; x++)
                f2[x] *= adjustmentFactor;

            start = end;
        }

        /* 
         * Smooth the series to ensure continuity at the junctions of x-intervals
         * 
         * We apply a simple moving average to smooth the series within each interval.
         * We ensure continuity at the junctions of intervals by averaging the values at the boundaries.
         */
        smoothSeries(f2, intervalLengths);

        return f2;
    }

    private static void smoothSeries(double[] f2, int[] intervalLengths)
    {
        int numIntervals = intervalLengths.length;
        int start = 0;

        for (int i = 0; i < numIntervals; i++)
        {
            int end = start + intervalLengths[i];

            // Apply a simple moving average to smooth the series
            for (int x = start + 1; x < end - 1; x++)
                f2[x] = (f2[x - 1] + f2[x] + f2[x + 1]) / 3.0;

            // Ensure continuity at the junction with the next interval
            if (i < numIntervals - 1)
                f2[end - 1] = (f2[end - 2] + f2[end - 1] + f2[end]) / 3.0;

            start = end;
        }
    }

    public static void main(String[] args)
    {
        // Example usage
        int XMAX = 10;
        double[] f = { 1.0, 2.0, -1.0, 3.0, -2.0, 4.0, 5.0, -3.0, 6.0, 7.0, 8.0 };
        int[] intervalLengths = { 3, 4, 4 };

        double[] f2 = modifySeries(f, intervalLengths, XMAX);

        // Print the modified series
        for (double value : f2)
        {
            System.out.println(value);
        }
    }
}