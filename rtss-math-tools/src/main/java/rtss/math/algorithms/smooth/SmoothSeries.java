package rtss.math.algorithms.smooth;

import java.util.Arrays;

/*
 * Использование:
 * 
 *     double[] s1 = SeriesSmoothing.smoothCenteredMovingAverage(src, 5);
 *     double[] s2 = SeriesSmoothing.smoothMedianThenAverage(src, 3, 5);
 *     double[] s3 = SeriesSmoothing.smoothWhittaker(src, 50.0, null);
 * 
 * Для смертности с эпидемическими годами:
 * 
 *     double[] weights = new double[src.length];
 *     Arrays.fill(weights, 1.0);
 *     // Например, если startYear = 1900:
 *     weights[1918 - startYear] = 0.1;
 *     weights[1919 - startYear] = 0.1;
 *     weights[1925 - startYear] = 0.1;
 *     double[] baselineCdr = SeriesSmoothing.smoothWhittaker(src, 100.0, weights);
 *   
 * CBR: lambda умеренная.
 * Для CBR стоит начинать с lambda = 20..50.
 *  
 * CDR: lambda сильнее, но с пониженными весами для эпидемических лет.  
 * Для CDR — начинать с lambda = 50..200, особенно если нужно выделить базовый тренд.   
 */
public class SmoothSeries
{
    /*
     * 5-летнее центрированное среднее:
     *
     * dst[i] = average(src[i-2], src[i-1], src[i], src[i+1], src[i+2])
     *
     * На краях используется укороченное окно.
     * NaN игнорируются.
     */
    public static double[] smoothCenteredMovingAverage(double[] src, int window)
    {
        checkOddWindow(window);

        int n = src.length;
        double[] dst = new double[n];
        int radius = window / 2;

        for (int i = 0; i < n; i++)
            dst[i] = centeredMean(src, i, radius);

        return dst;
    }

    /*
     * Медиана + среднее:
     *
     * 1) сначала 3-летняя или другая центрированная медиана;
     * 2) затем 5-летнее или другое центрированное среднее.
     *
     * Для варианта "3-летняя медиана + 5-летнее среднее":
     *
     *     smoothMedianThenAverage(src, 3, 5)
     *
     * На краях используются укороченные окна.
     * NaN игнорируются.
     */
    public static double[] smoothMedianThenAverage(double[] src, int medianWindow, int averageWindow)
    {
        checkOddWindow(medianWindow);
        checkOddWindow(averageWindow);

        int n = src.length;
        double[] med = new double[n];
        int medianRadius = medianWindow / 2;

        for (int i = 0; i < n; i++)
            med[i] = centeredMedian(src, i, medianRadius);

        return smoothCenteredMovingAverage(med, averageWindow);
    }

    /*
     * Whittaker smoothing: 
    *
     * Минимизирует:
     *
     *     sum_i weights[i] * (src[i] - z[i])^2
     *       + lambda * sum_i (z[i] - 2*z[i+1] + z[i+2])^2
     *
     * lambda:
     *   0      => без сглаживания
     *   больше => более гладкая линия
     *
     * weights:
     *   null   => все веса = 1
     *   1.0    => обычный год
     *   0.1    => год учитывается слабо, например эпидемический пик
     *   0.0    => год фактически исключён из привязки
     *
     * NaN в src автоматически получает вес 0.
     */
    public static double[] smoothWhittaker(double[] src, double lambda, double[] weights)
    {
        if (lambda < 0)
            throw new IllegalArgumentException("lambda must be >= 0");

        int n = src.length;

        if (weights != null && weights.length != n)
            throw new IllegalArgumentException("weights.length != src.length");

        if (n == 0)
            return new double[0];

        double[][] a = new double[n][n];
        double[] b = new double[n];

        for (int i = 0; i < n; i++)
        {
            double y = src[i];
            double w = weights == null ? 1.0 : weights[i];

            if (Double.isNaN(y))
                w = 0.0;

            if (w < 0)
                throw new IllegalArgumentException("weights[" + i + "] < 0");

            a[i][i] += w;
            b[i] = Double.isNaN(y) ? 0.0 : w * y;
        }

        /*
         * Добавляем lambda * D'D,
         * где D — матрица вторых разностей:
         *
         *     z[i] - 2*z[i+1] + z[i+2]
         */
        for (int i = 0; i <= n - 3; i++)
        {
            int i0 = i;
            int i1 = i + 1;
            int i2 = i + 2;

            addPenalty(a, lambda, i0, i0, 1.0);
            addPenalty(a, lambda, i0, i1, -2.0);
            addPenalty(a, lambda, i0, i2, 1.0);

            addPenalty(a, lambda, i1, i0, -2.0);
            addPenalty(a, lambda, i1, i1, 4.0);
            addPenalty(a, lambda, i1, i2, -2.0);

            addPenalty(a, lambda, i2, i0, 1.0);
            addPenalty(a, lambda, i2, i1, -2.0);
            addPenalty(a, lambda, i2, i2, 1.0);
        }

        return solveLinearSystem(a, b);
    }

