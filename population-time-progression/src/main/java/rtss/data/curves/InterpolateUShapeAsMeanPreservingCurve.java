package rtss.data.curves;

import java.util.HashSet;

import javax.validation.ConstraintViolationException;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.math.interpolate.ConstrainedCubicSplineInterpolator;
import rtss.math.interpolate.TargetPrecision;
import rtss.math.interpolate.mpspline.MeanPreservingIterativeSpline;
import rtss.util.Clipboard;
import rtss.util.Util;
import rtss.util.plot.ChartXYSplineAdvanced;

/**
 * Interpolate aggregated bins to a smooth yearly curve, in a mean-preserving way.
 * Typically used to interpolate the "qx" curve from an aggregated multi-year data to a yearly resolution.
 * 
 * Works only with U-shaped data consisting of two monotonic segments.
 */
public class InterpolateUShapeAsMeanPreservingCurve
{
    /*
     * Currently implemented for U-shaped data, although the same approach can be used for inverse U-shape.
     * 
     * 1. Find minimum-value bin.
     * 2. Split the bins into two monotonic parts: left (descending) and right (ascending).
     * 3. Interpolate each part using monotonic mean-preserving spline.
     * 4. For the minimum bin, take average of two interpolations.  
     */
    public static double[] curve(Bin... bins) throws Exception, ConstraintViolationException
    {
        String debug_title = null;
        return curve(debug_title, bins);
    }
    
    public static double[] curve(String debug_title, Bin... bins) throws Exception, ConstraintViolationException
    {
        if (Bins.flips(bins) > 1)
            throw new Exception("Mortality curve has multiple local minumums and maximums");
        
        Bin firstBin = Bins.firstBin(bins);
        Bin lastBin = Bins.lastBin(bins);

        Bin maxBin = Bins.findMaxBin(bins);
        if (maxBin != firstBin && maxBin != lastBin)
            throw new Exception("Mortality curve has an unexpected maximum in the middle");

        // minimum value bin
        Bin minBin = Bins.findMinBin(bins);
        if (minBin == firstBin || minBin == lastBin)
            throw new Exception("Mortality minimum is not in the middle");
        
        Bin[] leftSet = Bins.subset(firstBin, minBin);
        Bin[] rightSet = Bins.subset(minBin, lastBin);
        int ppy = 1000;
        
        double[] lcurve = bins2curve(leftSet, ppy);
        double[] rcurve = bins2curve(rightSet, ppy);
        
        double[] yyy = joinCurves(lcurve, rcurve, ppy, minBin);
        
        yyy = EnsurePositiveCurve.ensurePositive(yyy, bins);
        
        if (Util.False)
        {
            double[] xxx = Bins.ppy_x(bins, ppy);
            Clipboard.put(" ", xxx, yyy);
        }

        if (Util.False)
        {
            double[] xxx = Bins.ppy_x(bins, ppy);
            ChartXYSplineAdvanced chart = new ChartXYSplineAdvanced("Make curve", "x", "y");
            chart.addSeries("curve", xxx, yyy);
            chart.addSeries("bins", xxx, Bins.ppy_y(bins, ppy));
            chart.display();
        }

        if (!Util.isPositive(yyy))
            throw new ConstraintViolationException("Error calculating curve (negative or zero value)", new HashSet<>());

        double[] yy = Bins.ppy2yearly(yyy, ppy);

        CurveUtil.validate_means(yy, bins);

        return yy;
    }
    
    private static double[] bins2curve(Bin[] bins, int ppy) throws Exception
    {
        TargetPrecision precision = new TargetPrecision().eachBinRelativeDifference(0.001);
        MeanPreservingIterativeSpline.Options options = new MeanPreservingIterativeSpline.Options()
                .checkPositive(false);
        // options = options.debug();
        options.basicSplineType(ConstrainedCubicSplineInterpolator.class);
        // options.basicSplineType(AkimaSplineInterpolator.class);
        // options.basicSplineType(SteffenSplineInterpolator.class);
        return MeanPreservingIterativeSpline.eval(bins, ppy, options, precision);
    }
    
    private static double[] joinCurves(double[] left, double[] right, int ppy, Bin minBin)
    {
        int overlap = minBin.widths_in_years * ppy;
        double[] y = new double[left.length + right.length - overlap];
        
        /* left non-overlapped part */
        for (int k = 0; k < left.length - overlap; k++)
            y[k] = left[k];
        
        /* right non-overlapped part */
        for (int k = overlap; k < right.length; k++)
            y[left.length + (k - overlap)] = right[k];

        /* overlapped part */
        for (int k = 0; k < overlap; k++)
        {
            double v1 = left[left.length - overlap + k];
            double v2 = right[k];
            y[left.length - overlap + k] = (v1 + v2) / 2;
        }
        
        return y;
    }
}
