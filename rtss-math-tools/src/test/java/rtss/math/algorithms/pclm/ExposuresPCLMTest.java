package rtss.math.algorithms.pclm;

import rtss.data.bin.Bin;

/**
 * Test and demonstration of exposure-aware PCLM algorithm.
 *
 * This test uses realistic population age distributions to demonstrate
 * how the exposure-aware PCLM handles mortality rate disaggregation.
 */
public class ExposuresPCLMTest
{
    private static final double TOLERANCE = 0.02; // 2% tolerance

    public static void main(String[] args) throws Exception
    {
        System.out.println("ExposuresPCLM Algorithm Test\n");
        System.out.println("This test demonstrates exposure-aware mortality rate disaggregation");
        System.out.println("using realistic population age distributions.\n");

        // Test 1: Standard demographic bins with realistic population
        test1_StandardDemographicBins();

        // Test 2: Wide senior age group (85-100) - the main use case
        test2_WideSeniorAgeGroup();

        // Test 3: Quarterly disaggregation with exposures
        test3_QuarterlyDisaggregation();

        // Test 4: Comparison with uniform exposure (should behave differently)
        test4_ComparisonWithUniformExposure();

        System.out.println("\n=== All tests completed successfully ===");
    }

    /**
     * Test 1: Standard demographic bins (0, 1-4, 5-9, ..., 80-84, 85-100)
     * with realistic U.S.-like population distribution.
     */
    private static void test1_StandardDemographicBins() throws Exception
    {
        System.out.println("=== Test 1: Standard demographic bins with realistic population ===");

        // Standard demographic bins with mortality rates
        Bin[] bins = {
                new Bin(0, 0, 0.00550),     // Infant mortality
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
                new Bin(85, 100, 0.15000)   // Wide open-ended interval
        };

        int ppy = 1;
        double lambda = 10.0;

        // Generate realistic population exposures (ages 0-100)
        double[] exposures = generateRealisticPopulation(0, 100, ppy);

        System.out.println("Population statistics:");
        printPopulationStats(exposures, ppy);

        // Run ExposuresPCLM
        ExposuresPCLM pclm = new ExposuresPCLM(bins, exposures, lambda, ppy);
        double[] result = pclm.pclm();

        System.out.println("\nDisaggregated mortality rates (sample ages):");
        int[] sampleAges = {0, 1, 10, 20, 40, 60, 80, 85, 90, 95, 100};
        for (int age : sampleAges)
        {
            if (age < result.length)
            {
                System.out.printf("  Age %3d: %.6f (exposure: %,.0f)%n",
                        age, result[age], exposures[age]);
            }
        }

        // Verify conservation property with exposures
        // Higher tolerance for high lambda (more smoothing = less exact conservation)
        System.out.println("\nConservation verification (exposure-weighted):");
        verifyExposureWeightedConservation(bins, result, exposures, ppy, 0.06); // 6% tolerance for lambda=10

        System.out.println();
    }

    /**
     * Test 2: Focus on wide senior age group (85-100) where exposure-weighting
     * makes the most difference.
     */
    private static void test2_WideSeniorAgeGroup() throws Exception
    {
        System.out.println("=== Test 2: Wide senior age group (85-100) ===");
        System.out.println("This test demonstrates why exposure-weighting matters for wide senior bins.\n");

        // Simple bins focusing on senior ages
        Bin[] bins = {
                new Bin(60, 64, 0.00950),
                new Bin(65, 69, 0.01550),
                new Bin(70, 74, 0.02550),
                new Bin(75, 79, 0.04200),
                new Bin(80, 84, 0.06900),
                new Bin(85, 100, 0.15000)   // Wide bin with 16 years
        };

        int ppy = 1;
        double lambda = 5.0;

        // Generate realistic population with sharp decline in 85+ range
        double[] exposures = generateRealisticPopulation(60, 100, ppy);

        System.out.println("Population in 85-100 age range:");
        System.out.println("Age    Population    % of 85-100 total");
        System.out.println("----------------------------------------");

        double total85plus = 0.0;
        for (int age = 85; age <= 100; age++)
        {
            total85plus += exposures[age - 60];
        }

        for (int age = 85; age <= 100; age += 5)
        {
            int idx = age - 60;
            if (idx < exposures.length)
            {
                double pct = 100.0 * exposures[idx] / total85plus;
                System.out.printf("%3d    %,10.0f    %5.1f%%%n", age, exposures[idx], pct);
            }
        }

        System.out.println("\nNotice: Population drops sharply with age in 85-100 range.");
        System.out.println("Exposure-weighting allows mortality rates to rise steeply while");
        System.out.println("maintaining the correct grouped average.\n");

        // Run ExposuresPCLM
        ExposuresPCLM pclm = new ExposuresPCLM(bins, exposures, lambda, ppy);
        double[] result = pclm.pclm();

        System.out.println("Disaggregated mortality rates in 85-100 range:");
        System.out.println("Age    Rate        Deaths");
        System.out.println("------------------------------");
        for (int age = 85; age <= 100; age += 3)
        {
            int idx = age - 60;
            if (idx < result.length)
            {
                double deaths = result[idx] * exposures[idx];
                System.out.printf("%3d    %.6f    %,.1f%n", age, result[idx], deaths);
            }
        }

        System.out.println("\nNotice: Mortality rate increases with age, but deaths per age");
        System.out.println("remain relatively balanced due to declining population.\n");

        // Verify conservation (lambda=5.0 is moderate, allow 4% tolerance)
        verifyExposureWeightedConservation(bins, result, exposures, ppy, 0.04);

        System.out.println();
    }

