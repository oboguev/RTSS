package rtss.data.curves.refine;

import rtss.data.bin.Bin;
import rtss.data.curves.CurveVerifier;
import rtss.data.selectors.Gender;
import rtss.util.Util;

import ch.qos.logback.classic.Level;

public class RefineYearlyPopulation
{
    public static double[] refine(Bin[] bins, String title, double[] p, Integer yearHint, Gender gender) throws Exception
    {
        final double[] p0 = p;

        if (Util.False)
            return p0;

        if (bins.length < 3 ||
            bins[0].widths_in_years != 5 ||
            bins[1].widths_in_years != 5 ||
            bins[3].widths_in_years != 5)
        {
            return p0;
        }

        int nTunablePoints = 10;
        int nFixedPoints = 2;

        if (bins[0].avg > bins[1].avg && bins[1].avg > bins[2].avg)
        {
            /*
             * Case:
             *      _
             *        _
             *          _ 
             */
            nTunablePoints = 10; // ages 0-9
            nFixedPoints = 2; // ages 10-11
        }
        else if (bins[0].avg > bins[1].avg && bins[1].avg < bins[2].avg)
        {
            /*
             * Case:
             *      _   _  
             *        _ 
             */
            int np = locateTurnpoint(bins, p);

            if (np == 5 && p[5] > p[4])
            {
                nTunablePoints = 5; // ages 0-4
                nFixedPoints = 0;
            }
            else
            {
                nTunablePoints = np;
                nFixedPoints = 1;
            }
        }
        else
        {
            return p0;
        }

        /*
         * if points at the right margin keep trending down, extend nFixedPoints
         */
        for (;;)
        {
            int n = nTunablePoints + nFixedPoints;
            if (n >= 20)
                break;
            if (p[n] < p[n - 1])
                nFixedPoints++;
            else
                break;
        }

        double[] attrition = Util.normalize(RefineYearlyPopulationModel.select_attrition09(yearHint, gender));
        double importance_smoothness = 0.94;
        double importance_target_diff_matching = 1.0 - importance_smoothness;

        int plength = Math.max(10, nTunablePoints + nFixedPoints);
        p = Util.splice(p0, 0, plength - 1);

        try
        {
            RefineYearlyPopulationCore rc = new RefineYearlyPopulationCore(Util.dup(p),
                                                                           Util.splice(attrition, 0, nTunablePoints - 1),
                                                                           importance_smoothness,
                                                                           importance_target_diff_matching,
                                                                           nTunablePoints,
                                                                           nFixedPoints,
                                                                           title,
                                                                           null);

            Level outerLogLevel = Level.INFO;
            Level innerLogLevel = Level.INFO;

            double[] px = rc.refineSeriesIterative(outerLogLevel, innerLogLevel);

            Util.assertion(px.length == nTunablePoints);

            p = Util.dup(p0);
            Util.insert(p, px, 0);

            verifyMonotonicity(p, nTunablePoints);
            CurveVerifier.validate_means(p, bins);

            if (Util.True)
            {
                // Util.out("RefineYearlyPopulation processed " + title);
                return p;
            }
            else
            {
                return p0;
            }
        }
        catch (Exception ex)
        {
            Util.err("RefineYearlyPopulation failed for " + title + ", error: " + ex.getLocalizedMessage());
            // ex.printStackTrace();
            // throw ex;
            return p0;
        }
    }

    /*
     * Locate a turning point within bin[1].
     * Turning point is a local minimum, and next point after it goes up.
     * Or it is the last point in the bin. 
     */
    private static int locateTurnpoint(Bin[] bins, double[] p)
    {
        Bin bin = bins[1];
        for (int age = bin.age_x1;; age++)
        {
            if (age == bin.age_x2 || p[age + 1] > p[age])
                return age;
        }
    }

    private static void verifyMonotonicity(double[] p, int nTunablePoints) throws Exception
    {
        for (int k = 0; k < nTunablePoints; k++)
        {
            if (p[k] < p[k + 1])
                throw new Exception("curve child segment is not monotonic");
        }
    }
}
