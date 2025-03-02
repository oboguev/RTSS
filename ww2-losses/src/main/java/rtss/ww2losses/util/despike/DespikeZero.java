package rtss.ww2losses.util.despike;

import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

public class DespikeZero
{
    public static PopulationContext despike(PopulationContext p, int nds) throws Exception
    {
        PopulationContext p2 = p.clone();
        
        for (Gender gender : Gender.TwoGenders)
        {
            double[] v = p.asArray(Locality.TOTAL, gender);
            double[] v2 = redistributeSpike(v, nds);
            Util.checkSame(Util.sum(v), Util.sum(v2));
            p2.fromArray(Locality.TOTAL, gender, v2);
        }
        
        return p2;
    }
    
    private static double[] redistributeSpike(double[] f, int xmax)
    {
        if (f == null || xmax <= 0 || xmax >= f.length)
        {
            throw new IllegalArgumentException("Invalid input parameters.");
        }

        // Calculate the excess value at f[0]
        double excess = f[0] - f[1];

        // Reduce f[0] to f[1]
        f[0] = f[1];

        // Calculate the total weight for redistribution
        double totalWeight = 0.0;
        double[] weights = new double[xmax + 1];

        // Define the weight function
        int midPoint = xmax / 2;
        for (int i = 0; i <= xmax; i++)
        {
            if (i <= midPoint)
            {
                weights[i] = 1.0; // Constant weight for the first half
            }
            else
            {
                // Linearly taper the weight from midPoint to xmax
                weights[i] = 1.0 - (double) (i - midPoint) / (xmax - midPoint);
            }
            totalWeight += weights[i] * f[i];
        }

        // Redistribute the excess proportionally
        if (totalWeight > 0)
        {
            for (int i = 0; i <= xmax; i++)
            {
                f[i] += excess * (weights[i] * f[i]) / totalWeight;
            }
        }

        return f;
    }
}