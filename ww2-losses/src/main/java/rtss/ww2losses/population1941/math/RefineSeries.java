package rtss.ww2losses.population1941.math;

import rtss.util.Util;

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
 *     
 *     I used the  algorithm on real data and there is a divergence between the sum of original f(x) and the sum of produced f2(x). 
 *     The divergence is small, about 0.005% of the total, but it is there. What can be causing it?
 *     
 *     ...............
 *     
 *     It is important that the sum is exactly preserved not only for the whole series, but also for each x-interval. 
 *     Can you please amend the algorithm to ensure it?
 *     
 *     ...............
 *     
 *     I noticed that enforcing the minimum value constraint results in hard clipping of values for f2(x), and a horizontal line on the chart where f2 hits minimum. 
 *     Is it possible to modify the algorithm such that 10% of average value designates the limit only for the bottom-most value or values in the x-interval, 
 *     but descent to this local minimum or minimums and ascent from them are gradual. 
 *     I.e. to replace hard clipping with more gradual and smooth f2 series behavior around local minimums?
 *     
 *     ...............
 *
 *     I have a large number of points, a typical x-interval width is 1825 points. 
 *     Should I use larger values for some smoothing parameters in the code? Such as for gaussian kernel window, or for sigmoid transition?
 *     
 *     ...............
 *     
 *     And yet, it still clips minimal values. 
 *     The bottom part of f2 series runs on the chart as a horizontal line rather than smooth curve.
 *     What could be the cause?  
 *     
 *     https://chat.deepseek.com/a/chat/s/534e72a7-acca-4905-b531-385778cad57a
 */
public class RefineSeries
{
    public double minRelativeLevel = 0.1;
    public double sigma = 1.0;
    public int gaussianKernelWindow = 3;

    public double[] modifySeries(double[] f, int[] intervalLengths, int XMAX) throws Exception
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
                sum += f[x];
            Util.assertion(sum > 0);

            // Calculate the average value of f2(x) in the current interval
            double average = Util.validate(sum / intervalLengths[i]); 
            double minValue = minRelativeLevel * average; // Minimum value constraint

            // Modify the values in the current interval to satisfy the constraints
            for (int x = start; x < end; x++)
            {
                if (f[x] < minValue)
                    f2[x] = minValue; // Ensure minimum value is at least 10% of the average
                else
                    f2[x] = f[x];
            }

            // Adjust the values to ensure the sum remains the same
            double sumF2 = 0.0;
            for (int x = start; x < end; x++)
                sumF2 += f2[x];

            double adjustmentFactor = sum / sumF2;
            Util.checkValid(adjustmentFactor);
            for (int x = start; x < end; x++)
                f2[x] *= adjustmentFactor;

            start = end;
        }

        // Smooth the series using a Gaussian kernel while preserving the sum and minimum value
        smoothSeriesWithGaussian(f2, intervalLengths);

        return f2;
    }

    private void smoothSeriesWithGaussian(double[] f2, int[] intervalLengths)
    {
        int numIntervals = intervalLengths.length;
        int start = 0;

        for (int i = 0; i < numIntervals; i++)
        {
            int end = start + intervalLengths[i];

            // Calculate the original sum and average of the interval
            double originalSum = 0.0;
            for (int x = start; x < end; x++)
                originalSum += f2[x];
            double average = Util.validate(originalSum / intervalLengths[i]);
            double minValue = minRelativeLevel * average; // Minimum value constraint

            // Create a temporary array to store smoothed values for the current interval
            double[] smoothedInterval = new double[intervalLengths[i]];

            // Apply Gaussian smoothing within the interval
            for (int x = start; x < end; x++)
            {
                double sum = 0.0;
                double weightSum = 0.0;

                // Apply the Gaussian kernel to neighboring points
                for (int dx = -gaussianKernelWindow; dx <= gaussianKernelWindow; dx++)
                { // Use a window of 7 points (-3 to +3)
                    int neighborX = x + dx;
                    if (neighborX >= start && neighborX < end)
                    {
                        double weight = gaussian(dx, sigma);
                        sum += f2[neighborX] * weight;
                        weightSum += weight;
                    }
                }

                smoothedInterval[x - start] = Util.validate(sum / weightSum); // Normalize by the sum of weights
            }

            // Calculate the sum of the smoothed interval
            double smoothedSum = 0.0;
            for (double value : smoothedInterval)
                smoothedSum += value;

            // Normalize the smoothed interval to match the original sum
            double normalizationFactor = Util.validate(originalSum / smoothedSum);
            for (int x = start; x < end; x++)
                f2[x] = smoothedInterval[x - start] * normalizationFactor;

            // Enforce the minimum value constraint gradually
            enforceMinimumValueGradually(f2, start, end, minValue, originalSum);

            start = end;
        }
    }

    private void enforceMinimumValueGradually(double[] f2, int start, int end, double minValue, double originalSum)
    {
        // Find the bottom-most value(s) in the interval
        int numBottomValues = 1; // Number of bottom-most values to adjust
        int[] bottomIndices = new int[numBottomValues];
        double[] bottomValues = new double[numBottomValues];

        for (int i = 0; i < numBottomValues; i++)
        {
            bottomIndices[i] = start;
            bottomValues[i] = f2[start];
            for (int x = start + 1; x < end; x++)
            {
                if (f2[x] < bottomValues[i])
                {
                    bottomIndices[i] = x;
                    bottomValues[i] = f2[x];
                }
            }
        }

        // Apply a gradual scaling function to the bottom-most values
        for (int i = 0; i < numBottomValues; i++)
        {
            int x = bottomIndices[i];
            double currentValue = f2[x];
            if (currentValue < minValue)
            {
                // Use a gradual scaling function to approach the minimum value
                double scale = Math.sqrt((currentValue - minValue) / (0 - minValue)); // Gradual scaling
                Util.checkValid(scale);
                f2[x] = minValue + (currentValue - minValue) * scale;
            }
        }

        // Re-normalize to ensure the sum matches the original sum
        double finalSum = 0.0;
        for (int x = start; x < end; x++)
            finalSum += f2[x];
        double finalAdjustmentFactor = Util.validate(originalSum / finalSum);
        for (int x = start; x < end; x++)
            f2[x] *= finalAdjustmentFactor;
    }

    private double gaussian(int x, double sigma)
    {
        // Gaussian function: e^(-x^2 / (2 * sigma^2))
        return Math.exp(-(x * x) / (2 * sigma * sigma));
    }
}