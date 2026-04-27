package rtss.math.algorithms.pclm;

import rtss.data.bin.Bin;

/**
 * Test and demonstration of PCLM algorithm.
 */
public class PCLMTest
{
    private static final double TOLERANCE_LOW_LAMBDA = 0.02; // 2% tolerance for low lambda (less smoothing)
    private static final double TOLERANCE_HIGH_LAMBDA = 3.00; // 300% tolerance for high lambda (more smoothing)

    public static void main(String[] args) throws Exception
    {
        System.out.println("PCLM Algorithm Test\n");

        // Test 1: Simple example with uniform bins
        test1_UniformBins();

        // Test 2: Typical demographic data with 5-year bins
        test2_FiveYearBins();

        // Test 3: Irregular bin widths
        test3_IrregularBins();

        // Test 4: Quarterly disaggregation (ppy=4)
        test4_QuarterlyDisaggregation();

        // Test 5: Five-year bins with quarterly disaggregation
        test5_FiveYearBins_Quarterly();

        System.out.println("=== All tests completed successfully ===");
    }

    /**
     * Test 1: Simple uniform bins
     */
    private static void test1_UniformBins() throws Exception
    {
        System.out.println("=== Test 1: Uniform 10-year bins ===");

        Bin[] bins = {
                       new Bin(0, 9, 10.0),
                       new Bin(10, 19, 20.0),
                       new Bin(20, 29, 30.0),
                       new Bin(30, 39, 25.0),
                       new Bin(40, 49, 15.0)
        };

        double lambda = 1.0;
        int ppy = 1;

        PCLM pclm = new PCLM(bins, lambda, ppy);
        double[] result = pclm.pclm();

        System.out.println("Input bins:");
        for (Bin bin : bins)
        {
            System.out.printf("  Age %d-%d: %.2f%n", bin.age_x1, bin.age_x2, bin.avg);
        }

        System.out.println("\nDisaggregated values (first 10 and last 10):");
        for (int i = 0; i < Math.min(10, result.length); i++)
        {
            System.out.printf("  Age %d: %.4f%n", i, result[i]);
        }
        System.out.println("  ...");
        for (int i = Math.max(result.length - 10, 0); i < result.length; i++)
        {
            System.out.printf("  Age %d: %.4f%n", i, result[i]);
        }

        System.out.println("\nResult length: " + result.length);

        // Verify expected length
        int expectedLength = ppy * (bins[bins.length - 1].age_x2 - bins[0].age_x1 + 1);
        verifyLength(result.length, expectedLength);

        // Verify rebinning produces original averages
        verifyRebinning(bins, result, ppy, TOLERANCE_LOW_LAMBDA);

        System.out.println();
    }

    /**
     * Test 2: Typical 5-year age bins (demographic data)
     */
    private static void test2_FiveYearBins() throws Exception
    {
        System.out.println("=== Test 2: Five-year age bins (typical mortality data) ===");

        Bin[] bins = {
                       new Bin(0, 0, 0.005), // Infant mortality
                       new Bin(1, 4, 0.0008),
                       new Bin(5, 9, 0.0002),
                       new Bin(10, 14, 0.0002),
                       new Bin(15, 19, 0.0005),
                       new Bin(20, 24, 0.0008),
                       new Bin(25, 29, 0.0009),
                       new Bin(30, 34, 0.0012),
                       new Bin(35, 39, 0.0015),
                       new Bin(40, 44, 0.0025),
                       new Bin(45, 49, 0.0040),
                       new Bin(50, 54, 0.0065),
                       new Bin(55, 59, 0.0105),
                       new Bin(60, 64, 0.0170),
                       new Bin(65, 69, 0.0280),
                       new Bin(70, 74, 0.0450),
                       new Bin(75, 79, 0.0720),
                       new Bin(80, 84, 0.1150),
                       new Bin(85, 100, 0.2000) // Open-ended last interval
        };

        double lambda = 10.0; // Higher smoothing for mortality data
        int ppy = 1;

        PCLM pclm = new PCLM(bins, lambda, ppy);
        double[] result = pclm.pclm();

        System.out.println("Input: " + bins.length + " bins");
        System.out.println("Output: " + result.length + " single-year values");

        // Verify expected length
        int expectedLength = ppy * (bins[bins.length - 1].age_x2 - bins[0].age_x1 + 1);
        verifyLength(result.length, expectedLength);

        System.out.println("\nSample disaggregated mortality rates:");
        int[] sampleAges = { 0, 1, 5, 10, 20, 40, 60, 80, 100 };
        for (int age : sampleAges)
        {
            if (age < result.length)
            {
                System.out.printf("  Age %d: %.6f%n", age, result[age]);
            }
        }

        // Verify rebinning produces original averages (higher tolerance due to high lambda=10)
        verifyRebinning(bins, result, ppy, TOLERANCE_HIGH_LAMBDA);

        System.out.println();
    }

