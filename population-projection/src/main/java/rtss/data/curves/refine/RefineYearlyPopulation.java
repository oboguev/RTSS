package rtss.data.curves.refine;

import rtss.data.bin.Bin;
import rtss.data.curves.CurveVerifier;
import rtss.data.curves.refine.RefineYearlyPopulationModel.AttritionModel;
import rtss.data.selectors.Gender;
import rtss.util.Util;

import ch.qos.logback.classic.Level;

/*
 * Tune initial age points of the disaggregated populaton to conform young-age typical mortality pattern.
 * See more detatiled explanation in module RefineYearlyPopulationCore.
 */
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

        /*
         * Attrition array describes
         */
        AttritionModel model = RefineYearlyPopulationModel.select_model(yearHint, gender);
        double[] attrition = model.attrition09();

        /*
         * Number of age points to tune.
         * And number of subsequent age points to use for curve smoothness evaluation; actually this is (nFixedPoints - 1) points.  
         */
        int nTunablePoints = 10;
        int nFixedPoints = 2;

        if (bins[0].avg > bins[1].avg &&
            bins[1].avg > bins[2].avg)
        {
            /*
             * Case:
             *      _
             *        _
             *          _
             *          
             * Gradually decreasing population, normal case. 
             * Assume approximately steady births through recent 10 years           
             */
            nTunablePoints = 10; // ages 0-9
            nFixedPoints = 2; // ages 10-11
        }
        else if (bins[0].avg > bins[1].avg &&
                 bins[1].avg < bins[2].avg &&
                 bins[0].avg > bins[2].avg)
        {
            /*
             * Case:
             *      _   _  
             *        _ 
             *        
             * U-shaped population. 
             * There was a drop in births during second bin birth years.
             * Should adjust attrition to account for it.           
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

            adjustedAttrition(bins, yearHint, gender);
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

        double importance_smoothness = 0.94;
        double importance_target_diff_matching = 1.0 - importance_smoothness;

        int plength = Math.max(10, nTunablePoints + nFixedPoints);
        p = Util.splice(p0, 0, plength - 1);

        try
        {
            RefineYearlyPopulationCore rc = new RefineYearlyPopulationCore(Util.dup(p),
                                                                           Util.normalize(Util.splice(attrition, 0, nTunablePoints - 1)),
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

    /* ====================================================================================== */

    private static void adjustedAttrition(Bin[] bins, Integer yearHint, Gender gender) throws Exception
    {
        /*
         * build the curve of expected model population progress for ages 0...14 under given mortality pattern 
         */
        AttritionModel model = RefineYearlyPopulationModel.select_model(yearHint, gender);

        double[] p = new double[15];
        p[0] = model.L0;
        for (int age = 1; age <= 14; age++)
            p[age] = p[age - 1] - model.attrition[age - 1];

        double b0 = Util.sum_range(p, 0, 4) / 5;
        double b1 = Util.sum_range(p, 5, 9) / 5;
        double b2 = Util.sum_range(p, 10, 14) / 5;
        double a = (b1 - b2) / (b0 - b2);
        Util.assertion(a >= 0);

        b0 = bins[0].avg;
        b1 = bins[1].avg;
        b2 = bins[2].avg;
        double v1 = a * b0 + (1 - a) * b2;
        
        /*
         * @v1 is expected population in bins[s] if there were no drop in birth rates 
         */
        Util.assertion(v1 >= b1);

        Util.noop();
    }
}