    /**
     * Test 3: Quarterly disaggregation (ppy=4) with exposures.
     */
    private static void test3_QuarterlyDisaggregation() throws Exception
    {
        System.out.println("=== Test 3: Quarterly disaggregation (ppy=4) ===");

        Bin[] bins = {
                new Bin(0, 0, 0.00550),
                new Bin(1, 4, 0.00025),
                new Bin(5, 9, 0.00012),
                new Bin(10, 14, 0.00015),
                new Bin(15, 19, 0.00045)
        };

        int ppy = 4; // Quarterly
        double lambda = 5.0;

        // Generate quarterly exposures
        double[] exposures = generateRealisticPopulation(0, 19, ppy);

        System.out.printf("Generated %d quarterly exposure values for ages 0-19%n", exposures.length);

        ExposuresPCLM pclm = new ExposuresPCLM(bins, exposures, lambda, ppy);
        double[] result = pclm.pclm();

        System.out.printf("Output: %d quarterly mortality rates%n", result.length);

        System.out.println("\nFirst year (age 0) quarterly rates:");
        for (int q = 0; q < 4; q++)
        {
            System.out.printf("  Q%d: %.6f%n", q + 1, result[q]);
        }

        System.out.println("\nAge 10 quarterly rates:");
        int age10Start = 10 * ppy;
        for (int q = 0; q < 4; q++)
        {
            System.out.printf("  Q%d: %.6f%n", q + 1, result[age10Start + q]);
        }

        verifyExposureWeightedConservation(bins, result, exposures, ppy, TOLERANCE);

        System.out.println();
    }

    /**
     * Test 4: Compare behavior with uniform vs. realistic exposures.
     * This demonstrates why exposure-weighting matters.
     */
    private static void test4_ComparisonWithUniformExposure() throws Exception
    {
        System.out.println("=== Test 4: Comparison - Realistic vs. Uniform Exposures ===");
        System.out.println("This shows the difference exposure-weighting makes.\n");

        Bin[] bins = {
                new Bin(80, 84, 0.06900),
                new Bin(85, 100, 0.15000)
        };

        int ppy = 1;
        double lambda = 1.0;

        // Realistic exposures: sharp decline with age
        double[] realisticExposures = generateRealisticPopulation(80, 100, ppy);

        // Uniform exposures: constant across all ages
        double[] uniformExposures = new double[21];
        double avgExposure = 0.0;
        for (double exp : realisticExposures)
        {
            avgExposure += exp;
        }
        avgExposure /= realisticExposures.length;
        for (int i = 0; i < uniformExposures.length; i++)
        {
            uniformExposures[i] = avgExposure;
        }

        System.out.println("Exposure comparison for ages 85-100:");
        System.out.println("Age    Realistic    Uniform");
        System.out.println("--------------------------------");
        for (int age = 85; age <= 100; age += 5)
        {
            int idx = age - 80;
            System.out.printf("%3d    %,10.0f    %,10.0f%n",
                    age, realisticExposures[idx], uniformExposures[idx]);
        }

        // Run with realistic exposures
        ExposuresPCLM pclmRealistic = new ExposuresPCLM(bins, realisticExposures, lambda, ppy);
        double[] resultRealistic = pclmRealistic.pclm();

        // Run with uniform exposures
        ExposuresPCLM pclmUniform = new ExposuresPCLM(bins, uniformExposures, lambda, ppy);
        double[] resultUniform = pclmUniform.pclm();

        System.out.println("\nDisaggregated rates in 85-100 range:");
        System.out.println("Age    Realistic    Uniform      Difference");
        System.out.println("----------------------------------------------");
        for (int age = 85; age <= 100; age += 3)
        {
            int idx = age - 80;
            double diff = resultRealistic[idx] - resultUniform[idx];
            double pctDiff = 100.0 * diff / resultUniform[idx];
            System.out.printf("%3d    %.6f    %.6f    %+.6f (%+.1f%%)%n",
                    age, resultRealistic[idx], resultUniform[idx], diff, pctDiff);
        }

        System.out.println("\nObservation: With realistic (declining) exposures, the mortality curve");
        System.out.println("can rise more steeply because higher rates at older ages contribute");
        System.out.println("less to the grouped average due to lower exposure.\n");

        verifyExposureWeightedConservation(bins, resultRealistic, realisticExposures, ppy, TOLERANCE);
        verifyExposureWeightedConservation(bins, resultUniform, uniformExposures, ppy, TOLERANCE);

        System.out.println();
    }

