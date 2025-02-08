package rtss.math.interpolate.disaggregate;

import java.util.Arrays;

import rtss.util.Util;

/*
 * Дезагреггировать массив состоящий из агрегированных средних величин в исходные до-аггегированные значения.
 * Работает с участками (корзинами) аггрегации равной длины.
 * Для корзин переменной длины использовать DisaggregateVariableWidthSeries (который также работает и для корзин одинаковой ширины).
 * 
 * Алгоритм составлен DeepSeek-V3 по запросу:
 * 
 *     Can you design an algorithm?
 *
 *     The goal is to decompose the aggregated values of a series into original values.
 *
 *     Suppose there is a serires f(x) where x is an integer number in range 0 to 999.
 *     Series f(x) has positive values and is reasonably smooth.
 *
 *     Then axis x is divided into consequtive intervals of 10 points each, and for each interval the average value of f is calculated for the interval, yielding series f2.
 *
 *     Then the original series f is discarded or lost, so only the aggregated series f2 remains available.
 *
 *     The goal is to produce series f3(x) for every integer point x from 0 to 999 approximating the original values f(x) such that:
 *
 *     1. The average value of f3(x) over the interval points equals f2 for the interval.
 *
 *     2. Values of f3(x) are positive.
 *
 *     3. The restored series f3(x) is smooth looking.
 *
 *     Can you please write it in Java?
 */
public class DisaggregateConstantWidthSeries
{
    /*
     * Дезагреггировать @aggregated в массив длиной @restoredPoints.
     * Каждая точка в  @aggregated распаковывается в участок длиной @samplinglSize точек.
     * Предполагается, что restoredPoints == samplinglSize * aggregated.length.
     * 
     * Пример вызова:
     *         double[] yyy = DisaggregateConstantWidthSeries.disaggregate(sss, restoredPoints, samplinglSize, 2000, 50.0, 1e-6);
     */
    public static double[] disaggregate(double[] aggregated, int restoredPoints, int samplinglSize, int numIterations, double smoothingSigma, double positivityThreshold) throws Exception
    {
        Util.assertion(restoredPoints == samplinglSize * aggregated.length);
        
        double[] restored = new double[restoredPoints];

        /*
         * The @restored array is initialized with the average values from @aggregated for each interval.
         */
        for (int i = 0; i < aggregated.length; i++)
        {
            int start = i * samplinglSize;
            int end = start + samplinglSize;
            Arrays.fill(restored, start, end, aggregated[i]);
        }

        double[] restoredPrev = new double[restoredPoints];
        System.arraycopy(restored, 0, restoredPrev, 0, restoredPoints);

        /*
         * The smoothing and adjustment steps are repeated until the series converges 
         * (changes between iterations fall below a threshold).
         */
        for (int iteration = 0; iteration < numIterations; iteration++)
        {
            /*
             * Apply Gaussian smoothing.
             * A Gaussian filter is applied to smooth the @restored array. 
             * The gaussianFilter method creates a Gaussian kernel and convolves it with the data.
             */
            double[] restoredSmoothed = gaussianFilter(restored, smoothingSigma);

            /*
             * Enforce average constraints.
             * For each interval, the values in @restored are adjusted to match the average value from @aggregated.
             */
            for (int i = 0; i < aggregated.length; i++)
            {
                int start = i * samplinglSize;
                int end = start + samplinglSize;
                double currentAvg = calculateAverage(restoredSmoothed, start, end);
                double scalingFactor = aggregated[i] / currentAvg;
                for (int j = start; j < end; j++)
                {
                    restored[j] = restoredSmoothed[j] * scalingFactor;
                }
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

            /*
             * Optional: Check for convergence.
             * he maxDifference method calculates the maximum difference between the current and previous iterations 
             * to determine convergence.
             */
            if (iteration > 0 && maxDifference(restored, restoredPrev) < 1e-6)
                break;

            System.arraycopy(restored, 0, restoredPrev, 0, restoredPoints);
        }

        return restored;
    }

    private static double[] gaussianFilter(double[] data, double sigma)
    {
        int size = data.length;
        double[] smoothedData = new double[size];
        double[] kernel = createGaussianKernel(sigma);

        int kernelRadius = kernel.length / 2;

        for (int i = 0; i < size; i++)
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

        // Normalize the kernel
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