package rtss.math.algorithms.pclm;

import rtss.data.bin.Bin;

/**
 * Conservation property verification for exposure-aware PCLM.
 *
 * This test verifies that the ExposuresPCLM algorithm correctly maintains
 * the exposure-weighted conservation property across various scenarios.
 */
public class ExposuresPCLMConservationTest
{
    public static void main(String[] args) throws Exception
    {
        System.out.println("ExposuresPCLM Conservation Test\n");
        System.out.println("Verifying exposure-weighted conservation property:");
        System.out.println("  For each bin i: sum(rate[j] * exposure[j]) ≈ bin.avg * sum(exposure[j])");
        System.out.println();

        // Test 1: Standard bins with realistic population
        test1_StandardConservation();

        // Test 2: Wide age bins (most challenging case)
        test2_WideAgeBins();

        // Test 3: Different lambda values
        test3_DifferentLambdaValues();

        // Test 4: Quarterly disaggregation
        test4_QuarterlyConservation();

        // Test 5: Edge case - very low exposures in some ranges
        test5_LowExposureRanges();

        System.out.println("\n=== All conservation tests passed ===");
    }

    /**
     * Test 1: Standard demographic bins with realistic population.
     */
    private static void test1_StandardConservation() throws Exception
    {
        System.out.println("=== Test 1: Standard demographic bins ===");

        Bin[] bins = {
                new Bin(0, 0, 0.00550),
                new Bin(1, 4, 0.00025),
                new Bin(5, 9, 0.00012),
                new Bin(10, 14, 0.00015),
                new Bin(15, 19, 0.00045),
                new Bin(20, 24, 0.00065),
                new Bin(25, 29, 0.00070),
                new Bin(30, 34, 0.00080),
                new Bin(35, 39, 0.00100),
                new Bin(40, 44, 0.00150),
                new Bin(45, 49, 0.00230),
                new Bin(50, 54, 0.00370),
                new Bin(55, 59, 0.00590),
                new Bin(60, 64, 0.00950),
                new Bin(65, 69, 0.01550),
                new Bin(70, 74, 0.02550),
                new Bin(75, 79, 0.04200),
                new Bin(80, 84, 0.06900),
                new Bin(85, 100, 0.15000)
        };

        double lambda = 10.0;
        int ppy = 1;

        double[] exposures = generateRealisticPopulation(0, 100, ppy);

        ExposuresPCLM pclm = new ExposuresPCLM(bins, exposures, lambda, ppy);
        double[] result = pclm.pclm();

        // Calculate total input and output deaths
        double totalInputDeaths = 0.0;
        double totalOutputDeaths = 0.0;

        int offset = bins[0].age_x1;

        System.out.println("\nBin-by-bin conservation:");
        System.out.println("Bin        Expected Deaths    Actual Deaths      Error %");
        System.out.println("---------------------------------------------------------------");

        for (Bin bin : bins)
        {
            int start = (bin.age_x1 - offset) * ppy;
            int end = (bin.age_x2 - offset + 1) * ppy;

            // Calculate exposure sum in this bin
            double exposureSum = 0.0;
            for (int j = start; j < end && j < exposures.length; j++)
            {
                exposureSum += exposures[j];
            }

            // Expected deaths from bin average
            double expectedDeaths = bin.avg * exposureSum;
            totalInputDeaths += expectedDeaths;

            // Actual deaths from disaggregated rates
            double actualDeaths = 0.0;
            for (int j = start; j < end && j < result.length; j++)
            {
                actualDeaths += result[j] * exposures[j];
            }
            totalOutputDeaths += actualDeaths;

            double error = 100.0 * (actualDeaths - expectedDeaths) / expectedDeaths;

            System.out.printf("%3d-%-3d    %,12.2f         %,12.2f         %+6.3f%%%n",
                    bin.age_x1, bin.age_x2, expectedDeaths, actualDeaths, error);
        }

        System.out.println("---------------------------------------------------------------");
        System.out.printf("TOTAL      %,12.2f         %,12.2f         %+6.3f%%%n",
                totalInputDeaths, totalOutputDeaths,
                100.0 * (totalOutputDeaths - totalInputDeaths) / totalInputDeaths);

        double totalError = Math.abs(totalOutputDeaths - totalInputDeaths) / totalInputDeaths;
        if (totalError > 0.02) // 2% tolerance
        {
            throw new AssertionError("Total conservation error exceeds 2%");
        }

        System.out.println("\n✓ Conservation property verified\n");
    }

