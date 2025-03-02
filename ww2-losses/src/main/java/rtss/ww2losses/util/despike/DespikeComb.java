package rtss.ww2losses.util.despike;

import java.util.ArrayList;
import java.util.List;

import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

import static rtss.data.population.projection.ForwardPopulation.years2days;

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

    private static double[] removeSpikes(double[] f, int xmax) throws Exception
    {
        List<Integer> spikes = locateSpikes(f, xmax);
        for (int k = 0; k < spikes.size(); k++)
        {
            int currSpike = spikes.get(k);
            int end = (k == spikes.size() - 1) ? xmax : spikes.get(k + 1);
            fixSpike(f, currSpike, end - 1);
        }
        return f;
    }

    private static List<Integer> locateSpikes(double[] f, int xmax) throws Exception
    {
        int ndays = years2days(0.5);

        f = Util.abs(f);

        List<Integer> list = new ArrayList<>();
        for (int k = 1; k < xmax; k++)
        {
            double curr = f[k];
            double prev = f[k - 1];
            double next = f[k + 1];
            double avg = (prev + next) / 2;
            if (avg != 0 && curr / avg > 3)
            {
                int kk = (int) Math.round(k / (double) ndays);
                if (Math.abs(kk * ndays - k) > 4)
                    throw new Exception("Misplaced spike");
                list.add(k);
            }
        }

        if (list.size() != 9)
            throw new Exception("Misplaced spikes");

        return list;
    }

    private static void fixSpike(double[] f, int spike, int end)
    {
        double excess = f[spike] - f[spike + 1];
        f[spike] = f[spike + 1];
        int count = end - spike + 1;
        for (int k = spike; k <= end; k++)
            f[k] += excess / count;
    }
}