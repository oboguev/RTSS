package rtss.ww2losses.helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
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
import rtss.util.plot.ChartXYSplineAdvanced;
import rtss.ww2losses.struct.HalfYearEntry;
import rtss.ww2losses.struct.HalfYearEntries.HalfYearSelector;

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
        
        this.mt1940.clear_daily_lx();

        for (int year = 1941; year <= 1945; year++)
        {
            CombinedMortalityTable cmt = getTable(year, HalfYearSelector.FirstHalfYear);
            mt2lx(year, HalfYearSelector.FirstHalfYear, cmt, Locality.TOTAL, Gender.MALE);
            mt2lx(year, HalfYearSelector.FirstHalfYear, cmt, Locality.TOTAL, Gender.FEMALE);

            cmt = getTable(year, HalfYearSelector.SecondHalfYear);
            mt2lx(year, HalfYearSelector.SecondHalfYear, cmt, Locality.TOTAL, Gender.MALE);
            mt2lx(year, HalfYearSelector.SecondHalfYear, cmt, Locality.TOTAL, Gender.FEMALE);
        }
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

            xmt = PatchMortalityTable.patch(mt1940, instructions, "поправка антибиотиков для " + year + "." + halfyear.seq(1));
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
        if (mt.has_daily_lx(locality, gender))
            build_daily_lx(year, halfyear, mt, locality, gender);
        return Util.dup(mt.daily_lx(locality, gender));
    }

    private void build_daily_lx(int year, HalfYearSelector halfyear, CombinedMortalityTable mt, Locality locality, Gender gender) throws Exception
    {
        // ###
    }

    /* ======================================================================================= */

    private Map<String, CombinedMortalityTable> cacheTable = new HashMap<>();

    private String key(int year, HalfYearSelector halfyear)
    {
        return HalfYearEntry.id(year, halfyear);
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

    /* ======================================================================================= */

    public static class YearHalfYear
    {
        public int year;
        public HalfYearSelector halfyear;

        public YearHalfYear(int year, HalfYearSelector halfyear)
        {
            this.year = year;
            this.halfyear = halfyear;
        }
    }

    private List<YearHalfYear> allHalfYears() throws Exception
    {
        List<YearHalfYear> list = new ArrayList<>();
        for (int year = 1940; year <= 1946; year++)
        {
            list.add(new YearHalfYear(year, HalfYearSelector.fromString("1")));
            list.add(new YearHalfYear(year, HalfYearSelector.fromString("2")));
        }
        return list;
    }

    /*
     * Диагностическая распечатка
     */
    public void diagPrintFirstEntries(int nyears) throws Exception
    {
        diagPrintFirstEntries(Gender.MALE, nyears);
        diagPrintFirstEntries(Gender.FEMALE, nyears);
    }

    public void diagPrintFirstEntries(Gender gender, int nyears) throws Exception
    {
        Util.out("");
        Util.out("Возрастные коэффиценты смертности для " + gender.name());

        StringBuilder sb = new StringBuilder();
        sb.append("  ");
        for (YearHalfYear yhy : allHalfYears())
            sb.append(String.format(" %d.%d", yhy.year, yhy.halfyear.seq(1)));
        Util.out(sb.toString());

        sb = new StringBuilder();
        sb.append("==");
        for (YearHalfYear yhy : allHalfYears())
        {
            sb.append(" ======");
            Util.unused(yhy);
        }
        Util.out(sb.toString());

        for (int age = 0; age < nyears; age++)
        {
            sb = new StringBuilder();
            sb.append(String.format("%2d", age));

            for (YearHalfYear yhy : allHalfYears())
            {
                CombinedMortalityTable cmt = getTable(yhy.year, yhy.halfyear);
                double[] qx = cmt.getSingleTable(Locality.TOTAL, gender).qx();
                sb.append(String.format("  %.3f", qx[age]));
            }

            Util.out(sb.toString());
        }
    }

    public void diag_display_lx() throws Exception
    {
        for (YearHalfYear yhy : allHalfYears())
        {
            String title = String.format("%d.%d", yhy.year, yhy.halfyear.seq(1));
            CombinedMortalityTable cmt = getTable(yhy.year, yhy.halfyear);
            double[] m_lx = mt2lx(yhy.year, yhy.halfyear, cmt, Locality.TOTAL, Gender.MALE);
            double[] f_lx = mt2lx(yhy.year, yhy.halfyear, cmt, Locality.TOTAL, Gender.FEMALE);
            new ChartXYSplineAdvanced(title, "возраст", "остаток lx").addSeries("male", m_lx).addSeries("femake", f_lx).display();
        }
    }
}