    /**
     * Test 2: Wide age bins - the most challenging case for conservation.
     */
    private static void test2_WideAgeBins() throws Exception
    {
        System.out.println("=== Test 2: Wide age bins (challenging case) ===");

        Bin[] bins = {
                new Bin(0, 24, 0.00100),   // 25-year bin
                new Bin(25, 49, 0.00150),  // 25-year bin
                new Bin(50, 74, 0.01500),  // 25-year bin
                new Bin(75, 100, 0.08000)  // 26-year bin with steep mortality increase
        };

        double lambda = 5.0;
        int ppy = 1;

        double[] exposures = generateRealisticPopulation(0, 100, ppy);

        ExposuresPCLM pclm = new ExposuresPCLM(bins, exposures, lambda, ppy);
        double[] result = pclm.pclm();

        verifyConservation(bins, result, exposures, ppy, "wide bins");

        System.out.println();
    }

    /**
     * Test 3: Conservation across different lambda (smoothing) values.
     */
    private static void test3_DifferentLambdaValues() throws Exception
    {
        System.out.println("=== Test 3: Different lambda values ===");

        Bin[] bins = {
                new Bin(60, 64, 0.00950),
                new Bin(65, 69, 0.01550),
                new Bin(70, 74, 0.02550),
                new Bin(75, 79, 0.04200),
                new Bin(80, 84, 0.06900),
                new Bin(85, 100, 0.15000)
        };

        int ppy = 1;
        double[] exposures = generateRealisticPopulation(60, 100, ppy);

        double[] lambdaValues = {0.1, 1.0, 10.0, 50.0, 100.0};

        System.out.println("Lambda    Total Expected    Total Actual      Error %");
        System.out.println("-----------------------------------------------------------");

        for (double lambda : lambdaValues)
        {
            ExposuresPCLM pclm = new ExposuresPCLM(bins, exposures, lambda, ppy);
            double[] result = pclm.pclm();

            // Calculate totals
            double expectedTotal = 0.0;
            double actualTotal = 0.0;

            int offset = bins[0].age_x1;
            for (Bin bin : bins)
            {
                int start = (bin.age_x1 - offset) * ppy;
                int end = (bin.age_x2 - offset + 1) * ppy;

                double exposureSum = 0.0;
                for (int j = start; j < end && j < exposures.length; j++)
                {
                    exposureSum += exposures[j];
                }

                expectedTotal += bin.avg * exposureSum;

                for (int j = start; j < end && j < result.length; j++)
                {
                    actualTotal += result[j] * exposures[j];
                }
            }

            double error = 100.0 * (actualTotal - expectedTotal) / expectedTotal;
            System.out.printf("%6.1f    %,12.2f         %,12.2f         %+6.3f%%%n",
                    lambda, expectedTotal, actualTotal, error);

            if (Math.abs(error) > 2.0)
            {
                throw new AssertionError("Conservation error exceeds 2% for lambda=" + lambda);
            }
        }

        System.out.println("\n✓ Conservation maintained across all lambda values\n");
    }

    /**
     * Test 4: Quarterly disaggregation (ppy=4).
     */
    private static void test4_QuarterlyConservation() throws Exception
    {
        System.out.println("=== Test 4: Quarterly disaggregation (ppy=4) ===");

        Bin[] bins = {
                new Bin(0, 0, 0.00550),
                new Bin(1, 4, 0.00025),
                new Bin(5, 9, 0.00012),
                new Bin(10, 19, 0.00020),
                new Bin(20, 29, 0.00070)
        };

        int ppy = 4;
        double lambda = 5.0;

        double[] exposures = generateRealisticPopulation(0, 29, ppy);

        ExposuresPCLM pclm = new ExposuresPCLM(bins, exposures, lambda, ppy);
        double[] result = pclm.pclm();

        System.out.printf("Generated %d quarterly rates from %d annual bins%n",
                result.length, bins.length);

        verifyConservation(bins, result, exposures, ppy, "quarterly");

        System.out.println();
    }

