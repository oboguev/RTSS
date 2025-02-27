package rtss.ww2losses.util;

import rtss.data.selectors.Gender;
import rtss.util.Util;

/*
 * Заменить отрицательные участки loss intensity на положительные, интерполировав
 * между точками близкими к крайним. 
 */
public class UnnegLossIntensity
{
    public static double[] unneg(double[] f, int XMAX, double thresholdFactor, Gender gender)
    {
        double[] result = f.clone();

        for (int x = 0; x < XMAX; x++)
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
                int leftStart = leftEnd;
                while (leftStart > 0 && result[leftStart - 1] >= 0)
                    leftStart--;

                // Find right positive range
                int rightStart = negEnd + 1;
                while (rightStart < XMAX && result[rightStart] < 0)
                    rightStart++;
                int rightEnd = rightStart;
                while (rightEnd < XMAX && result[rightEnd] >= 0)
                    rightEnd++;
                rightEnd--;

                if (leftEnd >= 0 || rightStart < rightEnd)
                { 
                    // Ensure at least one valid positive range
                    double leftAvg = (leftEnd >= 0) ? average(result, leftStart, leftEnd) : result[rightStart];
                    double rightAvg = (rightStart < rightEnd) ? average(result, rightStart, rightEnd) : result[leftEnd];

                    int interpStart = findThresholdPointLeft(result, leftStart, negStart, leftAvg * thresholdFactor);
                    int interpEnd = findThresholdPointRight(result, negEnd, rightEnd, rightAvg * thresholdFactor);

                    linearInterpolation(result, interpStart, interpEnd);
                    
                    // Util.out(String.format("UnnegLossIntensity %s %d-%d", gender.name(), interpStart, interpEnd));
                }
            }
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

    private static int findThresholdPointLeft(double[] f, int start, int end, double threshold)
    {
        for (int i = end ; i >= start; i--)
        {
            if (f[i] >= threshold)
            {
                return i;
            }
        }
        return start; // Fallback if no threshold point is found
    }

    private static int findThresholdPointRight(double[] f, int start, int end, double threshold)
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
