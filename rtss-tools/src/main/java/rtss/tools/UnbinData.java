package rtss.tools;

import java.io.File;

import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.curves.CurveVerifier;
import rtss.data.curves.TargetResolution;
import rtss.data.curves.ensure.EnsureNonNegativeCurve;
import rtss.math.interpolate.ConstrainedCubicSplineInterpolator;
import rtss.math.interpolate.SteffenSplineInterpolator;
import rtss.math.interpolate.TargetPrecision;
import rtss.math.interpolate.disaggregate.csasra.DisaggregateVariableWidthSeries;
import rtss.math.interpolate.mpspline.MeanPreservingIntegralSpline;
import rtss.math.interpolate.mpspline.MeanPreservingIterativeSpline;
import rtss.math.pclm.PCLM_Rizzi_2015;
import rtss.util.Clipboard;
import rtss.util.UI;
import rtss.util.Util;

public class UnbinData
{
    public static void main(String[] args)
    {
        try
        {
            new UnbinData().do_main();
            Util.out("*** Result was placed on the clipboard.");
        }
        catch (Throwable ex)
        {
            Util.err("Exception: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private void do_main() throws Exception
    {
        String text = Clipboard.getText();
        if (text == null || text.length() == 0)
            throw new Exception("No data on the clipboard");

        Bin[] bins = Bins.fromString(text);
        
        StringBuilder sb = new StringBuilder();
        addOutput(sb, "CSASRA", bins, unbin_csasra(bins));
        addOutput(sb, "SPLINE", bins, unbin_spline(bins));

        // text = Bins.asString(bins);
        
        text = sb.toString();

        if (File.separatorChar == '\\')
            text = text.replace("\n", "\r\n");

        Clipboard.put(text);
    }
    
    /* ============================================================================================================= */

    private void addOutput(StringBuilder sb, String method, Bin[] bins, double[] yy)
    {
        if (sb.length() != 0)
        {
            sb.append("\n");
            sb.append("###########################################################################\n");
            sb.append("\n");
        }
            
        if (yy == null)
        {
            sb.append(String.format("Method %s failed", method));
        }
        else
        {
            sb.append("\n");
            sb.append(String.format("Unbinned with method %s:", method));
            sb.append("\n");

            for (int k = 0; k < yy.length; k++)
            {
                // assuming x-scale step by 1
                double dyear = bins[0].age_x1 + k;
                Integer iyear = asInteger(dyear);

                if (iyear != null)
                {
                    sb.append(String.format("%d %f", iyear, yy[k]));
                }
                else
                {
                    sb.append(String.format("%f %f", dyear, yy[k]));
                }
            }
        }
    }

    private static Integer asInteger(double v)
    {
        if (Double.isNaN(v) || Double.isInfinite(v))
            return null;

        if (v < Integer.MIN_VALUE || v > Integer.MAX_VALUE)
            return null;

        int i = (int) v;
        return (v == i) ? Integer.valueOf(i) : null;
    }
    
    /* ============================================================================================================= */

    private double[] unbin_csasra(Bin[] bins)
    {
        // final int ppy = 1;
        // final double[] xxx = Bins.ppy_x(bins, ppy);
        final double[] averages = Bins.midpoint_y(bins);

        final int[] intervalWidths = Bins.widths(bins);
        final int maxIterations = 5000;
        final double positivityThreshold = 1e-6;
        final double maxConvergenceDifference = 1e-3;
        final double smoothingSigma = 1.0;

        try
        {
            double[] yyy = DisaggregateVariableWidthSeries.disaggregate(averages,
                                                                        intervalWidths,
                                                                        maxIterations,
                                                                        smoothingSigma,
                                                                        positivityThreshold,
                                                                        maxConvergenceDifference,
                                                                        true);

            if (!Util.isNonNegative(yyy))
                throw new Exception("Error calculating curve (negative value)");

            CurveVerifier.validate_means(yyy, bins);
            
            return yyy;
        }
        catch (Exception ex)
        {
            UI.messageBox("Unable to unbin with CSASRA: " + ex.getLocalizedMessage(), "Acknowledge");
            return null;
        }
    }

    /* ============================================================================================================= */

    private double[] unbin_spline(Bin[] bins)
    {
        final int ppy = 1;
        final TargetResolution targetResolution = TargetResolution.YEARLY;
        
        String title = "clipboard data";

        TargetPrecision precision = new TargetPrecision().eachBinRelativeDifference(0.001);
        MeanPreservingIterativeSpline.Options splineOptions = new MeanPreservingIterativeSpline.Options()
                .checkPositive(false);

        if (Util.True)
        {
            /*
             * Helps to avoid the last segment of the curve dive down too much
             */
            splineOptions = splineOptions.placeLastBinKnotAtRightmostPoint();
        }

        // double[] xxx = Bins.ppy_x(bins, ppy);

        double[] yyy1 = null;
        double[] yyy2 = null;
        double[] yyy3 = null;
        double[] yyy4 = null;
        double[] yyy5 = null;

        try
        {
            if (Util.False)
            {
                splineOptions.basicSplineType(SteffenSplineInterpolator.class);
                yyy1 = MeanPreservingIterativeSpline.eval(bins, ppy, splineOptions, precision);
            }

            if (Util.False)
            {
                splineOptions.basicSplineType(AkimaSplineInterpolator.class);
                yyy2 = MeanPreservingIterativeSpline.eval(bins, ppy, splineOptions, precision);
            }

            if (Util.True)
            {
                splineOptions.basicSplineType(ConstrainedCubicSplineInterpolator.class);
                yyy3 = MeanPreservingIterativeSpline.eval(bins, ppy, splineOptions, precision);
            }

            if (Util.False)
            {
                MeanPreservingIntegralSpline.Options xoptions = new MeanPreservingIntegralSpline.Options();
                xoptions = xoptions.ppy(ppy).debug_title(title).basicSplineType(ConstrainedCubicSplineInterpolator.class);
                xoptions = xoptions.splineParams("title", title);
                // do not use f2.trends since it over-determines the spline and makes value of s' discontinuous between segments 
                // options = options.splineParams("f2.trends", trends);
                yyy4 = MeanPreservingIntegralSpline.eval(bins, xoptions);
            }

            if (Util.False)
            {
                final double lambda = 0.0001;
                yyy5 = PCLM_Rizzi_2015.pclm(bins, lambda, ppy);
            }

            double[] yyy = yyy1;
            if (yyy == null)
                yyy = yyy2;
            if (yyy == null)
                yyy = yyy3;
            if (yyy == null)
                yyy = yyy4;
            if (yyy == null)
                yyy = yyy5;

            yyy = EnsureNonNegativeCurve.ensureNonNegative(yyy, bins, title, targetResolution);

            if (!Util.isNonNegative(yyy))
                throw new Exception("Error calculating curve (negative value)");

            double[] yy = Bins.ppy2yearly(yyy, ppy);

            if (!Util.isNonNegative(yy))
                throw new Exception("Error calculating curve (negative value)");

            CurveVerifier.validate_means(yy, bins);
            
            return yy;
        }
        catch (Exception ex)
        {
            UI.messageBox("Unable to unbin with SPLINE: " + ex.getLocalizedMessage(), "Acknowledge");
            
            return null;
        }
    }
}
