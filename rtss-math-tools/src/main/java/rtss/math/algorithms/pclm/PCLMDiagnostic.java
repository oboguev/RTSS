package rtss.math.algorithms.pclm;

import rtss.data.bin.Bin;

/**
 * Diagnostic utility to understand the difference between simple averaging
 * and exposure-weighted averaging in PCLM results.
 */
public class PCLMDiagnostic
{
    /**
     * Analyzes PCLM results and shows both simple average and exposure-weighted average
     * for each bin, to help understand conservation properties.
     */
    public static void analyzeBinConservation(Bin[] bins, double[] rates, double[] exposures, int ppy)
    {
        int firstAge = bins[0].age_x1;

        System.out.println("\nBin Conservation Analysis:");
        System.out.println("Bin      Input Rate   Simple Avg   Exp-Weighted   Simple Err   Exp-Wtd Err");
        System.out.println("--------------------------------------------------------------------------------");

        for (Bin bin : bins)
        {
            int start = (bin.age_x1 - firstAge) * ppy;
            int end = (bin.age_x2 - firstAge + 1) * ppy;

            // Calculate simple average of disaggregated rates
            double simpleSum = 0.0;
            int count = 0;
            for (int j = start; j < end && j < rates.length; j++)
            {
                simpleSum += rates[j];
                count++;
            }
            double simpleAvg = simpleSum / count;

            // Calculate exposure-weighted average
            double weightedSum = 0.0;
            double exposureSum = 0.0;
            for (int j = start; j < end && j < exposures.length; j++)
            {
                weightedSum += rates[j] * exposures[j];
                exposureSum += exposures[j];
            }
            double weightedAvg = weightedSum / exposureSum;

            // Calculate errors
            double simpleErr = 100.0 * (simpleAvg - bin.avg) / bin.avg;
            double weightedErr = 100.0 * (weightedAvg - bin.avg) / bin.avg;

            System.out.printf("%3d-%-3d   %10.5f   %10.5f   %10.5f      %+7.2f%%      %+7.2f%%%n",
                    bin.age_x1, bin.age_x2, bin.avg, simpleAvg, weightedAvg, simpleErr, weightedErr);
        }

        System.out.println("\nKey insight:");
        System.out.println("- 'Simple Avg' = arithmetic mean of disaggregated rates (what basic PCLM preserves)");
        System.out.println("- 'Exp-Weighted' = exposure-weighted mean (what ExposuresPCLM preserves)");
        System.out.println("- ExposuresPCLM should have near-zero 'Exp-Wtd Err', but may have non-zero 'Simple Err'");
    }

    /**
     * Shows the exposure distribution to understand how much weighting matters.
     */
    public static void showExposureDistribution(Bin[] bins, double[] exposures, int ppy)
    {
        int firstAge = bins[0].age_x1;

        System.out.println("\nExposure Distribution:");
        System.out.println("Bin      Total Exposure   Min Exposure   Max Exposure   Ratio (Max/Min)");
        System.out.println("-------------------------------------------------------------------------");

        for (Bin bin : bins)
        {
            int start = (bin.age_x1 - firstAge) * ppy;
            int end = (bin.age_x2 - firstAge + 1) * ppy;

            double total = 0.0;
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;

            for (int j = start; j < end && j < exposures.length; j++)
            {
                total += exposures[j];
                min = Math.min(min, exposures[j]);
                max = Math.max(max, exposures[j]);
            }

            double ratio = max / min;

            System.out.printf("%3d-%-3d   %,15.0f   %,13.0f   %,13.0f   %10.2f%n",
                    bin.age_x1, bin.age_x2, total, min, max, ratio);
        }

        System.out.println("\nNote: High ratios indicate non-uniform exposure distribution.");
        System.out.println("When ratio ≈ 1.0, simple and exposure-weighted averages are similar.");
        System.out.println("When ratio >> 1.0, exposure-weighting matters significantly.");
    }
}
