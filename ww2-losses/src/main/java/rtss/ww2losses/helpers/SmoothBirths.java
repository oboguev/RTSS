package rtss.ww2losses.helpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.curves.InterpolatePopulationAsMeanPreservingCurve;
import rtss.data.curves.TargetResolution;
import rtss.data.curves.InterpolatePopulationAsMeanPreservingCurve.InterpolationOptions;
import rtss.data.selectors.Gender;
import rtss.util.Util;
import rtss.util.plot.ChartXY;
import rtss.ww2losses.params.AreaParameters;
import rtss.ww2losses.struct.HalfYearEntries;
import rtss.ww2losses.struct.HalfYearEntry;
import rtss.ww2losses.struct.HalfYearEntries.HalfYearSelector;

import static rtss.data.population.projection.ForwardPopulation.years2days;

public class SmoothBirths
{
    private final int ndays = years2days(0.5);
    private final double PROMILLE = 1000;

    private AreaParameters ap;
    private List<Bin> bins;
    private double[] births_1941_1st_halfyear;

    public SmoothBirths init_nonwar(AreaParameters ap, HalfYearEntries<HalfYearEntry> halves) throws Exception
    {
        this.ap = ap;
        this.bins = new ArrayList<>();
        int nd = 0;

        for (HalfYearEntry he : halves)
        {
            if (he.next == null)
                break;

            double p1 = he.p_nonwar_with_births.sum();
            double p2 = he.next.p_nonwar_with_births.sum();
            double pavg = (p1 + p2) / 2;

            double births = ap.CBR_1940_MIDYEAR * pavg * 0.5 / PROMILLE;

            bins.add(new Bin(nd, nd + ndays - 1, births / ndays));
            nd += ndays;
        }

        births_1941_1st_halfyear = halves.get(1941, HalfYearSelector.FirstHalfYear).expected_nonwar_births_byday;

        return this;
    }

    public SmoothBirths init_actual(AreaParameters ap, HalfYearEntries<HalfYearEntry> halves) throws Exception
    {
        this.ap = ap;
        this.bins = new ArrayList<>();
        int nd = 0;

        for (HalfYearEntry he : halves)
        {
            if (he.next == null)
                break;

            bins.add(new Bin(nd, nd + ndays - 1, he.actual_births / ndays));
            nd += ndays;
        }

        return this;
    }

    public void calc() throws Exception
    {
        InterpolationOptions options = new InterpolationOptions().usePrimaryCSASRA(true).usePrimarySPLINE(false).useSecondaryRefineYearlyAges(false);
        double[] births = InterpolatePopulationAsMeanPreservingCurve.curve(Bins.bins(bins), "wartime births", TargetResolution.DAILY, 1942,
                                                                           Gender.MALE, options);
        Util.insert(births, births_1941_1st_halfyear, 0);

        int nd1 = births_1941_1st_halfyear.length;
        int nd2 = nd1 + (ndays * 3) / 4;

        for (int k = 0; k < 365; k++)
        {
            Util.out(String.format("f[%d] = %.3f", k, births[k]));
        }

        double[] births2 = smoothData(births, nd1, nd2, 50.0, 300);

        // ChartXY.display("Рождения " + ap.area, births);
        // ###

        new ChartXY("Рождения " + ap.area, "x", "y")
                .addSeries("b1", births)
                .addSeries("b2", births2)
                .display();
    }

    /* ================================================================================================= */

    public static double[] smoothData(double[] f, int x1, int x2, double sigma, int iterations)
    {
        // Step 1: Create a copy of the original array
        double[] f2 = f.clone();

        // Step 2: Calculate the original sum in the range [x1 ... x2]
        double originalSum = 0.0;
        for (int i = x1; i <= x2; i++)
        {
            originalSum += f[i];
        }

        // Step 3: Create the Gaussian kernel
        double[] kernel = createGaussianKernel(sigma);

        // Step 4: Apply Gaussian smoothing multiple times
        double[] smoothedValues = new double[f.length]; // Same length as f
        System.arraycopy(f, 0, smoothedValues, 0, f.length); // Copy original values

        double[] tempArray = new double[f.length]; // Temporary array for intermediate results

        for (int iter = 0; iter < iterations; iter++)
        {
            // Use tempArray to store the smoothed values for this iteration
            System.arraycopy(smoothedValues, 0, tempArray, 0, f.length);
            applyGaussianSmoothing(tempArray, smoothedValues, x1, x2, kernel);
        }

        // Step 5: Calculate the smoothed sum in the range [x1 ... x2]
        double smoothedSum = 0.0;
        for (int i = x1; i <= x2; i++)
        {
            smoothedSum += smoothedValues[i];
        }

        // Step 6: Calculate the difference between the original sum and the smoothed sum
        double difference = originalSum - smoothedSum;

        // Step 7: Distribute the difference smoothly across the range [x1 ... x2]
        distributeDifference(smoothedValues, x1, x2, difference, kernel);

        // Step 8: Copy the smoothed values back to f2
        System.arraycopy(smoothedValues, x1, f2, x1, x2 - x1 + 1);

        return f2;
    }

    private static double[] createGaussianKernel(double sigma)
    {
        int kernelSize = (int) (12 * sigma); // Larger kernel size (e.g., 12 * sigma)
        if (kernelSize % 2 == 0)
        {
            kernelSize++; // Ensure kernel size is odd
        }

        double[] kernel = new double[kernelSize];
        double sum = 0;
        int radius = kernelSize / 2;

        // Create the Gaussian kernel
        for (int i = -radius; i <= radius; i++)
        {
            double value = Math.exp(-(i * i) / (2 * sigma * sigma));
            kernel[i + radius] = value;
            sum += value;
        }

        // Normalize the kernel so that the sum of weights equals 1
        for (int i = 0; i < kernel.length; i++)
        {
            kernel[i] /= sum;
        }

        return kernel;
    }

    private static void applyGaussianSmoothing(double[] source, double[] destination, int x1, int x2, double[] kernel)
    {
        int radius = kernel.length / 2;

        for (int i = x1; i <= x2; i++)
        {
            double sum = 0.0;

            for (int j = -radius; j <= radius; j++)
            {
                int index = i + j;
                if (index >= 0 && index < source.length)
                { // Ensure index is within bounds
                    sum += source[index] * kernel[j + radius];
                }
            }

            destination[i] = sum;
        }
    }

    private static void distributeDifference(double[] smoothedValues, int x1, int x2, double difference, double[] kernel)
    {
        int radius = kernel.length / 2;
        double totalWeight = 0.0;

        // Calculate the total weight for normalization
        for (int i = x1; i <= x2; i++)
        {
            int kernelIndex = (i - x1) - (x2 - x1) / 2 + radius;
            if (kernelIndex >= 0 && kernelIndex < kernel.length)
            {
                totalWeight += kernel[kernelIndex];
            }
        }

        // Distribute the difference proportionally to the Gaussian weights
        for (int i = x1; i <= x2; i++)
        {
            int kernelIndex = (i - x1) - (x2 - x1) / 2 + radius;
            if (kernelIndex >= 0 && kernelIndex < kernel.length)
            {
                double weight = kernel[kernelIndex];
                double correction = difference * (weight / totalWeight);
                smoothedValues[i] += correction;
            }
        }
    }
}
