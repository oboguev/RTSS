package rtss.data.mortality.synthetic;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.mortality.SingleMortalityTable;
import rtss.util.Util;
import rtss.util.plot.ChartXYSplineAdvanced;

// import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
// import com.manyangled.snowball.analysis.interpolation.MonotonicSplineInterpolator;

public class MakeCurve_Old
{
    public static final int MAX_AGE = SingleMortalityTable.MAX_AGE;

    /*
     * Interpolate bins to a smooth yearly curve
     */
    public static double[] curve(Bin... bins) throws Exception
    {
        /*
         * Mortality curve is expected to be U-shaped.
         * 
         * Our approach:
         * 
         * 1. Find minimum-value bin.
         * 2. Split the bins into two monotonic parts: left (descending) and right (ascending).
         * 3. Interpolate each part using monotonic spline.
         * 4. Adjust the result to keep it mean-preserving for every bin. 
         */
        if (Bins.flips(bins) > 1)
            throw new Exception("Mortality curve has multiple local minumums and maximums");

        Bin maxBin = Bins.findMaxBin(bins);
        if (maxBin != Bins.firstBin(bins) && maxBin != Bins.lastBin(bins))
            throw new Exception("Mortality curve has an unexpected maximum in the middle");

        // minimum value bin
        Bin minBin = Bins.findMinBin(bins);

        // spline control points, whole range
        double[] all_cp_x = Bins.midpoint_x(bins);
        double[] all_cp_y = Bins.midpoint_y(bins);

        // output ranges
        int[] res_xi = Util.seq_int(0, MAX_AGE);
        double[] res_x = Util.seq_double(res_xi);
        double[] res_y = new double[res_x.length];

        // spline values for the left part
        if (minBin.index != 0)
        {
            double[] cp_x = Util.splice(all_cp_x, 0, minBin.index);
            double[] cp_y = Util.splice(all_cp_y, 0, minBin.index);
            SplineCurve sc = new SplineCurve(cp_x, cp_y);
            for (int age = 0; age <= Math.floor(minBin.mid_x); age++)
            {
                res_y[age] = sc.value(res_x[age]);
            }
        }

        // spline values for the right part
        if (minBin.index != all_cp_x.length - 1)
        {
            double[] cp_x = Util.splice(all_cp_x, minBin.index, all_cp_x.length - 1);
            double[] cp_y = Util.splice(all_cp_y, minBin.index, all_cp_x.length - 1);
            SplineCurve sc = new SplineCurve(cp_x, cp_y);
            for (int age = (int) Math.ceil(minBin.mid_x); age <= MAX_AGE; age++)
            {
                res_y[age] = sc.value(res_x[age]);
            }
        }

        @SuppressWarnings("unused")
        double[] before_res_y = Util.dup(res_y);
        preserve_means(res_y, bins);
        validate_means(res_y, bins);

        if (Util.True)
        {
            new ChartXYSplineAdvanced("Generating mean-preserving curve", "age", "y")
                    .addSeries("averages", res_x, Bins.bins2yearly(bins))
                    .addSeries("after", res_x, res_y)
                    // .addSeries("before", res_x, before_res_y)
                    .display();
        }

        return res_y;
    }

    static public class SplineCurve
    {
        // private PolynomialSplineFunction spline;
        private rtss.math.interpolate.FritschCarlsonMonotonicSpline aspline;
        private double cp_x_last;

        public SplineCurve(double[] cp_x, double[] cp_y) throws Exception
        {
            // cannot use https://github.com/erikerlandson/snowball as it requires at least 7 points
            // spline = new MonotonicSplineInterpolator().interpolate(cp_x, cp_y);

            // could also use
            // https://web.archive.org/web/20190713064342/http://www.korf.co.uk/spline.pdf
            // https://jetcracker.wordpress.com/2014/12/26/constrained-cubic-spline-java

            // but use android.util.Spline for now
            aspline = rtss.math.interpolate.FritschCarlsonMonotonicSpline.createMonotoneCubicSpline(cp_x, cp_y);
            cp_x_last = cp_x[cp_x.length - 1];
        }

        public double value(double x)
        {
            if (x > cp_x_last)
            {
                double y_last = value(cp_x_last);
                return 2 * y_last - value(2 * cp_x_last - x);
            }
            else
            {
                // return spline.value(x);
                return aspline.interpolate(x);
            }
        }
    }

    /*
     * Check that the curve preserves mean values as indicated by the bins
     */
    static private void validate_means(double[] yy, Bin... bins) throws Exception
    {
        for (Bin bin : bins)
        {
            double[] y = Util.splice(yy, bin.age_x1, bin.age_x2);
            if (Util.differ(Util.average(y), bin.avg))
                throw new Exception("Curve does not preserve mean values of the bins");
        }
    }

