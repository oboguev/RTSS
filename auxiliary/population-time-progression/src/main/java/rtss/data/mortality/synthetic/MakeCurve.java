package rtss.data.mortality.synthetic;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.mortality.SingleMortalityTable;
import rtss.util.Util;
import rtss.util.plot.ChartXYSplineAdvanced;

// import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
// import com.manyangled.snowball.analysis.interpolation.MonotonicSplineInterpolator;

public class MakeCurve
{
    public static final int MAX_AGE = SingleMortalityTable.MAX_AGE;
    
    /*
     * Extract bins to yearly array
     */
    public static double[] averages(Bin... bins) throws Exception
    {
        double[] d = new double[Bins.widths_in_years(bins)];
        
        int x = 0;
        
        for (Bin bin : bins)
        {
            for (int k = 0; k < bin.widths_in_years; k++)
                d[x++] = bin.avg;
        }
        
        return d;
    }
    
    /*
     * Interpolate bins to smooth yearly curve
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
        int [] res_xi = Util.seq_int(0, MAX_AGE);
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
        
        double[] before_res_y = Util.dup(res_y);
        preserve_means(res_y, bins);
        
        if (Util.True)
        {
            new ChartXYSplineAdvanced("Generating curve", "age", "y")
                // .addSeries("averages", res_x, averages(bins))
                .addSeries("before", res_x, before_res_y)
                .addSeries("after", res_x, res_y)
                .display();        
        }
        
        return res_y;
    }
    
    static public class SplineCurve
    {
        // private PolynomialSplineFunction spline;
        private android.util.Spline aspline;
        
        public SplineCurve(double[] cp_x, double[] cp_y) throws Exception
        {
            // cannot use https://github.com/erikerlandson/snowball as it requires at least 7 points
            // spline = new MonotonicSplineInterpolator().interpolate(cp_x, cp_y);
            
            // could also use
            // https://web.archive.org/web/20190713064342/http://www.korf.co.uk/spline.pdf
            // https://jetcracker.wordpress.com/2014/12/26/constrained-cubic-spline-java
            
            // but use android.util.Spline for now
            aspline = android.util.Spline.createMonotoneCubicSpline(cp_x, cp_y);
        }
        
        public double value(double x)
        {
            // return spline.value(x);
            return aspline.interpolate(x);
        }
    }
    
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

    static private void preserve_mean(double[] y, double[] yy, Bin bin)
    {
        if (Util.False)
        {
            // scale all points
            double avg = Util.average(y);
            for (int k = 0; k < y.length; k++)
            {
                y[k] *= bin.avg / avg;
            }
        }
        
        if (Util.True)
        {
            // #####
            // scale all points except the leftmost
            double avg = Util.average(Util.splice(y, 1, y.length - 1));
            for (int k = 1; k < y.length; k++)
            {
                y[k] *= bin.avg / avg;
            }
        }
    }
}
