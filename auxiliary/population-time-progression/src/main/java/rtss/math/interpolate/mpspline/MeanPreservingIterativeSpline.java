package rtss.math.interpolate.mpspline;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
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
 */
public class MeanPreservingIterativeSpline
{
    public static double[] eval(Bin[] bins, int ppy) throws Exception
    {
        return eval(bins, ppy, new TargetPrecision().eachBinAbsoluteDifference(0.1));
    }

    public static double[] eval(Bin[] bins, int ppy, TargetPrecision precision) throws Exception
    {
        return new MeanPreservingIterativeSpline().do_eval(bins, ppy, precision);
    }

    private double[] do_eval(Bin[] bins, int ppy, TargetPrecision precision) throws Exception
    {
        double[] xx = Bins.ppy_x(bins, ppy);
        double[] result = new double[xx.length];

        // use weighted control points for the spline
        // final double[] cp_x = Bins.midpoint_x(bins);
        final double[] cp_x = weigted_control_points_x(bins, ppy);
        double[] cp_y = Bins.midpoint_y(bins);

        for (;;)
        {
            double[] yy = new double[xx.length];

            /*
             * use Akima spline as it has less oscillations and overshooting 
             * http://www.alglib.net/interpolation/spline3.php#header5
             * https://www.researchgate.net/post/What_is_the_significance_of_Akima_spline_over_cubic_splines_or_other_curve_fitting_techniques
             * https://blogs.mathworks.com/cleve/2019/04/29/makima-piecewise-cubic-interpolation
             */
            PolynomialSplineFunction sp = new AkimaSplineInterpolator().interpolate(cp_x, cp_y);

            for (int k = 0; k < xx.length; k++)
                yy[k] = splineValue(sp, xx[k]);

            for (int k = 0; k < xx.length; k++)
                result[k] += yy[k];

            /*
             * re-bin the results
             */
            double[] yearly = Bins.ppy2yearly(result, ppy);
            double[] avg = Bins.yearly2bin_avg(bins, yearly);

            if (precision.achieved(bins, avg))
                break;

            if (Util.False)
            {
                new ChartXYSplineAdvanced("Mean-Preserving Iterative Spline [interim]", "x", "y")
                        .addSeries("MPS", xx, result)
                        .addSeries("bins", xx, Bins.ppy_y(bins, ppy))
                        .display();
            }

            /*
             * calculate residue
             */
            cp_y = Bins.midpoint_y(bins);
            for (int ix = 0; ix < avg.length; ix++)
                cp_y[ix] -= avg[ix];
        }

        if (Util.True)
        {
            new ChartXYSplineAdvanced("Mean-Preserving Iterative Spline [result]", "x", "y")
                    .addSeries("MPS", xx, result)
                    .addSeries("bins", xx, Bins.ppy_y(bins, ppy))
                    .display();
        }

        return result;
    }

    private double splineValue(PolynomialSplineFunction sp, double x) throws Exception
    {
        if (sp.isValidPoint(x))
            return sp.value(x);

        double[] knots = sp.getKnots();
        PolynomialFunction[] pf = sp.getPolynomials();

        if (pf.length != knots.length - 1)
            throw new Exception("Unexpected PolynomialSplineFunction ");

        if (x < knots[0])
        {
            return pf[0].value(x - knots[0]);
        }
        else if (x > knots[knots.length - 1])
        {
            return pf[pf.length - 1].value(x - knots[knots.length - 1]);
        }
        else
        {
            return sp.value(x);
        }
    }

    private double[] weigted_control_points_x(Bin[] bins, int ppy)
    {
        double minOffset = 1.0 / ppy;
        double[] cp_x = new double[bins.length];

        for (Bin bin : bins)
        {
            double x = eval_cp_x(bin);
            x = max(x, bin.age_x1 + minOffset);
            x = min(x, bin.age_x2 + 1 - minOffset);
            cp_x[bin.index] = x;
        }
        return cp_x;
    }

    private double eval_cp_x(Bin bin)
    {
        Bin prev = bin.prev;
        Bin next = bin.next;

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
}