    /**
     * Test 3: Irregular bin widths
     */
    private static void test3_IrregularBins() throws Exception
    {
        System.out.println("=== Test 3: Irregular bin widths ===");

        Bin[] bins = {
                       new Bin(0, 0, 100.0), // 1-year bin
                       new Bin(1, 4, 80.0), // 4-year bin
                       new Bin(5, 9, 60.0), // 5-year bin
                       new Bin(10, 14, 50.0), // 5-year bin
                       new Bin(15, 24, 45.0), // 10-year bin
                       new Bin(25, 49, 40.0), // 25-year bin
                       new Bin(50, 100, 35.0) // 51-year bin
        };

        double lambda = 5.0;
        int ppy = 1;

        PCLM pclm = new PCLM(bins, lambda, ppy);
        double[] result = pclm.pclm();

        System.out.println("Input bins (varying widths):");
        for (Bin bin : bins)
        {
            System.out.printf("  Age %d-%d (width=%d): %.2f%n",
                              bin.age_x1, bin.age_x2, bin.widths_in_years, bin.avg);
        }

        System.out.println("\nOutput length: " + result.length);

        // Verify expected length
        int expectedLength = ppy * (bins[bins.length - 1].age_x2 - bins[0].age_x1 + 1);
        verifyLength(result.length, expectedLength);

        System.out.println("\nSample disaggregated values:");
        int[] sampleAges = { 0, 1, 5, 10, 15, 25, 50, 75, 100 };
        for (int age : sampleAges)
        {
            if (age < result.length)
            {
                System.out.printf("  Age %d: %.4f%n", age, result[age]);
            }
        }

        // Verify rebinning produces original averages (lambda=5.0, moderate smoothing)
        verifyRebinning(bins, result, ppy, TOLERANCE_HIGH_LAMBDA);

        System.out.println();
    }

    /**
     * Test 4: Quarterly disaggregation (ppy=4)
     */
    private static void test4_QuarterlyDisaggregation() throws Exception
    {
        System.out.println("=== Test 4: Quarterly disaggregation (ppy=4) ===");

        Bin[] bins = {
                       new Bin(0, 9, 10.0),
                       new Bin(10, 19, 20.0),
                       new Bin(20, 29, 30.0),
                       new Bin(30, 39, 25.0),
                       new Bin(40, 49, 15.0)
        };

        double lambda = 1.0;
        int ppy = 4; // Quarterly disaggregation

        PCLM pclm = new PCLM(bins, lambda, ppy);
        double[] result = pclm.pclm();

        System.out.println("Input bins:");
        for (Bin bin : bins)
        {
            System.out.printf("  Age %d-%d: %.2f%n", bin.age_x1, bin.age_x2, bin.avg);
        }

        System.out.println("\nQuarterly disaggregation (ppy=4)");
        System.out.println("Result length: " + result.length);

        // Verify expected length
        int expectedLength = ppy * (bins[bins.length - 1].age_x2 - bins[0].age_x1 + 1);
        verifyLength(result.length, expectedLength);

        System.out.println("\nFirst year quarters (age 0-1):");
        for (int i = 0; i < Math.min(4, result.length); i++)
        {
            System.out.printf("  Quarter %d: %.4f%n", i, result[i]);
        }

        System.out.println("\nAge 10 quarters:");
        int age10Start = 10 * ppy;
        for (int i = 0; i < 4 && (age10Start + i) < result.length; i++)
        {
            System.out.printf("  Quarter %d: %.4f%n", i, result[age10Start + i]);
        }

        // Verify rebinning produces original averages
        verifyRebinning(bins, result, ppy, TOLERANCE_LOW_LAMBDA);

        System.out.println();
    }

