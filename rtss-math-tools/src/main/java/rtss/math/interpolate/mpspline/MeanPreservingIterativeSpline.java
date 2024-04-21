package rtss.math.interpolate.mpspline;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.math.interpolate.ConstrainedCubicSplineInterpolator;
import rtss.math.interpolate.FunctionRangeExtenderDirect;
import rtss.math.interpolate.SteffenSplineInterpolator;
import rtss.math.interpolate.FunctionRangeExtenderMirror;
import rtss.math.interpolate.TargetPrecision;
import rtss.util.Util;
import rtss.util.plot.ChartXYSplineAdvanced;

/**
 * Generate mean-preserving spline.
 * 
 *     @param bins  Bins of average values for the intervals
 *     @param ppy   Points per year
 *     
 *     @return      Array of computed values. Size of the array is bins width in years multiplied by ppy
 *     
 * We first compute a spline that is not mean-preserving.     
 * Then we calculate the residue (per-bin difference between bin values and generated spline),
 * calculate the spline for the residue and add it to the initial result.
 * We then iteratively repeat the process until a desired precision is achieved.
 * 
 * Current implementation is meant for use with demographic data and expects positive values in bin averages 
 * (not negative and non-zero). It would be trivial though to generalize the implementation (methods "same"
 * and "eval_cp_x") to handle any data, by taking into account max and min value of the whole range of
 * bin averages (i.e. data channel height).
 * 
 * Result is not guaranteed to be non-negative but usually is. 
 */
public class MeanPreservingIterativeSpline
{
    public static double[] eval(Bin[] bins, int ppy) throws Exception
    {
        return eval(bins, ppy, null, new TargetPrecision().eachBinAbsoluteDifference(0.1));
    }

    public static double[] eval(Bin[] bins, int ppy, Options options, TargetPrecision precision) throws Exception
    {
        return new MeanPreservingIterativeSpline().do_eval(bins, ppy, options, precision);
    }

    private double[] do_eval(Bin[] bins, int ppy, Options options, TargetPrecision precision) throws Exception
    {
        if (options == null)
            options = new Options();
        else
            options = new Options(options);
        options.applyDefaults();

        double[] xx = Bins.ppy_x(bins, ppy);
        double[] result = new double[xx.length];

        // use weighted control points for the spline
        // final double[] cp_x = Bins.midpoint_x(bins);
        final double[] cp_x = weigted_control_points_x(bins, ppy, options);
        double[] cp_y = Bins.midpoint_y(bins);

        for (int pass = 0;; pass++)
        {
            double[] yy = new double[xx.length];

            /*
             * generally use Akima spline as it has less oscillations and overshooting 
             * http://www.alglib.net/interpolation/spline3.php#header5
             * https://www.researchgate.net/post/What_is_the_significance_of_Akima_spline_over_cubic_splines_or_other_curve_fitting_techniques
             * https://blogs.mathworks.com/cleve/2019/04/29/makima-piecewise-cubic-interpolation
             */
            UnivariateFunction sp = makeSpline(cp_x, cp_y, options);

            for (int k = 0; k < xx.length; k++)
                yy[k] = sp.value(xx[k]);

            for (int k = 0; k < xx.length; k++)
                result[k] += yy[k];

            /*
             * re-bin the results
             */
            double[] yearly = Bins.ppy2yearly(result, ppy);
            double[] avg = Bins.yearly2bin_avg(bins, yearly);

            if (precision.achieved(bins, avg))
                break;

            if (Util.False || options.debug)
            {
                String title = String.format("Mean-Preserving Iterative Spline [interim pass %d %s]", 
                                             pass, options.basicSplineType.getSimpleName());
                new ChartXYSplineAdvanced(title, "x", "y").showSplinePane(false)
                        .addSeries("MPS", xx, result)
                        .addSeries("bins", xx, Bins.ppy_y(bins, ppy))
                        .display();
            }
            
            if (options.debug)
            {
                Util.noop();
            }

            /*
             * calculate per-bin residue and store in updated control points for next iteration
             */
            cp_y = Bins.midpoint_y(bins);
            for (int ix = 0; ix < avg.length; ix++)
                cp_y[ix] -= avg[ix];
        }

        if (Util.False || options.debug)
        {
            String title = String.format("Mean-Preserving Iterative Spline [result %s]", 
                                         options.basicSplineType.getSimpleName());
            new ChartXYSplineAdvanced(title, "x", "y").showSplinePane(false)
                    .addSeries("MPS", xx, result)
                    .addSeries("bins", xx, Bins.ppy_y(bins, ppy))
                    .display();
        }
        
        if (options.checkNonNegative != null && options.checkNonNegative && !Util.isNonNegative(result))
            throw new Exception("Result of interpolation is negative");

        if (options.checkPositive != null && options.checkPositive && !Util.isPositive(result))
            throw new Exception("Result of interpolation is negative or zero");

        return result;
    }