    private static void addPenalty(double[][] a, double lambda, int row, int col, double value)
    {
        a[row][col] += lambda * value;
    }

    private static void checkOddWindow(int window)
    {
        if (window <= 0)
            throw new IllegalArgumentException("window must be positive");

        if (window % 2 == 0)
            throw new IllegalArgumentException("window must be odd");
    }

    private static double centeredMean(double[] src, int center, int radius)
    {
        int n = src.length;

        int from = Math.max(0, center - radius);
        int to = Math.min(n - 1, center + radius);

        double sum = 0.0;
        int count = 0;

        for (int i = from; i <= to; i++)
        {
            double v = src[i];

            if (!Double.isNaN(v))
            {
                sum += v;
                count++;
            }
        }

        return count == 0 ? Double.NaN : sum / count;
    }

    private static double centeredMedian(double[] src, int center, int radius)
    {
        int n = src.length;

        int from = Math.max(0, center - radius);
        int to = Math.min(n - 1, center + radius);

        double[] tmp = new double[to - from + 1];
        int count = 0;

        for (int i = from; i <= to; i++)
        {
            double v = src[i];

            if (!Double.isNaN(v))
                tmp[count++] = v;
        }

        if (count == 0)
            return Double.NaN;

        Arrays.sort(tmp, 0, count);

        if (count % 2 == 1)
            return tmp[count / 2];
        else
            return (tmp[count / 2 - 1] + tmp[count / 2]) / 2.0;
    }

    /*
     * Обычный Gaussian elimination with partial pivoting.
     * Для рядов длиной 50-150 лет этого более чем достаточно.
     */
    private static double[] solveLinearSystem(double[][] a, double[] b)
    {
        int n = b.length;

        for (int p = 0; p < n; p++)
        {
            int pivot = p;
            double max = Math.abs(a[p][p]);

            for (int i = p + 1; i < n; i++)
            {
                double v = Math.abs(a[i][p]);

                if (v > max)
                {
                    max = v;
                    pivot = i;
                }
            }

            if (max < 1e-14)
                throw new ArithmeticException("Singular or nearly singular matrix");

            if (pivot != p)
            {
                double[] row = a[p];
                a[p] = a[pivot];
                a[pivot] = row;

                double tb = b[p];
                b[p] = b[pivot];
                b[pivot] = tb;
            }

            for (int i = p + 1; i < n; i++)
            {
                double factor = a[i][p] / a[p][p];

                if (factor == 0.0)
                    continue;

                a[i][p] = 0.0;

                for (int j = p + 1; j < n; j++)
                    a[i][j] -= factor * a[p][j];

                b[i] -= factor * b[p];
            }
        }

        double[] x = new double[n];

        for (int i = n - 1; i >= 0; i--)
        {
            double sum = b[i];

            for (int j = i + 1; j < n; j++)
                sum -= a[i][j] * x[j];

            x[i] = sum / a[i][i];
        }

        return x;
    }
}