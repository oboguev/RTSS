package rtss.ww2losses.population1941.math;

import rtss.util.Util;

public class RefineSeries
{
    public double minRelativeLevel = 0.1;
    public double sigma = 1.0;
    public int gaussianKernelWindow = 3;

    public double[] modifySeries(double[] f, double[] minValues, int[] intervalLengths, int XMAX) throws Exception
    {
        int numIntervals = intervalLengths.length;
        double[] f2 = new double[XMAX + 1];
        int start = 0;

        for (int i = 0; i < numIntervals; i++)
        {
            int end = start + intervalLengths[i];
            double sum = 0.0;

            for (int x = start; x < end; x++)
                sum += f[x];
            Util.assertion(sum > 0);

            double average = Util.validate(sum / intervalLengths[i]);
            double minValue = minRelativeLevel * average;

            for (int x = start; x < end; x++)
            {
                f2[x] = Math.max(f[x], Math.max(minValue, minValues[x]));
            }

            redistributeValuesToPreserveSum(f2, minValues, start, end, sum);
            smoothSeriesWithGaussian(f2, intervalLengths, minValues, start, end, sum);
            start = end;
        }
        return f2;
    }

    private void redistributeValuesToPreserveSum(double[] f2, double[] minValues, int start, int end, double targetSum)
    {
        double sumF2 = 0.0;
        for (int x = start; x < end; x++)
            sumF2 += f2[x];

        if (sumF2 != targetSum)
        {
            double adjustmentFactor = targetSum / sumF2;
            double excess = 0.0;

            for (int x = start; x < end; x++)
            {
                double newValue = f2[x] * adjustmentFactor;
                if (newValue < minValues[x])
                {
                    excess += minValues[x] - newValue;
                    f2[x] = minValues[x];
                }
                else
                {
                    f2[x] = newValue;
                }
            }

            if (excess > 0)
            {
                double remainingSum = 0.0;
                for (int x = start; x < end; x++)
                {
                    if (f2[x] > minValues[x])
                    {
                        remainingSum += f2[x] - minValues[x];
                    }
                }
                if (remainingSum > 0)
                {
                    double redistributionFactor = (remainingSum - excess) / remainingSum;
                    for (int x = start; x < end; x++)
                    {
                        if (f2[x] > minValues[x])
                        {
                            f2[x] = minValues[x] + (f2[x] - minValues[x]) * redistributionFactor;
                        }
                    }
                }
            }
        }
    }

    private void smoothSeriesWithGaussian(double[] f2, int[] intervalLengths, double[] minValues, int start, int end, double originalSum)
    {
        double[] smoothedInterval = new double[end - start];

        for (int x = start; x < end; x++)
        {
            double sum = 0.0;
            double weightSum = 0.0;

            for (int dx = -gaussianKernelWindow; dx <= gaussianKernelWindow; dx++)
            {
                int neighborX = x + dx;
                if (neighborX >= start && neighborX < end)
                {
                    double weight = gaussian(dx, sigma);
                    sum += f2[neighborX] * weight;
                    weightSum += weight;
                }
            }
            smoothedInterval[x - start] = sum / weightSum;
        }

        double smoothedSum = 0.0;
        for (double value : smoothedInterval)
            smoothedSum += value;

        double normalizationFactor = originalSum / smoothedSum;
        for (int x = start; x < end; x++)
        {
            f2[x] = Math.max(smoothedInterval[x - start] * normalizationFactor, minValues[x]);
        }
    }

    private double gaussian(int x, double sigma)
    {
        return Math.exp(-(x * x) / (2 * sigma * sigma));
    }
}
