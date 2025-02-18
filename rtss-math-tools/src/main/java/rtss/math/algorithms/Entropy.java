package rtss.math.algorithms;

import rtss.util.Util;

public class Entropy
{
    /*
     * Мера концентрации значений 
     */
    public static double concentration(double[] values)
    {
        if (values.length == 0)
            return 0;
        
        Util.checkValidNonNegative(values);

        // If all values are zero, return 0 (no concentration)
        if (Util.sum(values) == 0)
            return 0;

        // Normalize the values to get probabilities
        double[] probabilities = Util.normalize(values);

        // Calculate entropy
        double entropy = 0;
        for (double p : probabilities)
        {
            if (p > 0)
            {
                // Avoid log(0)
                entropy -= p * Math.log(p);
            }
        }

        // Normalize entropy by the maximum possible entropy (log(n))
        double maxEntropy = Math.log(probabilities.length);
        double normalizedEntropy = entropy / maxEntropy;

        // Convert entropy to a concentration measure (1 - normalized entropy)
        double concentration = 1.0 - normalizedEntropy;

        return concentration;
    }
}
