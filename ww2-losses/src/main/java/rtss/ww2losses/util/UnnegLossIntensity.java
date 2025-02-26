package rtss.ww2losses.util;

/*
 * Заменить отрицательные участки loss intensity на положительные, интерполировав
 * между точками близкими к крайним. 
 */
public class UnnegLossIntensity
{
    public static double[] unneg(double[] f, int XMAX, double thresholdFactor)
    {
        double[] result = f.clone();

        int x = 0;
        while (x < XMAX)
        {
            if (result[x] < 0)
            {
                // Identify start and end of the negative range
                int negStart = x;
                while (x < XMAX && result[x] < 0)
                    x++;
                int negEnd = x - 1;

                // Find left positive range
                int leftEnd = negStart - 1;
                while (leftEnd >= 0 && result[leftEnd] >= 0)
                    leftEnd--;
                int leftStart = leftEnd + 1;

                // Find right positive range
                int rightStart = negEnd + 1;
                while (rightStart < XMAX && result[rightStart] < 0)
                    rightStart++;
                int rightEnd = rightStart;
                while (rightEnd < XMAX && result[rightEnd] >= 0)
                    rightEnd++;
                rightEnd--;

                if (leftStart < leftEnd && rightStart < rightEnd)
                {
                    double leftAvg = average(result, leftStart, leftEnd);
                    double rightAvg = average(result, rightStart, rightEnd);

                    int interpStart = findThresholdPoint(result, leftStart, negStart, leftAvg * thresholdFactor);
                    int interpEnd = findThresholdPoint(result, negEnd, rightEnd, rightAvg * thresholdFactor);

                    linearInterpolation(result, interpStart, interpEnd);
                }
            }
            x++;
        }
        return result;
    }

    private static double average(double[] f, int start, int end)
    {
        double sum = 0;
        int count = 0;
        for (int i = start; i <= end; i++)
        {
            sum += f[i];
            count++;
        }
        return count > 0 ? sum / count : 0;
    }

    private static int findThresholdPoint(double[] f, int start, int end, double threshold)
    {
        for (int i = start; i <= end; i++)
        {
            if (f[i] >= threshold)
            {
                return i;
            }
        }
        return end; // Fallback to end if no threshold point is found
    }

    private static void linearInterpolation(double[] f, int start, int end)
    {
        double startVal = f[start];
        double endVal = f[end];
        for (int i = start; i <= end; i++)
        {
            double ratio = (double) (i - start) / (end - start);
            f[i] = startVal * (1 - ratio) + endVal * ratio;
        }
    }
}