    /**
     * Generates a realistic population distribution by age.
     * Uses a simplified demographic model with gradual decline and sharper decline
     * in senior ages.
     *
     * @param firstAge Starting age
     * @param lastAge Ending age (inclusive)
     * @param ppy Points per year
     * @return Array of population exposures
     */
    private static double[] generateRealisticPopulation(int firstAge, int lastAge, int ppy)
    {
        int ageRange = lastAge - firstAge + 1;
        int nPoints = ageRange * ppy;
        double[] exposures = new double[nPoints];

        for (int i = 0; i < nPoints; i++)
        {
            // Convert point index to age
            double age = firstAge + (double) i / ppy;

            // Base population (higher for younger ages)
            double population;

            if (age < 20)
            {
                // Young population: relatively stable, slight increase
                population = 40000 + (age * 200);
            }
            else if (age < 65)
            {
                // Working age: stable, gradually increasing
                population = 45000 + ((age - 20) * 100);
            }
            else if (age < 85)
            {
                // Early retirement: gradual decline
                double yearsAfter65 = age - 65;
                population = 49500 - (yearsAfter65 * 500); // Linear decline
            }
            else
            {
                // Senior ages 85+: exponential decline (mortality effect)
                double yearsAfter85 = age - 85;
                population = 39500 * Math.exp(-0.08 * yearsAfter85);
            }

            exposures[i] = population;
        }

        return exposures;
    }

    /**
     * Prints basic statistics about the population distribution.
     */
    private static void printPopulationStats(double[] exposures, int ppy)
    {
        double total = 0.0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        for (double exp : exposures)
        {
            total += exp;
            min = Math.min(min, exp);
            max = Math.max(max, exp);
        }

        System.out.printf("  Total population: %,.0f%n", total);
        System.out.printf("  Age range: 0-%d years (%d points, ppy=%d)%n",
                exposures.length / ppy - 1, exposures.length, ppy);
        System.out.printf("  Population range: %,.0f to %,.0f%n", min, max);
    }

    /**
     * Verifies exposure-weighted conservation property.
     * For each bin, checks that:
     *   sum(rate[j] * exposure[j]) ≈ bin.avg * sum(exposure[j])
     */
    private static void verifyExposureWeightedConservation(
            Bin[] bins, double[] rates, double[] exposures, int ppy, double tolerance)
    {
        System.out.println("Exposure-weighted conservation check:");

        int offset = bins[0].age_x1;
        boolean allPassed = true;

        for (Bin bin : bins)
        {
            int start = (bin.age_x1 - offset) * ppy;
            int end = (bin.age_x2 - offset + 1) * ppy;

            // Sum: rate[j] * exposure[j]
            double weightedSum = 0.0;
            double exposureSum = 0.0;

            for (int j = start; j < end && j < rates.length; j++)
            {
                weightedSum += rates[j] * exposures[j];
                exposureSum += exposures[j];
            }

            // Expected: bin.avg * sum(exposure[j])
            double expected = bin.avg * exposureSum;

            double relativeError = Math.abs(weightedSum - expected) / Math.max(expected, 1e-10);
            boolean passed = relativeError <= tolerance;
            allPassed = allPassed && passed;

            String status = passed ? "✓" : "✗";
            System.out.printf("  Bin %3d-%3d: expected_deaths=%,10.2f, actual_deaths=%,10.2f, error=%.2f%% %s%n",
                    bin.age_x1, bin.age_x2, expected, weightedSum, relativeError * 100, status);
        }

        if (!allPassed)
        {
            throw new AssertionError("Conservation verification failed: some bins exceed tolerance");
        }
        System.out.println("✓ All bins pass conservation check (within " + (tolerance * 100) + "% tolerance)");
    }
}
