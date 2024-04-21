package rtss.math.interpolate.mpspline;

import org.apache.commons.math3.analysis.UnivariateFunction;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.curves.CurveVerifier;
import rtss.math.interpolate.ConstrainedCubicSplineInterpolator;
import rtss.math.interpolate.FritschCarlsonMonotonicSpline;
import rtss.math.interpolate.rsplines.HymanSpline;
import rtss.util.Util;

public class MeanPreservingIntegralSpline
{
    public static double[] eval(Bin[] bins, int ppy) throws Exception
    {
        return eval(bins, ppy, null);
    }

    public static double[] eval(Bin[] bins, int ppy, Options options) throws Exception
    {
        return new MeanPreservingIntegralSpline().do_eval(bins, ppy, options);
    }

    private double[] do_eval(Bin[] bins, int ppy, Options options) throws Exception
    {
        if (options == null)
            options = new Options();
        else
            options = new Options(options);
        options.applyDefaults();

        /*
         * Make control points for the curve that represents the running cumulative sum of the bins
         */
        double[] cp_x = new double[bins.length + 1];
        double[] cp_y = new double[bins.length + 1];
        for (int k = 0; k < bins.length; k++)
        {
            Bin bin = bins[k];
            cp_x[k] = bin.age_x1;
            if (bin.next == null)
                cp_x[k + 1] = bin.age_x2 + 1;
            if (k == 0)
                cp_y[k] = 0;
            cp_y[k + 1] = cp_y[k] + bin.avg * bin.widths_in_years;
        }

        /*
         * Create monotonic spline passing through these control points
         */
        UnivariateFunction aspline = null;
        if (Util.False)
            aspline = FritschCarlsonMonotonicSpline.createMonotoneCubicSpline(cp_x, cp_y);
        if (Util.True)
            aspline = new ConstrainedCubicSplineInterpolator().interpolate(cp_x, cp_y);
        if (Util.False)
            aspline = new HymanSpline(cp_x, cp_y);

        double[] scurve = new double[Bins.widths_in_years(bins) * ppy + 1];
        double xstep = 1.0 / ppy;
        int k = 0;
        for (double x = cp_x[0]; x <= cp_x[cp_x.length - 1]; x += xstep)
            scurve[k++] = aspline.value(x);
        /*
         * Calculate its derivative
         */
        double[] curve = new double[scurve.length - 1];
        for (k = 0; k < curve.length; k++)
            curve[k] = (scurve[k + 1] - scurve[k]) / xstep;

        // ### adjust segments (distort)
        
        if (options.checkPositive && !Util.isPositive(curve))
            throw new Exception("Curve has negative values");
        
        if (options.checkNonNegative && !Util.isNonNegative(curve))
            throw new Exception("Curve has negative or zero values");
        
        CurveVerifier.validate_means(curve, bins);

        return curve;
    }

    /*=======================================================================================================*/

    public static class Options
    {
        Boolean checkNonNegative;
        Boolean checkPositive;
        Boolean debug;
        Class<?> basicSplineType;

        public Options()
        {
            checkNonNegative = null;
            checkPositive = null;
            debug = null;
            basicSplineType = null;
        }

        public Options(Options x)
        {
            checkNonNegative = x.checkNonNegative;
            checkPositive = x.checkPositive;
            debug = x.debug;
            basicSplineType = x.basicSplineType;
        }

        public Options checkNonNegative()
        {
            return checkNonNegative(true);
        }

        public Options checkNonNegative(Boolean b)
        {
            checkNonNegative = b;
            return this;
        }

        public Options checkPositive()
        {
            return checkPositive(true);
        }

        public Options checkPositive(Boolean b)
        {
            checkPositive = b;
            return this;
        }

        public Options debug()
        {
            return debug(true);
        }

        public Options debug(boolean b)
        {
            debug = b;
            return this;
        }

        public Options basicSplineType(Class<?> clz)
        {
            basicSplineType = clz;
            return this;
        }

        public void applyDefaults()
        {
            if (checkNonNegative == null)
                checkNonNegative = false;
            
            if (checkPositive == null)
                checkPositive = false;

            // if (basicSplineType == null)
            //     basicSplineType = SteffenSplineInterpolator.class;

            if (debug == null)
                debug = false;
        }
    }
}
