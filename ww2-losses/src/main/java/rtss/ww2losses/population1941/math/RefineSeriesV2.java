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
 *     Most of the values of f(x) are positive, but f(x) also has some segments with negative values, i.e. the value of f(x) goes negative over some ranges of x.
 *     
 *     The goal is to modify f(x) and produce its modified version f2(x) with those negative ranges changed to positive values, with the following additional constraints:
 *     
 *     1. The sum of elements f(x) over each x-interval must remain the same. I.e. the sum of f2(x) over every x-interval must be equal to the sum of f(x) over the same interval.
 *     
 *     2. Within each x-interval, the minimum value of f2(x) must become at least 10% of the average value of f2(x) withing this interval.
 *     
 *     3. Series f2(x) must look smooth.
 *     
 *     4. In particular, series f2(x) must be smooth and continuous at the junctions of x-intervals.
 *     
 *     Please implement in Java.
 *     
 *     ...............
 *     
 *     Can you rather use a Gaussian kernel for better smoothing?
 *     With this algorithm, will the sum over every x-interval retains the original value even after smoothing?
 *     But... some of the produced values are zero, whereas they should be at least 10% of the average over x-interval and thus positive.
 *     
 *     ...............

 *     I used the  algorithm on real data and there is a divergence between the sum of original f(x) and the sum of produced f2(x). 
 *     The divergence is small, about 0.005% of the total, but it is there. What can be causing it?
 *     
 *     ...............

 *     It is important that the sum is exactly preserved not only for the whole series, but also for each x-interval. 
 *     Can you please amend the algorithm to ensure it?
 *     
 *     https://chat.deepseek.com/a/chat/s/534e72a7-acca-4905-b531-385778cad57a
 */
public class RefineSeriesV2
{
    public static double[] modifySeries(double[] f, int[] intervalLengths, int XMAX)
    {
        int numIntervals = intervalLengths.length;
        double[] f2 = new double[XMAX + 1];
        int start = 0;

        // Modify each interval
        for (int i = 0; i < numIntervals; i++)
        {
            int end = start + intervalLengths[i];
            double sum = 0.0;

            // Calculate the sum of f(x) in the current interval
            for (int x = start; x < end; x++)
            {
                sum += f[x];
            }

            // Calculate the average value of f2(x) in the current interval
            double average = sum / intervalLengths[i];
            double minValue = 0.1 * average; // Minimum value constraint

            // Modify the values in the current interval to satisfy the constraints
            for (int x = start; x < end; x++)
            {
                if (f[x] < minValue)
                {
                    f2[x] = minValue; // Ensure minimum value is at least 10% of the average
                }
                else
                {
                    f2[x] = f[x];
                }
            }

            // Adjust the values to ensure the sum remains the same
            double sumF2 = 0.0;
            for (int x = start; x < end; x++)
            {
                sumF2 += f2[x];
            }

            double adjustmentFactor = sum / sumF2;
            for (int x = start; x < end; x++)
            {
                f2[x] *= adjustmentFactor;
            }

            start = end;
        }

        // Smooth the series using a Gaussian kernel while preserving the sum and minimum value
        smoothSeriesWithGaussian(f2, intervalLengths, 1.0); // Sigma = 1.0 for the Gaussian kernel

        return f2;
    }

    private static void smoothSeriesWithGaussian(double[] f2, int[] intervalLengths, double sigma)
    {
        int numIntervals = intervalLengths.length;
        int start = 0;

        for (int i = 0; i < numIntervals; i++)
        {
            int end = start + intervalLengths[i];

            // Calculate the original sum and average of the interval
            double originalSum = 0.0;
            for (int x = start; x < end; x++)
            {
                originalSum += f2[x];
            }
            double average = originalSum / intervalLengths[i];
            double minValue = 0.1 * average; // Minimum value constraint

            // Create a temporary array to store smoothed values for the current interval
            double[] smoothedInterval = new double[intervalLengths[i]];

            // Apply Gaussian smoothing within the interval
            for (int x = start; x < end; x++)
            {
                double sum = 0.0;
                double weightSum = 0.0;

                // Apply the Gaussian kernel to neighboring points
                for (int dx = -3; dx <= 3; dx++)
                { // Use a window of 7 points (-3 to +3)
                    int neighborX = x + dx;
                    if (neighborX >= start && neighborX < end)
                    {
                        double weight = gaussian(dx, sigma);
                        sum += f2[neighborX] * weight;
                        weightSum += weight;
                    }
                }

                smoothedInterval[x - start] = sum / weightSum; // Normalize by the sum of weights
            }

            // Calculate the sum of the smoothed interval
            double smoothedSum = 0.0;
            for (double value : smoothedInterval)
            {
                smoothedSum += value;
            }

            // Normalize the smoothed interval to match the original sum
            double normalizationFactor = originalSum / smoothedSum;
            for (int x = start; x < end; x++)
            {
                f2[x] = smoothedInterval[x - start] * normalizationFactor;
            }

            // Enforce the minimum value constraint while preserving the sum
            enforceMinimumValueConstraint(f2, start, end, minValue, originalSum);

            start = end;
        }
    }

    private static void enforceMinimumValueConstraint(double[] f2, int start, int end, double minValue, double originalSum)
    {
        // Calculate the sum of values below the minimum threshold
        double deficit = 0.0;
        for (int x = start; x < end; x++)
        {
            if (f2[x] < minValue)
            {
                deficit += minValue - f2[x];
                f2[x] = minValue;
            }
        }

        // Redistribute the deficit proportionally to values above the minimum
        if (deficit > 0)
        {
            double sumAboveMin = 0.0;
            for (int x = start; x < end; x++)
            {
                if (f2[x] > minValue)
                {
                    sumAboveMin += f2[x] - minValue;
                }
            }

            if (sumAboveMin > 0)
            {
                double scaleFactor = 1.0 - (deficit / sumAboveMin);
                for (int x = start; x < end; x++)
                {
                    if (f2[x] > minValue)
                    {
                        f2[x] = minValue + (f2[x] - minValue) * scaleFactor;
                    }
                }
            }
        }

        // Verify that the sum is preserved
        double finalSum = 0.0;
        for (int x = start; x < end; x++)
        {
            finalSum += f2[x];
        }
        if (Math.abs(finalSum - originalSum) > 1e-10)
        {
            throw new IllegalStateException("Sum preservation failed for interval [" + start + ", " + (end - 1) + "]");
        }
    }

    private static double gaussian(int x, double sigma)
    {
        // Gaussian function: e^(-x^2 / (2 * sigma^2))
        return Math.exp(-(x * x) / (2 * sigma * sigma));
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

        // Verify that the sum over each interval is preserved
        int start = 0;
        for (int length : intervalLengths)
        {
            int end = start + length;
            double sum = 0.0;
            for (int x = start; x < end; x++)
            {
                sum += f2[x];
            }
            System.out.println("Sum over interval [" + start + ", " + (end - 1) + "]: " + sum);
            start = end;
        }
    }
}