package rtss.math.interpolate.disaggregate;

import java.util.Arrays;

import rtss.util.Util;

/*
 * Дезагреггировать массив состоящий из агрегированных средних величин в исходные до-аггегированные значения.
 * Работает с участками (корзинами) аггрегации переменной длины.
 * В отличие от DisaggregateConstantWidthSeries, предполагающего одинаковость ширины корзин.
 * 
 * Constrained Smooth Aggregated Series Reconstruction Algorithm (CSASRA).
 * 
 * Алгоритм составлен DeepSeek-V3 по запросу:
 * 
 *     Can you design an algorithm?
 *
 *     The goal is to decompose the aggregated values of a series into original values.
 *
 *     Suppose there is a series f(x) where x is an integer number in range 0 to 999.
 *     Series f(x) has positive values and is reasonably smooth.
 *
 *     Then axis x is divided into consequtive intervals of 10 points each, and for each interval the average value of f 
 *     is calculated for the interval, yielding series f2.
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
 *     
 *     Can you please refine the algorithm for the case when aggregation intervals have not a fixed width of 10, 
 *     but variable width specified in an external array.
 *     Produce it again in Java. 
 *     
 * Мы дополнительно делаем первый участок линейным.
 * Алгоритм предназначен для дезагрегации возрастных групп населения.        
 */
public class DisaggregateVariableWidthSeries
{
    public static double[] disaggregate(
            double[] aggregated,
            int[] intervalWidths,
            int maxIterations,
            double smoothingSigma,
            double positivityThreshold,
            double maxConvergenceDifference) throws Exception
    {
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
            double[] restoredSmoothed = gaussianFilter(restored, kernel);

            /*
             * Enforce average constraints.
             * For each interval, the values in @restored are adjusted to match the average value from @aggregated. 
             * The intervals are determined by the intervalWidths array.
             */
            index = 0;
            for (int i = 0; i < aggregated.length; i++)
            {
                int width = intervalWidths[i];
                double currentAvg = calculateAverage(restoredSmoothed, index, index + width);
                double scalingFactor = aggregated[i] / currentAvg;
                for (int j = index; j < index + width; j++)
                {
                    restored[j] = restoredSmoothed[j] * scalingFactor;
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

            /* 
             * Optional: Check for convergence
             * The maxDifference method calculates the maximum difference between the current and previous iterations to determine convergence. 
             */
            if (iteration > 0 && maxDifference(restored, restoredPrev) < maxConvergenceDifference)
            {
                converged = true;
                break;
            }
            
            if (iteration == maxIterations - 1)
            {
                // about to abort due to non-convergence
                Util.noop();
            }

            System.arraycopy(restored, 0, restoredPrev, 0, totalPoints);
        }
        
        if (!converged)
            throw new Exception("disaggregation failed to converge");

        return linearize_first_segment(restored, intervalWidths[0], intervalWidths[0] * aggregated[0]);
    }

    private static double[] gaussianFilter(double[] data, double[] kernel)
    {
        int size = data.length;
        double[] smoothedData = new double[size];
    
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

    /* ======================================================================== */

    /*
     * Сделать значения в первом интервале линейно спадающими.
     * @width = число точек в первом интервале
     * @sum = сумма, которую должны иметь точки
     */
    private static double[] linearize_first_segment(double[] y, int width, double sum) throws Exception
    {
        double b2 = 20 * sum / width;
        double b1 = 0.05 * sum / width;

        double a, b;

        for (int pass = 0 ;;)
        {
            if (pass++ > 10_000)
                throw new Exception("не сходится");
            b = (b1 + b2) / 2;
            a = (y[width] - b) / width;

            double s = sum(a, b, width);
            if (Util.same(sum, s, 0.00001))
                break;
            
            if (s > sum)
                b2 = b;
            else
                b1 = b;
        }

        y = Util.dup(y);

        for (int x = 0; x < width; x++)
            y[x] = a * x + b;

        return y;
    }

    private static double sum(double a, double b, int width)
    {
        double sum = 0;

        for (int x = 0; x < width; x++)
            sum += a * x + b;

        return sum;
    }

    /* 
     * Простая, но ошибочная версия.
     * Понять потом, в чём ошибка. 
     */
    @SuppressWarnings("unused")
    private static double[] linearize_first_segment_000(double[] y, int width, double sum) throws Exception
    {
        y = Util.dup(y);

        double a = (2 * y[width] - sum) / (width + 1);
        double b = y[width] - a * width;

        for (int x = 0; x < width; x++)
            y[x] = a * x + b;

        return y;
    }
}