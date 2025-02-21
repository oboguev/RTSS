package my.test.example;

import rtss.util.Util;

public class CB3
{
    public static void main(String[] args)
    {
        // Example data series S1 and S2
        double[] S1 = {
                        1366.0386210302956, 1359.7009059759328, 1356.4755068485572, 1353.374600358261,
                        1347.4103632151368, 1335.5949721292761, 1314.9406038107716, 1280.889187404156,
                        1192.5626602963498, 1035.0124966147866, 854.7625488891154, 701.2775835860147,
                        623.9091464970211, 655.1361546417107
        };

        double[] S2 = {
                        1574.2284225026235, 1378.8747997244197, 1279.2413132219726, 1275.3277315741925,
                        1275.3277304193516, 1275.3277248239967, 1275.32770530434, 1263.9976797947363,
                        1240.3792504104267, 1103.9675599441323, 854.7625488891154, 701.2775835860147,
                        623.9091464970211, 655.1361546417107
        };

        // Calculate penalty for S1 and S2
        double penaltyS1 = calculateSmoothnessPenalty(S1);
        double penaltyS2 = calculateSmoothnessPenalty(S2);

        System.out.println("Penalty for S1: " + penaltyS1);
        System.out.println("Penalty for S2: " + penaltyS2);
    }

    public static double calculateSmoothnessPenalty(double[] series)
    {
        series = Util.normalize(series);
        
        // Step 1: Compute the second derivative
        double[] secondDerivative = computeSecondDerivative(series);

        // Step 2: Compute the sum of absolute changes in the second derivative (third derivative)
        double penalty = 0;
        for (int i = 1; i < secondDerivative.length; i++)
        {
            penalty += Math.abs(secondDerivative[i] - secondDerivative[i - 1]);
        }

        return penalty;
    }

    private static double[] computeSecondDerivative(double[] series)
    {
        int n = series.length;
        double[] secondDerivative = new double[n - 2];

        for (int i = 1; i < n - 1; i++)
        {
            // Central difference approximation for the second derivative
            secondDerivative[i - 1] = series[i + 1] - 2 * series[i] + series[i - 1];
        }

        return secondDerivative;
    }
}