    /**
     * Test 5: Edge case with very low exposures in some age ranges.
     */
    private static void test5_LowExposureRanges() throws Exception
    {
        System.out.println("=== Test 5: Low exposure ranges (edge case) ===");

        Bin[] bins = {
                new Bin(90, 94, 0.18000),
                new Bin(95, 100, 0.30000)
        };

        int ppy = 1;
        double lambda = 1.0;

        // Generate population with very low numbers at advanced ages
        double[] exposures = new double[11]; // ages 90-100
        for (int i = 0; i < 11; i++)
        {
            int age = 90 + i;
            // Exponential decline - very few people at age 100
            exposures[i] = 10000.0 * Math.exp(-0.15 * (age - 90));
        }

        System.out.println("Population at advanced ages:");
        for (int i = 0; i < exposures.length; i++)
        {
            System.out.printf("  Age %d: %,.0f%n", 90 + i, exposures[i]);
        }

        ExposuresPCLM pclm = new ExposuresPCLM(bins, exposures, lambda, ppy);
        double[] result = pclm.pclm();

        System.out.println("\nDisaggregated mortality rates:");
        for (int i = 0; i < result.length; i++)
        {
            System.out.printf("  Age %d: %.6f (deaths: %.1f)%n",
                    90 + i, result[i], result[i] * exposures[i]);
        }

        verifyConservation(bins, result, exposures, ppy, "low exposure");

        System.out.println();
    }

    /**
     * Helper: Generate realistic population distribution.
     */
    private static double[] generateRealisticPopulation(int firstAge, int lastAge, int ppy)
    {
        int ageRange = lastAge - firstAge + 1;
        int nPoints = ageRange * ppy;
        double[] exposures = new double[nPoints];

        for (int i = 0; i < nPoints; i++)
        {
            double age = firstAge + (double) i / ppy;

            double population;
            if (age < 20)
            {
                population = 40000 + (age * 200);
            }
            else if (age < 65)
            {
                population = 45000 + ((age - 20) * 100);
            }
            else if (age < 85)
            {
                double yearsAfter65 = age - 65;
                population = 49500 - (yearsAfter65 * 500);
            }
            else
            {
                double yearsAfter85 = age - 85;
                population = 39500 * Math.exp(-0.08 * yearsAfter85);
            }

            exposures[i] = population;
        }

        return exposures;
    }

    /**
     * Helper: Verify conservation property.
     */
    private static void verifyConservation(Bin[] bins, double[] rates, double[] exposures,
                                           int ppy, String testName)
    {
        double totalExpected = 0.0;
        double totalActual = 0.0;
        double maxBinError = 0.0;

        int offset = bins[0].age_x1;

        for (Bin bin : bins)
        {
            int start = (bin.age_x1 - offset) * ppy;
            int end = (bin.age_x2 - offset + 1) * ppy;

            double exposureSum = 0.0;
            for (int j = start; j < end && j < exposures.length; j++)
            {
                exposureSum += exposures[j];
            }

            double expected = bin.avg * exposureSum;
            totalExpected += expected;

            double actual = 0.0;
            for (int j = start; j < end && j < rates.length; j++)
            {
                actual += rates[j] * exposures[j];
            }
            totalActual += actual;

            double binError = Math.abs(actual - expected) / expected;
            maxBinError = Math.max(maxBinError, binError);
        }

        double totalError = Math.abs(totalActual - totalExpected) / totalExpected;

        System.out.printf("Total expected deaths: %,.2f%n", totalExpected);
        System.out.printf("Total actual deaths:   %,.2f%n", totalActual);
        System.out.printf("Total error:           %.4f%% (%.6f)%n", totalError * 100, totalError);
        System.out.printf("Max bin error:         %.4f%%%n", maxBinError * 100);

        if (totalError > 0.02)
        {
            throw new AssertionError(String.format(
                    "Conservation test '%s' failed: total error %.4f%% exceeds 2%%",
                    testName, totalError * 100));
        }

        if (maxBinError > 0.05)
        {
            throw new AssertionError(String.format(
                    "Conservation test '%s' failed: max bin error %.4f%% exceeds 5%%",
                    testName, maxBinError * 100));
        }

        System.out.printf("✓ Conservation verified for %s (error < 2%%)%n", testName);
    }
}
