package rtss.math.interpolate.mpspline;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.curves.CurveVerifier;
import rtss.math.interpolate.ConstrainedCubicSplineInterpolator;
import rtss.math.interpolate.FritschCarlsonMonotonicSpline;
import rtss.math.interpolate.SteffenSplineInterpolator;
import rtss.math.interpolate.rsplines.FMMSpline;
import rtss.math.interpolate.rsplines.HymanSpline;
import rtss.util.Clipboard;
import rtss.util.Util;

public class MeanPreservingIntegralSpline
{
    public static double[] eval(Bin[] bins, Options options) throws Exception
    {
        return new MeanPreservingIntegralSpline().do_eval(bins, options);
    }

    private double[] do_eval(Bin[] bins, Options options) throws Exception
    {
        if (options == null)
            options = new Options();
        options = options.applyDefaults();

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
        if (options.basicSplineType == FritschCarlsonMonotonicSpline.class)
            aspline = FritschCarlsonMonotonicSpline.createMonotoneCubicSpline(cp_x, cp_y);
        else if (options.basicSplineType == ConstrainedCubicSplineInterpolator.class)
            aspline = new ConstrainedCubicSplineInterpolator().interpolate(cp_x, cp_y, options.splineParams);
        else if (options.basicSplineType == HymanSpline.class)
            aspline = new HymanSpline(cp_x, cp_y);
        else if (options.basicSplineType == FMMSpline.class)
            aspline = new FMMSpline(cp_x, cp_y);
        else if (options.basicSplineType == SteffenSplineInterpolator.class)
            aspline = new SteffenSplineInterpolator().interpolate(cp_x, cp_y);
        else if (options.basicSplineType == AkimaSplineInterpolator.class)
            aspline = new AkimaSplineInterpolator().interpolate(cp_x, cp_y);
        else
            throw new IllegalArgumentException("Incorrect spline type");
        
        double[] curve = new double[Bins.widths_in_years(bins) * options.ppy];
        double xstep = 1.0 / options.ppy;

        if (Util.False && options.basicSplineType == ConstrainedCubicSplineInterpolator.class)
        {
            /*
             * This does not work at low @ppy, as derivative is evaluated at discrete points,
             * rather than covering the range of actual "dx" increments, so the sum over a bin range
             * somewhat differs from bin.avg. 
             */
            aspline = ConstrainedCubicSplineInterpolator.derivative(aspline);

            for (int k = 0; k < curve.length; k++)
            {
                double x = cp_x[0] + k * (cp_x[cp_x.length - 1] - cp_x[0] - xstep) / (curve.length - 1);
                x = Math.max(x, cp_x[0]);
                x = Math.min(x, cp_x[cp_x.length - 1]);
                curve[k] = aspline.value(x);
            }
        }
        else if (Util.False && aspline instanceof PolynomialSplineFunction)
        {
            /*
             * This does not work at low @ppy, as derivative is evaluated at discrete points,
             * rather than covering the range of actual "dx" increments, so the sum over a bin range
             * somewhat differs from bin.avg. 
             */
            aspline = ((PolynomialSplineFunction) aspline).derivative();
            
            for (int k = 0; k < curve.length; k++)
            {
                double x = cp_x[0] + k * (cp_x[cp_x.length - 1] - cp_x[0] - xstep) / (curve.length - 1);
                x = Math.max(x, cp_x[0]);
                x = Math.min(x, cp_x[cp_x.length - 1]);
                curve[k] = aspline.value(x);
            }
        }
        else
        {
            /*
             * Calculate spline
             */
            double[] scurve = new double[Bins.widths_in_years(bins) * options.ppy + 1];
            for (int k = 0; k < scurve.length; k++)
            {
                double x = cp_x[0] + k * (cp_x[cp_x.length - 1] - cp_x[0]) / (scurve.length - 1);
                x = Math.max(x, cp_x[0]);
                x = Math.min(x, cp_x[cp_x.length - 1]);
                scurve[k] = aspline.value(x);
            }

            /*
             * Calculate its derivative
             */
            for (int k = 0; k < curve.length; k++)
                curve[k] = (scurve[k + 1] - scurve[k]) / xstep;
        }
        
        if (Util.True)
        {
            double [] xxx = Bins.ppy_x(bins, options.ppy);
            Clipboard.put(" ", xxx, curve);
        }

        if (options.checkPositive && !Util.isPositive(curve))
            throw new Exception("Curve has negative values");
        
        if (options.checkNonNegative && !Util.isNonNegative(curve))
            throw new Exception("Curve has negative or zero values");
        
        // if using discrete derivatives directly, then for low values of ppy may want to adjust (distort) segments 
        // so they match the mean value  
        CurveVerifier.validate_means(curve, bins);

        return curve;
    }

    /*=======================================================================================================*/

    public static class Options
    {
        String debug_title;
        Integer ppy;
        Boolean checkNonNegative;
        Boolean checkPositive;
        Boolean debug;
        Class<?> basicSplineType;
        Map<String, Object> splineParams = new HashMap<>();

        public Options()
        {
            debug_title = null;
            ppy = null;
            checkNonNegative = null;
            checkPositive = null;
            debug = null;
            basicSplineType = null;
        }

        public Options(Options x)
        {
            this.debug_title = x.debug_title;
            this.ppy = x.ppy;
            this.checkNonNegative = x.checkNonNegative;
            this.checkPositive = x.checkPositive;
            this.debug = x.debug;
            this.basicSplineType = x.basicSplineType;
            this.splineParams = new HashMap<>(x.splineParams);
        }
        
        public Options clone()
        {
            return new Options(this);
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
        
        public Options splineParams(String key, Object value)
        {
            splineParams.put(key, value);
            return this;
        }

        public Options applyDefaults()
        {
            Options x = new Options(this);

            if (x.debug_title == null)
                x.debug_title = "";

            if (x.ppy == null)
                x.ppy = 1000;

            if (x.checkNonNegative == null)
                x.checkNonNegative = false;
            
            if (x.checkPositive == null)
                x.checkPositive = false;

            if (x.basicSplineType == null)
                x.basicSplineType = ConstrainedCubicSplineInterpolator.class;

            if (x.debug == null)
                x.debug = false;
            
            return x;
        }
    }
}
