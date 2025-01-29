package rtss.ww2losses.helpers;

import java.util.ArrayList;
import java.util.List;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.synthetic.PatchMortalityTable;
import rtss.data.mortality.synthetic.PatchMortalityTable.PatchInstruction;
import rtss.data.mortality.synthetic.PatchMortalityTable.PatchOpcode;
import rtss.math.interpolate.ConstrainedCubicSplineInterpolator;
import rtss.math.interpolate.TargetPrecision;
import rtss.math.interpolate.mpspline.MeanPreservingIterativeSpline;
import rtss.util.Util;
import rtss.ww2losses.HalfYearEntries.HalfYearSelector;

/*
 * Вычислить таблицу мирного времени с учётом антибиотиков
 */
public class PeacetimeMortalityTables
{
    /*
     * таблица смертностти 1940 года
     */
    private final CombinedMortalityTable mt1940;
    private final boolean applyAntibiotics;

    /*
     * коэффициент младенческой смертности в 1940-1946 гг. относительно 1940 года
     */
    private final double[] yearly_imr_multiplier = { 1.0, 1.0, 0.90, 0.70, 0.45, 0.40, 0.38 };
    private double[] halfyearly_imr_multiplier;

    public PeacetimeMortalityTables(CombinedMortalityTable mt1940, boolean applyAntibiotics) throws Exception
    {
        this.mt1940 = mt1940;
        this.applyAntibiotics = applyAntibiotics;
        
        /* интерполяция на полугодия */
        halfyearly_imr_multiplier = yearly2timepoints(yearly_imr_multiplier, 2, 100);
        halfyearly_imr_multiplier[0] = halfyearly_imr_multiplier[1] = halfyearly_imr_multiplier[2] = halfyearly_imr_multiplier[3] = 1.0;
    }
    
    public CombinedMortalityTable get(int year, HalfYearSelector halfyear) throws Exception
    {
        if (!applyAntibiotics)
            return mt1940;

        int ix = (year - 1940) * 2 + halfyear.seq(0);
        double scale0 = halfyearly_imr_multiplier[ix];

        if (!Util.differ(scale0, 1.0))
            return mt1940;
        
        PatchInstruction instruction = new PatchInstruction(PatchOpcode.MultiplyWithDecay, 0, 5, scale0, 1.0);
        List<PatchInstruction> instructions = new ArrayList<>();
        instructions.add(instruction);

        CombinedMortalityTable xmt = PatchMortalityTable.patch(mt1940, instructions, "поправка антибиотиков для " + year);

        return xmt;
    }

    /* ======================================================================================= */

    private static double[] yearly2timepoints(double[] yearly, int ppy, int ppinterval) throws Exception
    {
        double[] points = yearly2points(yearly, ppy * ppinterval);
        double[] timepoints = new double[ppy * yearly.length];

        for (int k = 0; k < timepoints.length; k++)
        {
            double[] x = Util.splice(points, k * ppinterval, k * ppinterval + ppinterval - 1);
            timepoints[k] = Util.average(x);
        }

        if (!Util.isNonNegative(timepoints))
            throw new Exception("Error calculating curve (negative value)");

        return timepoints;
    }

    private static double[] yearly2points(double[] yearly, int ppy) throws Exception
    {
        Bin[] bins = Bins.fromValues(yearly);

        TargetPrecision precision = new TargetPrecision().eachBinRelativeDifference(0.001);
        MeanPreservingIterativeSpline.Options options = new MeanPreservingIterativeSpline.Options()
                .checkPositive(false);

        if (Util.False)
        {
            /*
             * Helps to avoid the last segment of the curve dive down too much
             */
            options = options.placeLastBinKnotAtRightmostPoint();
        }

        // double[] xxx = Bins.ppy_x(bins, ppy);
        options.basicSplineType(ConstrainedCubicSplineInterpolator.class);
        double[] yyy = MeanPreservingIterativeSpline.eval(bins, ppy, options, precision);

        if (!Util.isNonNegative(yyy))
            throw new Exception("Error calculating curve (negative value)");

        double[] yy = Bins.ppy2yearly(yyy, ppy);

        if (!Util.isNonNegative(yy))
            throw new Exception("Error calculating curve (negative value)");

        validate_means(yy, bins);

        return yyy;
    }

    /*
     * Verify that the curve preserves mean values as indicated by the bins
     */
    static void validate_means(double[] yy, Bin... bins) throws Exception
    {
        for (Bin bin : bins)
        {
            double[] y = Util.splice(yy, bin.age_x1, bin.age_x2);
            if (Util.differ(Util.average(y), bin.avg, 0.001))
                throw new Exception("Curve does not preserve mean values of the bins");
        }
    }
}
