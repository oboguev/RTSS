package rtss.data.asfr;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.curves.CurveVerifier;
import rtss.data.curves.TargetResolution;
import rtss.data.curves.refine.RefineYearlyPopulation;
import rtss.data.selectors.Gender;
import rtss.math.interpolate.disaggregate.csasra.DisaggregateVariableWidthSeries;
import rtss.util.Util;
import rtss.util.plot.ChartXYSplineAdvanced;

/*
 * Возрастная интерполяция возрастных коэффициентов плодовитости 
 *   - из агрегированного по возрасту вида  
 *   - в годовое по возрасту разрешение. 
 */
public class InterpolateASFR_ByAge
{
    public static AgeSpecificFertilityRates interpolate(AgeSpecificFertilityRates asfr) throws Exception
    {
        Bin[] bins = asfr.binsReadonly(); 
        // ###
        return null;
    }
    
    private static class CurveResult
    {
        public final String method;
        public final double[] curve;
        public final double[] raw;

        public CurveResult(String method, double[] curve)
        {
            this.method = method;
            this.curve = curve;
            this.raw = null;
        }

        public CurveResult(String method, double[] curve, double[] raw)
        {
            this.method = method;
            this.curve = curve;
            this.raw = raw;
        }
    }

    private static CurveResult curve_csasra(
            Bin[] bins,
            String title)
            throws Exception
    {
        final int ppy = 1;
        double[] xxx = Bins.ppy_x(bins, ppy);
        double[] averages = Bins.midpoint_y(bins);

        int[] intervalWidths = Bins.widths(bins);
        int maxIterations = 5000;
        double positivityThreshold = 1e-6;
        double maxConvergenceDifference = 1e-3;

        /*
         * For yearly data (targetResolution == YEARLY) use sigma around 1.0 (0.5-1.5).
         * Difference of results within this range is typically miniscule.
         * 
         * For daily data (targetResolution == DAILY) use sigma around 10.0.
         */
        
        /*
        double smoothingSigma;
        switch (targetResolution)
        {
        case YEARLY:
            smoothingSigma = 1.0;
            break;

        case DAILY:
            smoothingSigma = 10.0;
            break;

        default:
            throw new IllegalArgumentException();
        }

        double[] yyy = DisaggregateVariableWidthSeries.disaggregate(averages,
                                                                    intervalWidths,
                                                                    maxIterations,
                                                                    smoothingSigma,
                                                                    positivityThreshold,
                                                                    maxConvergenceDifference);

        if (!Util.isNonNegative(yyy))
            throw new Exception("Error calculating curve (negative value)");

        double[] yy = Bins.ppy2yearly(yyy, ppy);

        if (!Util.isNonNegative(yy))
            throw new Exception("Error calculating curve (negative value)");

        CurveVerifier.validate_means(yy, bins);

            return new CurveResult("csasra", yy);
        */
        return null;
    }
}
