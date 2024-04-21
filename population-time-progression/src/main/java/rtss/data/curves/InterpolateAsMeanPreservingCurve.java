package rtss.data.curves;

import java.util.HashSet;

import javax.validation.ConstraintViolationException;

import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.mortality.SingleMortalityTable;
import rtss.math.interpolate.ConstrainedCubicSplineInterpolator;
import rtss.math.interpolate.SteffenSplineInterpolator;
import rtss.math.interpolate.TargetPrecision;
import rtss.math.interpolate.mpspline.MeanPreservingIterativeSpline;
import rtss.util.Util;
import rtss.util.plot.ChartXYSplineAdvanced;

/**
 * Interpolate aggregated bins to a smooth yearly curve, in a mean-preserving way.
 * Typically used to interpolate the "qx" curve from an aggregated multi-year data to a yearly resolution.
 * 
 * Does not always work.
 * When mortality at young ages drops too abruptly from very high values to very low values,
 * generated curve can overshoot and go into the negative range, at which point ConstraintViolationException
 * will be thrown. Then use InterpolateUShapeAsMeanPreservingCurve instead.
 */
public class InterpolateAsMeanPreservingCurve
{
    public static final int MAX_AGE = SingleMortalityTable.MAX_AGE;

    public static double[] curve(Bin[] bins) throws Exception, ConstraintViolationException
    {
        return curve(bins, null);
    }

    public static double[] curve(Bin[] bins, Options options) throws Exception, ConstraintViolationException
    {
        if (options == null)
            options = new Options();
        options = options.applyDefaults();

        TargetPrecision precision = new TargetPrecision().eachBinRelativeDifference(0.001);
        MeanPreservingIterativeSpline.Options splineOptions = new MeanPreservingIterativeSpline.Options()
                .checkPositive(false).placeLastBinKnotAtRightmostPoint();

        int ppy = options.ppy;
        double[] yyy1 = null;
        double[] yyy2 = null;
        double[] yyy3 = null;

        if (Util.False)
        {
            splineOptions.basicSplineType(SteffenSplineInterpolator.class);
            yyy1 = MeanPreservingIterativeSpline.eval(bins, ppy, splineOptions, precision);
            if (options.ensurePositive)
                yyy1 = EnsurePositiveCurve.ensurePositive(yyy1, bins);
            
            /*
             * Резкое падение смертности в возрастных группах 1-4 и 5-9 часто вызывает перехлёысты сплайна в этом диапазоне.
             * Изменить ход кривой сделав её монотонно уменьшающейся, но сохраняя средние значения.
             */
            if (options.ensureMonotonicallyDecreasing_1_4_5_9)
                EnsureMonotonic.ensureMonotonicallyDecreasing_1_4_5_9(yyy1, bins, options.debug_title);
        }

        if (Util.False)
        {
            splineOptions.basicSplineType(AkimaSplineInterpolator.class);
            yyy2 = MeanPreservingIterativeSpline.eval(bins, ppy, splineOptions, precision);
            if (options.ensurePositive)
                yyy2 = EnsurePositiveCurve.ensurePositive(yyy2, bins);

            /*
             * Резкое падение смертности в возрастных группах 1-4 и 5-9 часто вызывает перехлёысты сплайна в этом диапазоне.
             * Изменить ход кривой сделав её монотонно уменьшающейся, но сохраняя средние значения.
             */
            if (options.ensureMonotonicallyDecreasing_1_4_5_9)
                EnsureMonotonic.ensureMonotonicallyDecreasing_1_4_5_9(yyy2, bins, options.debug_title);
        }

        if (Util.True)
        {
            splineOptions.basicSplineType(ConstrainedCubicSplineInterpolator.class);
            yyy3 = MeanPreservingIterativeSpline.eval(bins, ppy, splineOptions, precision);
            if (options.ensurePositive)
                yyy3 = EnsurePositiveCurve.ensurePositive(yyy3, bins);

            /*
             * Резкое падение смертности в возрастных группах 1-4 и 5-9 часто вызывает перехлёысты сплайна в этом диапазоне.
             * Изменить ход кривой сделав её монотонно уменьшающейся, но сохраняя средние значения.
             */
            if (options.ensureMonotonicallyDecreasing_1_4_5_9)
                EnsureMonotonic.ensureMonotonicallyDecreasing_1_4_5_9(yyy3, bins, options.debug_title);
        }

        if (Util.False || options.displayCurve)
        {
            double[] xxx = Bins.ppy_x(bins, ppy);
            String title = "Make curve";
            if (options.debug_title != null)
                title += " " + options.debug_title;
            ChartXYSplineAdvanced chart = new ChartXYSplineAdvanced(title, "x", "y").showSplinePane(false);
            if (yyy1 != null)
                chart.addSeries("1", xxx, yyy1);
            if (yyy2 != null)
                chart.addSeries("2", xxx, yyy2);
            if (yyy3 != null)
                chart.addSeries("3", xxx, yyy3);
            chart.addSeries("bins", xxx, Bins.ppy_y(bins, ppy));
            chart.display();
        }

        double[] yyy = yyy1;
        if (yyy == null)
            yyy = yyy2;
        if (yyy == null)
            yyy = yyy3;

        if (!Util.isPositive(yyy))
            throw new ConstraintViolationException("Error calculating curve (negative or zero value)", new HashSet<>());

        double[] yy = Bins.ppy2yearly(yyy, ppy);

        CurveVerifier.validate_means(yy, bins);

        return yy;
    }
    
    /* =============================================================================================== */

    public static class Options
    {
        public String debug_title;
        public Integer ppy;
        public Boolean ensurePositive;
        public Boolean ensureMonotonicallyDecreasing_1_4_5_9;
        public Boolean displayCurve;

        public Options()
        {
        }

        public Options(Options x)
        {
            this.debug_title = x.debug_title;
            this.ppy = x.ppy;
            this.ensurePositive = x.ensurePositive;
            this.ensureMonotonicallyDecreasing_1_4_5_9 = x.ensureMonotonicallyDecreasing_1_4_5_9;
            this.displayCurve = x.displayCurve;
        }

        public Options applyDefaults()
        {
            Options x = new Options(this);

            if (x.debug_title == null)
                x.debug_title = "";

            if (x.ppy == null)
                x.ppy = 1000;

            if (x.ensurePositive == null)
                x.ensurePositive = false;

            if (x.ensureMonotonicallyDecreasing_1_4_5_9 == null)
                x.ensureMonotonicallyDecreasing_1_4_5_9 = false;

            if (x.displayCurve == null)
                x.displayCurve = false;

            return x;
        }

        public Options debug_title(String v)
        {
            debug_title = v;
            return this;
        }

        public Options ppy(Integer v)
        {
            ppy = v;
            return this;
        }

        public Options ensurePositive()
        {
            return ensurePositive(true);
        }
        
        public Options ensurePositive(Boolean v)
        {
            ensurePositive = v;
            return this;
        }

        public Options ensureMonotonicallyDecreasing_1_4_5_9()
        {
            return ensureMonotonicallyDecreasing_1_4_5_9(true);
        }
        
        public Options ensureMonotonicallyDecreasing_1_4_5_9(Boolean v)
        {
            ensureMonotonicallyDecreasing_1_4_5_9 = v;
            return this;
        }

        public Options displayCurve()
        {
            return displayCurve(true);
        }
        
        public Options displayCurve(Boolean v)
        {
            displayCurve = v;
            return this;
        }
    }
}