    private UnivariateFunction makeSpline(final double[] cp_x, final double[] cp_y, Options options) throws Exception
    {
        PolynomialSplineFunction sp = null;
        
        if (options.basicSplineType == AkimaSplineInterpolator.class)
        {
            sp = new AkimaSplineInterpolator().interpolate(cp_x, cp_y);
        }
        else if (options.basicSplineType == SteffenSplineInterpolator.class)
        {
            sp = new SteffenSplineInterpolator().interpolate(cp_x, cp_y);
        }
        else if (options.basicSplineType == ConstrainedCubicSplineInterpolator.class)
        {
            sp = new ConstrainedCubicSplineInterpolator().interpolate(cp_x, cp_y);
        }
        else
        {
            throw new Exception("Not a valid spline type");
        }
        
        if (options.functionExtenderType == FunctionRangeExtenderDirect.class)
        {
            return new FunctionRangeExtenderDirect(sp);
        }
        else if (options.functionExtenderType == FunctionRangeExtenderMirror.class)
        {
            return new FunctionRangeExtenderMirror(sp);
        }
        else
        {
            throw new Exception("Not a valid function range extender type");
        }
    }

    private double[] weigted_control_points_x(Bin[] bins, int ppy, Options options)
    {
        double minOffset = 1.0 / ppy;
        double[] cp_x = new double[bins.length];

        for (Bin bin : bins)
        {
            double x = eval_cp_x(bin, options);
            x = max(x, bin.age_x1 + minOffset);
            x = min(x, bin.age_x2 + 1 - minOffset);
            cp_x[bin.index] = x;
        }
        return cp_x;
    }

    private double eval_cp_x(Bin bin, Options options)
    {
        Bin prev = bin.prev;
        Bin next = bin.next;
        
        if (next == null && options.placeLastBinKnotAtRightmostPoint)
            return bin.age_x2 + 1;

        // endpoint
        if (prev == null || next == null)
            return bin.mid_x;

        double a = prev.avg;
        double b = bin.avg;
        double c = next.avg;

        double xa = prev.mid_x;
        double xb = bin.mid_x;
        double xc = next.mid_x;

        if (bin.avg > prev.avg && bin.avg > next.avg)
        {
            // local maximum
            double wa = a / b;
            double wb = b / b;
            double wc = c / b;

            double x = (xa * wa + xb * wb + xc * wc) / (wa + wb + wc);
            return x;
        }
        else if (bin.avg < prev.avg && bin.avg < next.avg)
        {
            // local minimum
            double wa = b / a;
            double wb = b / b;
            double wc = b / c;

            double x = (xa * wa + xb * wb + xc * wc) / (wa + wb + wc);
            return x;
        }
        else if (same(a, b) && same(b, c))
        {
            return bin.mid_x;
        }
        else if (same(a, b))
        {
            return bin.age_x1;
        }
        else if (same(b, c))
        {
            return bin.age_x2 + 1;
        }
        else
        {
            return bin.mid_x;
        }
    }

    private boolean same(double a, double b)
    {
        double adiff = abs(a - b);
        double mass = max(abs(a), abs(b));
        return adiff / mass < 0.0001;
    }
    
    /*=======================================================================================================*/
    
    public static class Options
    {
        Boolean checkNonNegative;
        Boolean checkPositive;
        Boolean placeLastBinKnotAtRightmostPoint;
        Boolean debug;
        Class<?> basicSplineType;
        Class<?> functionExtenderType;
        
        public Options()
        {
            checkNonNegative = null;
            checkPositive = null;
            placeLastBinKnotAtRightmostPoint = null;
            debug = null;
            basicSplineType = null;
            functionExtenderType = null;
        }
        
        public Options(Options x)
        {
            checkNonNegative = x.checkNonNegative;
            checkPositive = x.checkPositive;
            placeLastBinKnotAtRightmostPoint = x.placeLastBinKnotAtRightmostPoint;
            debug = x.debug;
            basicSplineType = x.basicSplineType;
            functionExtenderType = x.functionExtenderType;
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
        
        public Options placeLastBinKnotAtRightmostPoint()
        {
            return placeLastBinKnotAtRightmostPoint(true);
        }
        
        public Options placeLastBinKnotAtRightmostPoint(boolean b)
        {
            placeLastBinKnotAtRightmostPoint = b;
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
    
        public Options functionExtenderType(Class<?> clz)
        {
            functionExtenderType = clz;
            return this; 
        }
        
        public void applyDefaults()
        {
            if (checkNonNegative == null && checkPositive == null)
                checkNonNegative = true;
            
            if (placeLastBinKnotAtRightmostPoint == null)
                placeLastBinKnotAtRightmostPoint = false;
            
            if (basicSplineType == null)
                basicSplineType = SteffenSplineInterpolator.class;
            
            if (functionExtenderType == null)
                functionExtenderType = FunctionRangeExtenderDirect.class;
            
            if (debug == null)
                debug = false;
        }
    }
}
