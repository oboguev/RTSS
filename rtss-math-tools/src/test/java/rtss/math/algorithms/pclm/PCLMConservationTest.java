package rtss.math.algorithms.pclm;

import rtss.data.bin.Bin;

/**
 * Simple verification test for PCLM conservation property.
 */
public class PCLMConservationTest
{
    public static void main(String[] args) throws Exception
    {
        System.out.println("PCLM Conservation Test\n");

        // Create bins where avg represents actual counts (deaths, for example)
        Bin[] bins = {
                       new Bin(0, 4, 50.0), // 50 deaths per year in age group 0-4
                       new Bin(5, 9, 20.0), // 20 deaths per year in age group 5-9
                       new Bin(10, 14, 15.0), // 15 deaths per year in age group 10-14
                       new Bin(15, 19, 25.0), // 25 deaths per year in age group 15-19
                       new Bin(20, 24, 30.0) // 30 deaths per year in age group 20-24
        };

        double lambda = 1.0;
        int ppy = 1;

        PCLM pclm = new PCLM(bins, lambda, ppy);
        double[] result = pclm.pclm();

        System.out.println("Input bins (counts per year):");
        double totalInput = 0.0;
        for (Bin bin : bins)
        {
            double binTotal = bin.avg * bin.widths_in_years;
            totalInput += binTotal;
            System.out.printf("  Age %d-%d: %.2f per year, total=%.2f%n",
                              bin.age_x1, bin.age_x2, bin.avg, binTotal);
        }
        System.out.printf("Total input: %.2f%n%n", totalInput);

        System.out.println("Conservation check (bin by bin):");
        int firstAge = bins[0].age_x1;

        double totalOutput = 0.0;
        for (Bin bin : bins)
        {
            int startIdx = (bin.age_x1 - firstAge) * ppy;
            int endIdx = (bin.age_x2 - firstAge + 1) * ppy;

            double sumDisaggregated = 0.0;
            for (int j = startIdx; j < endIdx && j < result.length; j++)
            {
                sumDisaggregated += result[j];
            }

            double expected = bin.avg * bin.widths_in_years * ppy;
            double ratio = sumDisaggregated / expected;

            totalOutput += sumDisaggregated;

            System.out.printf("  Bin %d-%d: expected=%.4f, disaggregated=%.4f, ratio=%.6f%n",
                              bin.age_x1, bin.age_x2, expected, sumDisaggregated, ratio);
        }

        System.out.printf("%nTotal output: %.4f%n", totalOutput);
        System.out.printf("Total ratio: %.6f%n", totalOutput / totalInput);
        System.out.printf("Conservation error: %.6f%%%n", Math.abs(1.0 - totalOutput / totalInput) * 100);

        // Show some disaggregated values
        System.out.println("\nSample disaggregated values:");
        int[] sampleAges = { 0, 1, 5, 10, 15, 20, 24 };
        for (int age : sampleAges)
        {
            int idx = (age - firstAge) * ppy;
            if (idx >= 0 && idx < result.length)
            {
                System.out.printf("  Age %d: %.4f%n", age, result[idx]);
            }
        }

        // Test with different lambda values
        System.out.println("\n\nTesting different lambda values:");
        double[] lambdas = { 0.1, 1.0, 10.0, 100.0 };
        for (double lam : lambdas)
        {
            PCLM pclm2 = new PCLM(bins, lam, ppy);
            double[] result2 = pclm2.pclm();

            double total = 0.0;
            for (double v : result2)
            {
                total += v;
            }

            System.out.printf("  lambda=%.1f: total=%.4f, ratio=%.6f%n",
                              lam, total, total / totalInput);
        }
    }
}
