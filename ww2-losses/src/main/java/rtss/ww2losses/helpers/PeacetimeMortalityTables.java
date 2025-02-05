package rtss.ww2losses.helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.curves.InterpolateYearlyToDailyAsValuePreservingMonotoneCurve;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.synthetic.PatchMortalityTable;
import rtss.data.mortality.synthetic.PatchMortalityTable.PatchInstruction;
import rtss.data.mortality.synthetic.PatchMortalityTable.PatchOpcode;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.math.interpolate.ConstrainedCubicSplineInterpolator;
import rtss.math.interpolate.TargetPrecision;
import rtss.math.interpolate.mpspline.MeanPreservingIterativeSpline;
import rtss.util.Util;
import rtss.ww2losses.HalfYearEntries.HalfYearSelector;
import rtss.ww2losses.HalfYearEntry;

/*
 * Вычислить таблицу смертности для условий мирного времени с учётом антибиотиков
 */
public class PeacetimeMortalityTables
{
    /*
     * таблица смертности 1940 года
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

        /* интерполяция на полугодия, но для 1941 и 1941 оставить 1.0 */
        halfyearly_imr_multiplier = yearly2timepoints(yearly_imr_multiplier, 2, 100);
        halfyearly_imr_multiplier[0] = halfyearly_imr_multiplier[1] = halfyearly_imr_multiplier[2] = halfyearly_imr_multiplier[3] = 1.0;
    }

    public CombinedMortalityTable getTable(int year, HalfYearSelector halfyear) throws Exception
    {
        if (!applyAntibiotics)
            return mt1940;
        
        CombinedMortalityTable xmt = cacheTable.get(key(year, halfyear));

        if (xmt == null)
        {
            int ix = (year - 1940) * 2 + halfyear.seq(0);
            double scale0 = halfyearly_imr_multiplier[ix];

            if (!Util.differ(scale0, 1.0))
                return mt1940;

            PatchInstruction instruction = new PatchInstruction(PatchOpcode.MultiplyWithDecay, 0, 5, scale0, 1.0);
            List<PatchInstruction> instructions = new ArrayList<>();
            instructions.add(instruction);

            xmt = PatchMortalityTable.patch(mt1940, instructions, "поправка антибиотиков для " + year);
            xmt.seal();
            cacheTable.put(key(year, halfyear), xmt);
        }

        return xmt;
    }
    
    /* ======================================================================================================= */

    public double[] mt2lx(int year, HalfYearSelector halfyear, CombinedMortalityTable mt, Locality locality, Gender gender) throws Exception
    {
        CombinedMortalityTable xmt = getTable(year, halfyear);
        Util.assertion(mt == xmt);

        double[] lx = cacheLX.get(key(year, halfyear, locality, gender));
        
        if (lx == null)
        {
            lx = mt2lx(mt, locality, gender);
            cacheLX.put(key(year, halfyear), lx);
        }

        return Util.dup(lx);
    }

    /*
     * Построить кривую l(x) для таблицы смертности mt, указаного типа местности и пола
     */
    private double[] mt2lx(final CombinedMortalityTable mt, final Locality locality, final Gender gender) throws Exception
    {
        double[] yearly_lx = mt.getSingleTable(locality, gender).lx();

        /*
         * Провести дневную кривую так что
         *       daily_lx[0]         = yearly_lx[0]
         *       daily_lx[365]       = yearly_lx[1]
         *       daily_lx[365 * 2]   = yearly_lx[2]
         *       etc.
         */
        double[] daily_lx = InterpolateYearlyToDailyAsValuePreservingMonotoneCurve.yearly2daily(yearly_lx);

        /*
         * Базовая проверка правильности
         */
        if (Util.differ(daily_lx[0], yearly_lx[0]) ||
            Util.differ(daily_lx[365 * 1], yearly_lx[1]) ||
            Util.differ(daily_lx[365 * 2], yearly_lx[2]) ||
            Util.differ(daily_lx[365 * 3], yearly_lx[3]))
        {
            throw new Exception("Ошибка в построении daily_lx");
        }

        Util.assertion(Util.isMonotonicallyDecreasing(daily_lx, true));

        return daily_lx;
    }

    /* ======================================================================================= */
    
    private Map<String, CombinedMortalityTable> cacheTable = new HashMap<>();
    private Map<String, double[]> cacheLX = new HashMap<>();

    private String key(int year, HalfYearSelector halfyear)
    {
        return HalfYearEntry.id(year,  halfyear);
    }

    private String key(int year, HalfYearSelector halfyear, Locality locality, Gender gender)
    {
        return key(year, halfyear) + "." + locality.name() + "." + gender.name();
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
