package my.test.example;

import java.util.Arrays;

import rtss.util.Util;

public class CA {
    
    public static double computePenalty(double[] series) {
        
        series = Util.normalize(series);
        
        int n = series.length;
        if (n < 3) {
            throw new IllegalArgumentException("Series must have at least 3 points");
        }

        // Compute second derivative
        double[] secondDerivative = new double[n - 2];
        for (int i = 1; i < n - 1; i++) {
            secondDerivative[i - 1] = series[i - 1] - 2 * series[i] + series[i + 1];
        }

        // Compute mean of second derivative
        double mean = Arrays.stream(secondDerivative).average().orElse(0.0);

        // Compute variance of second derivative
        double variance = Arrays.stream(secondDerivative)
                                .map(v -> (v - mean) * (v - mean))
                                .average()
                                .orElse(0.0);

        return variance;
    }
    
    public static double differentness_1(double[] p)
    {
        p = Util.normalize(p);
        int n = p.length;
        if (n == 0)
            return 0; // Edge case: empty array

        // Compute sum of absolute values
        double sumAbs = Util.sum(Util.abs(p));
        if (sumAbs == 0)
            return 0; // Edge case: all zeros

        // Compute sum of absolute pairwise differences
        double sumDiffs = 0.0;
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                sumDiffs += Math.abs(p[i] - p[j]);
            }
        }

        // Compute and return differentness metric
        return (sumDiffs / (2.0 * n * sumAbs));
    }

    public static void main(String[] args) {
        double[] S1 = {1366.0386210302956, 1359.7009059759328, 1356.4755068485572, 1353.374600358261,
                       1347.4103632151368, 1335.5949721292761, 1314.9406038107716, 1280.889187404156,
                       1192.5626602963498, 1035.0124966147866, 854.7625488891154, 701.2775835860147,
                       623.9091464970211, 655.1361546417107};

        double[] S2 = {1574.2284225026235, 1378.8747997244197, 1279.2413132219726, 1275.3277315741925,
                       1275.3277304193516, 1275.3277248239967, 1275.32770530434, 1263.9976797947363,
                       1240.3792504104267, 1103.9675599441323, 854.7625488891154, 701.2775835860147,
                       623.9091464970211, 655.1361546417107};

        System.out.println("Penalty for S1: " + computePenalty(S1));
        System.out.println("Penalty for S2: " + computePenalty(S2));
        
        System.out.println("differentness_1 foerS1: " + differentness_1(S1));
        System.out.println("differentness_1 for S2: " + differentness_1(S2));
    }
}