    /*
     * Modify the curve so it preserves mean values of the ranges as indicated by the bins
     */
    static private void preserve_means(double[] yy, Bin... bins)
    {
        for (Bin bin : bins)
        {
            if (bin.widths_in_years == 1)
            {
                yy[bin.age_x1] = bin.avg;
            }
            else
            {
                double[] y = Util.splice(yy, bin.age_x1, bin.age_x2);
                preserve_mean(y, yy, bin);
                Util.insert(yy, y, bin.age_x1);
            }
        }
    }

    /*
     * Modify the curve segment so it preserves mean value of the range as indicated by the bin
     */
    static private void preserve_mean(double[] y, double[] yy, Bin bin)
    {
        int ixLeft = 0;
        int ixRight = y.length - 1;

        double avg = Util.average(y);
        double scale = bin.avg / avg;

        // constraints for the movement of left and right endpoints
        ValueConstraint leftConstraint = leftConstraint(bin, yy);
        ValueConstraint rightConstraint = rightConstraint(bin, yy);

        if (!leftConstraint.any(scale * y[ixLeft]) && !rightConstraint.any(scale * y[ixRight]))
        {
            /*
             * endpoints are not constrained, scale the whole range
             */
            scalePoints(y, bin, false, false);
        }
        else if (leftConstraint.any(scale * y[ixLeft]) && rightConstraint.any(scale * y[ixRight]))
        {
            /*
             * both endpoints are constrained
             */
            y[ixLeft] = leftConstraint.apply(y[ixLeft] * scale);
            y[ixRight] = rightConstraint.apply(y[ixRight] * scale);

            // scale all inner points except the leftmost and rightmost ones
            scalePoints(y, bin, true, true);
        }
        else
        {
            /*
             * Only one of endpoints is constrained
             */

            // move left endpoint as much as we can
            y[ixLeft] = leftConstraint.apply(y[ixLeft] * scale);

            // move right endpoint as much as we can
            double sum = Util.sum(Util.splice(y, ixLeft + 1, ixRight));
            scale = (bin.avg * bin.widths_in_years - y[ixLeft]) / sum;
            y[ixRight] = rightConstraint.apply(y[ixRight] * scale);

            // move inner points except the leftmost and rightmost ones
            scalePoints(y, bin, true, true);
        }
        
        /*
         * Ad-hoc: if this is an increasing bin and the left point is the same value
         * as the endpoint of the preceding bin, try to raise it, in order to keep
         * the smoothness of the curve.
         */
        if (bin.prev != null && bin.avg > bin.prev.avg &&
                y[1] > y[0] &&
                y[ixLeft] >= yy[bin.prev.age_x2] &&
                y[ixLeft] - yy[bin.prev.age_x2] < 0.3 * (y[1] - y[0]))
        {
            y[0] += (y[1] - y[0]) / 2;

            // move inner points except the leftmost and rightmost ones 
            // (except if the last bin, then can also move the rightmost point)
            scalePoints(y, bin, true, bin.next != null);
            
            // TODO: if y[0] is now >= y[1], reduce the increase 
        }
    }
    
    private static void scalePoints(double[] y, Bin bin, boolean fixLeft, boolean fixRight)
    {
        final int ixLeft = 0;
        final int ixRight = y.length - 1;
        
        int range_x1 = ixLeft;
        if (fixLeft) 
            range_x1++;

        int range_x2 = ixRight;
        if (fixRight) 
            range_x2--;

        double range_sum = Util.sum(Util.splice(y, range_x1, range_x2));
        
        double target_sum = bin.avg * bin.widths_in_years;
        if (fixLeft)
            target_sum -= y[ixLeft];
        if (fixRight)
            target_sum -= y[ixRight];
        
        double scale = target_sum / range_sum;
        for (int x = range_x1; x <= range_x2; x++)
            y[x] *= scale;
    }

    public static class ValueConstraint
    {
        public Double min;
        public Double max;
        public Double eq;
        
        public boolean any(double v)
        {
            return Util.differ(v, apply(v));
        }

        public double apply(double v)
        {
            if (eq != null)
                return eq;

            if (max != null)
                v = Math.min(v, max);

            if (min != null)
                v = Math.max(v, min);

            return v;
        }
    }

    static private ValueConstraint leftConstraint(Bin bin, double[] yy)
    {
        ValueConstraint c = new ValueConstraint();
        Bin prev = bin.prev;

        if (prev != null)
        {
            if (prev.avg > bin.avg)
            {
                c.max = yy[prev.age_x2];
            }
            else if (prev.avg < bin.avg)
            {
                c.min = yy[prev.age_x2];
            }
            else // if (prev.avg == bin.avg)
            {
                c.eq = yy[prev.age_x2];
            }
        }

        return c;
    }

    static private ValueConstraint rightConstraint(Bin bin, double[] yy)
    {
        ValueConstraint c = new ValueConstraint();
        Bin next = bin.next;

        if (next != null)
        {
            if (next.avg > bin.avg)
            {
                c.max = yy[next.age_x1];
            }
            else if (next.avg < bin.avg)
            {
                c.min = yy[next.age_x1];
            }
            else // if (next.avg == bin.avg)
            {
                c.eq = yy[next.age_x1];
            }
        }

        return c;
    }
}
