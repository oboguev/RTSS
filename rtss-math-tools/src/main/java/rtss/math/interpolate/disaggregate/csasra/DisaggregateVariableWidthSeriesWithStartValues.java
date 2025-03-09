package rtss.math.interpolate.disaggregate.csasra;

import java.util.Arrays;

import rtss.util.Util;

// import rtss.util.Util;

/*
 * Вариация DisaggregateVariableWidthSeries сохраняющая фиксированным начальный интервал
 */
public class DisaggregateVariableWidthSeriesWithStartValues
{
    public static double[] disaggregate(
            double[] aggregated,
            int[] intervalWidths,
            int maxIterations,
            double smoothingSigma,
            double positivityThreshold,
            double maxConvergenceDifference,
            double[] startValues) throws Exception
    {
        if (startValues.length != intervalWidths[0])
            throw new IllegalArgumentException("неверная длина первого участка");
        
        boolean converged = false;
        int totalPoints = Arrays.stream(intervalWidths).sum();
        double[] restored = new double[totalPoints];

        /*
         * The @restored array is initialized with the average values from @aggregated for each interval. 
         * The intervals are specified by the intervalWidths array.
         */
        int index = 0;
        for (int i = 0; i < aggregated.length; i++)
        {
            int width = intervalWidths[i];
            Arrays.fill(restored, index, index + width, aggregated[i]);
            index += width;
        }

        Util.insert(restored, startValues, 0);

        double[] restoredPrev = new double[totalPoints];
        System.arraycopy(restored, 0, restoredPrev, 0, totalPoints);

        /*
         * Initialize gaussian smoothing kernel
         */
        double[] kernel = createGaussianKernel(smoothingSigma);

        /*
         * The smoothing and adjustment steps are repeated until the series converges (changes between iterations fall below a threshold).
         */
        for (int iteration = 0; iteration < maxIterations; iteration++)
        {
            /* 
             * Apply Gaussian smoothing.
             * A Gaussian filter is applied to smooth the @restored array. 
             * The gaussianFilter method creates a Gaussian kernel and convolves it with the data.
             */
            double[] restoredSmoothed = gaussianFilter(restored, kernel, startValues.length);

            /*
             * Enforce average constraints.
             * For each interval, the values in @restored are adjusted to match the average value from @aggregated. 
             * The intervals are determined by the intervalWidths array.
             */
            index = 0;
            for (int i = 0; i < aggregated.length; i++)
            {
                int width = intervalWidths[i];
                if (i != 0)
                {
                    double currentAvg = calculateAverage(restoredSmoothed, index, index + width);
                    double scalingFactor = aggregated[i] / currentAvg;
                    for (int j = index; j < index + width; j++)
                    {
                        restored[j] = restoredSmoothed[j] * scalingFactor;
                    }
                }
                index += width;
            }

            /* 
             * Ensure positivity.
             * Any negative values in @restored are set to a small positive threshold (positivityThreshold).
             */
            for (int i = 0; i < restored.length; i++)
            {
                if (restored[i] < positivityThreshold)
                    restored[i] = positivityThreshold;
            }

            Util.insert(restored, startValues, 0);

            /* 
             * Optional: Check for convergence
             * The maxDifference method calculates the maximum difference between the current and previous iterations to determine convergence. 
             */
            if (iteration > 0 && maxDifference(restored, restoredPrev) < maxConvergenceDifference)
            {
                converged = true;
                break;
            }

            System.arraycopy(restored, 0, restoredPrev, 0, totalPoints);
        }

        if (!converged)
            throw new Exception("disaggregation failed to converge");

        return restored;
    }

    private static double[] gaussianFilter(double[] data, double[] kernel, int start)
    {
        int size = data.length;
        double[] smoothedData = new double[size];

        int kernelRadius = kernel.length / 2;

        for (int i = start; i < size; i++)
        {
            double sum = 0;
            double weightSum = 0;

            for (int j = -kernelRadius; j <= kernelRadius; j++)
            {
                int index = i + j;
                if (index >= 0 && index < size)
                {
                    sum += data[index] * kernel[j + kernelRadius];
                    weightSum += kernel[j + kernelRadius];
                }
            }

            smoothedData[i] = sum / weightSum;
        }

        return smoothedData;
    }

    private static double[] createGaussianKernel(double sigma)
    {
        int kernelSize = (int) (6 * sigma);

        if (kernelSize % 2 == 0)
            kernelSize++;

        double[] kernel = new double[kernelSize];
        double sum = 0;
        int radius = kernelSize / 2;

        for (int i = -radius; i <= radius; i++)
        {
            double value = Math.exp(-(i * i) / (2 * sigma * sigma));
            kernel[i + radius] = value;
            sum += value;
        }

        /* Normalize the kernel */
        for (int i = 0; i < kernel.length; i++)
            kernel[i] /= sum;

        return kernel;
    }

    private static double calculateAverage(double[] data, int start, int end)
    {
        double sum = 0;

        for (int i = start; i < end; i++)
            sum += data[i];

        return sum / (end - start);
    }

    private static double maxDifference(double[] a, double[] b)
    {
        double maxDiff = 0;

        for (int i = 0; i < a.length; i++)
        {
            double diff = Math.abs(a[i] - b[i]);
            if (diff > maxDiff)
                maxDiff = diff;
        }

        return maxDiff;
    }
}