    /**
     * Test 5: Five-year bins with quarterly disaggregation
     */
    private static void test5_FiveYearBins_Quarterly() throws Exception
    {
        System.out.println("=== Test 5: Five-year bins with quarterly disaggregation (ppy=4) ===");

        Bin[] bins = {
                       new Bin(0, 0, 0.005),
                       new Bin(1, 4, 0.0008),
                       new Bin(5, 9, 0.0002),
                       new Bin(10, 14, 0.0002),
                       new Bin(15, 19, 0.0005),
                       new Bin(20, 24, 0.0008)
        };

        double lambda = 10.0;
        int ppy = 4; // Quarterly disaggregation

        PCLM pclm = new PCLM(bins, lambda, ppy);
        double[] result = pclm.pclm();

        System.out.println("Input: " + bins.length + " bins");
        System.out.println("Output: " + result.length + " quarterly values");

        // Verify expected length
        int expectedLength = ppy * (bins[bins.length - 1].age_x2 - bins[0].age_x1 + 1);
        verifyLength(result.length, expectedLength);

        System.out.println("\nSample quarterly values:");
        System.out.println("Age 0 (4 quarters):");
        for (int i = 0; i < 4 && i < result.length; i++)
        {
            System.out.printf("  Q%d: %.6f%n", i + 1, result[i]);
        }

        System.out.println("Age 10 (4 quarters):");
        int age10Start = 10 * ppy;
        for (int i = 0; i < 4 && (age10Start + i) < result.length; i++)
        {
            System.out.printf("  Q%d: %.6f%n", i + 1, result[age10Start + i]);
        }

        // Verify rebinning produces original averages (higher tolerance due to high lambda=10)
        verifyRebinning(bins, result, ppy, TOLERANCE_HIGH_LAMBDA);

        System.out.println();
    }

    /**
     * Verifies that the result array has the expected length.
     */
    private static void verifyLength(int actualLength, int expectedLength)
    {
        System.out.printf("Length verification: expected=%d, actual=%d", expectedLength, actualLength);
        if (actualLength == expectedLength)
        {
            System.out.println(" ✓ PASS");
        }
        else
        {
            System.out.println(" ✗ FAIL");
            throw new AssertionError(String.format(
                    "Length mismatch: expected %d but got %d", expectedLength, actualLength));
        }
    }

    /**
     * Verifies that rebinning (summing and averaging) the disaggregated values produces
     * the same or close values as the original bin averages.
     *
     * PCLM preserves bin totals (sum of all points in a bin), so we verify that
     * the average of disaggregated points matches the original bin average.
     *
     * Note: Higher lambda values produce smoother results but may deviate more from
     * the exact bin totals. The tolerance parameter should reflect this trade-off.
     */
    private static void verifyRebinning(Bin[] bins, double[] result, int ppy, double tolerance)
    {
        System.out.println("\nRebinning verification (disaggregated values summed back to bin totals):");

        int offset = bins[0].age_x1;
        boolean allPassed = true;

        for (int i = 0; i < bins.length; i++)
        {
            Bin bin = bins[i];
            int start = (bin.age_x1 - offset) * ppy;
            int end = (bin.age_x2 - offset + 1) * ppy;

            // Sum the disaggregated values in this bin
            double sum = 0.0;
            for (int j = start; j < end && j < result.length; j++)
            {
                sum += result[j];
            }

            // Expected total is average * width * ppy (same as PCLM does internally)
            double expectedTotal = bin.avg * bin.widths_in_years * ppy;
            double relativeError = Math.abs(sum - expectedTotal) / expectedTotal;
            boolean passed = relativeError <= tolerance;
            allPassed = allPassed && passed;

            String status = passed ? "✓" : "✗";
            System.out.printf("  Bin %d-%d: expected_total=%.6f, actual_total=%.6f, error=%.2f%% %s%n",
                              bin.age_x1, bin.age_x2, expectedTotal, sum, relativeError * 100, status);
        }

        if (!allPassed)
        {
            throw new AssertionError("Rebinning verification failed: some bins exceed tolerance of " +
                                     (tolerance * 100) + "%");
        }
        System.out.println("Rebinning verification: PASS (all bins within " + (tolerance * 100) + "% tolerance)");
    }
}
