package rtss.ww2losses.util.despike;

import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

public class DespikeComb
{
    public static PopulationContext despike(PopulationContext p, int nds) throws Exception
    {
        PopulationContext p2 = p.clone();

        for (Gender gender : Gender.TwoGenders)
        {
            double[] v = p.asArray(Locality.TOTAL, gender);
            double[] v2 = removeSpikes(v, nds);
            Util.checkSame(Util.sum(v), Util.sum(v2));
            p2.fromArray(Locality.TOTAL, gender, v2);
        }

        return p2;
    }

    public static double[] removeSpikes(double[] f, int xmax)
    {
        // Clone the original array to create f2
        double[] f2 = f.clone();

        // Iterate through the array backward from xmax to 1
        for (int i = xmax - 1; i >= 1; i--)
        {
            double prev = f2[i - 1];
            double curr = f2[i];
            double next = f2[i + 1];

            // Check if the current element is a spike
            if (isSpike(prev, curr, next))
            {
                // Calculate the average of the previous and next elements
                double avg = (prev + next) / 2.0;
                double excess = curr - avg;

                // Set the spike value to the average
                f2[i] = avg;

                // Distribute the excess value
                distributeExcess(f2, i, excess, xmax);
            }
        }

        return f2;
    }

    private static boolean isSpike(double prev, double curr, double next)
    {
        // A spike is at least twice as large as the average of its neighbors
        double neighborAvg = (prev + next) / 2.0;
        return curr >= 2 * neighborAvg;
    }

    private static void distributeExcess(double[] f2, int spikeIndex, double excess, int xmax)
    {
        // Determine the range to distribute the excess
        int start = 0; // Default start for the first spike
        int end = spikeIndex - 1;

        // Find the previous spike (if any) to adjust the start of the range
        for (int i = spikeIndex - 1; i >= 0; i--)
        {
            if (i == 0 || i >= xmax)
                continue; // Skip boundaries and beyond xmax
            double prev = f2[i - 1];
            double curr = f2[i];
            double next = f2[i + 1];
            if (isSpike(prev, curr, next))
            {
                start = i + 1; // Start after the previous spike
                break;
            }
        }

        // Calculate the total weight of the range
        double totalWeight = 0;
        for (int i = start; i <= end; i++)
        {
            totalWeight += f2[i];
        }

        // Distribute the excess proportionally
        if (totalWeight > 0)
        {
            for (int i = start; i <= end; i++)
            {
                f2[i] += (f2[i] / totalWeight) * excess;
            }
        }
        else
        {
            // If all elements are zero, distribute evenly
            double evenDistribution = excess / (end - start + 1);
            for (int i = start; i <= end; i++)
            {
                f2[i] += evenDistribution;
            }
        }
    }

    public static void main(String[] args)
    {
        // Example usage
        double[] f = { 1.0, 2.0, 10.0, 3.0, 4.0, 20.0, 5.0, 6.0, 7.0, 8.0 };
        int xmax = 7; // Only process up to index 7
        double[] result = removeSpikes(f, xmax);

        // Print the result
        for (double value : result)
        {
            System.out.print(value + " ");
        }
    }
}