package rtss.data.asfr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.math.interpolate.ConstrainedCubicSplineInterpolator;
import rtss.math.interpolate.TargetPrecision;
import rtss.math.interpolate.mpspline.MeanPreservingIterativeSpline;
import rtss.util.Util;

/*
 * ВременнАя интерполяция возрастных коэффциентов плодовитости из годового в суб-годовое разрешение
 * по временной (хронологической. не возрастной) шкале. 
 */
public class InterpolateASFR
{
    /*
     * Интерполировать коэффициенты в диапазоне лет [year1 ... year2].
     * @ppy = points per year.
     * Индексы точек имеют вид "year.number", где number в диапазоне [0 ... ppy - 1].
     * 
     * Например, если ppy = 4, то для 1940 года создаются точки 1940.0, 1940.1, 1940.2 и 1940.3.
     */
    static public AgeSpecificFertilityRatesByTimepoint interpolate(AgeSpecificFertilityRatesByYear asfry, int year1, int year2, int ppy) throws Exception
    {
        final int default_ppinterval = 20;
        return interpolate(asfry, year1, year2, ppy, default_ppinterval);
    }

    /*
     * @ppinterval указывает, сколько точек интерполяции создавать внутри одного диапазона для усреднения их значений. 
     */
    static public AgeSpecificFertilityRatesByTimepoint interpolate(AgeSpecificFertilityRatesByYear asfry, int year1, int year2, int ppy, int ppinterval) throws Exception
    {
        Map<String, List<Bin>> tp2bins = new HashMap<>();
        
        for (String ageGroup : asfry.ageGroups())
        {
            int age_x1 = ag_x1(ageGroup);
            int age_x2 = ag_x2(ageGroup);
            
            double[] yearly = asfry.ageGroupValues(ageGroup, year1, year2);
            nonzero(yearly);
            double[] points = yearly2timepoints(yearly, ppy, ppinterval);
            
            if (points.length != ppy * (year2 - year1 + 1))
                throw new Exception("ошибка вычисления");
            
            int pk = 0;
            for (int year = year1; year <= year2; year++)
            {
                for (int ppx = 0; ppx < ppy; ppx++)
                {
                    String tp = String.format("%d.%d", year, ppx);
                    List<Bin> binlist = tp2bins.get(tp);
                    if (binlist == null)
                    {
                        binlist = new ArrayList<Bin>(); 
                        tp2bins.put(tp, binlist);
                    }
                    
                    double v = points[pk++];
                    binlist.add(new Bin(age_x1, age_x2, v));
                }
            }
        }
        
        AgeSpecificFertilityRatesByTimepoint asfrtp = new AgeSpecificFertilityRatesByTimepoint(); 

        for (String tp : tp2bins.keySet())
        {
            List<Bin> binlist = tp2bins.get(tp);
            AgeSpecificFertilityRates asfr = new AgeSpecificFertilityRates(binlist);
            asfrtp.setForTimepoint(tp, asfr);
        }
        
        return asfrtp;
    }
    
    private static double[] yearly2timepoints(double[] yearly, int ppy, int ppinterval) throws Exception
    {
        double[] points = yearly2points(yearly, ppy * ppinterval);
        double[] timepoints = new double[ppy * yearly.length];
        
        for (int k = 0; k < timepoints.length; k++)
        {
            double[] x = Util.splice(points, k * ppinterval, k * ppinterval + ppinterval - 1);
            timepoints[k] = Util.average(x);
        }

        return timepoints;
    }
    
    /* ======================================================================================= */

    private static int ag_x1(String ageGroup) throws Exception
    {
        return ag_x(ageGroup, 0);
    }

    private static int ag_x2(String ageGroup) throws Exception
    {
        return ag_x(ageGroup, 1);
    }

    private static int ag_x(String ageGroup, int index) throws Exception
    {
        String[] sa = ageGroup.split("-");
        if (sa.length != 2)
            throw new IllegalArgumentException();
        return Integer.parseInt(sa[index]);
    }
    
    /*
     * заменить нулевые значения очень низкими,
     * чтобы избежать деления на ноль в построителе сплайна
     */
    private static void nonzero(double[] y)
    {
        for (int k = 0; k < y.length; k++)
        {
            if (y[k] >= 0 && y[k] < 0.0000000001)
                y[k] =  0.0000000001;
        }
    }
    
    /* ======================================================================================= */
    
    private static double[] yearly2points(double[] yearly, int ppy) throws Exception
    {
        Bin[] bins = Bins.fromValues(yearly);
        
        TargetPrecision precision = new TargetPrecision().eachBinRelativeDifference(0.001);
        MeanPreservingIterativeSpline.Options options = new MeanPreservingIterativeSpline.Options()
                .checkPositive(false);

        if (Util.False)
        {
            /*
             * Helps to avoid the last segment of the curve dive down too much
             */
            options = options.placeLastBinKnotAtRightmostPoint();
        }
        
        // double[] xxx = Bins.ppy_x(bins, ppy);
        options.basicSplineType(ConstrainedCubicSplineInterpolator.class);
        double[] yyy = MeanPreservingIterativeSpline.eval(bins, ppy, options, precision);

        if (Util.False && !Util.isNonNegative(yyy))
            throw new Exception("Error calculating curve (negative value)");
        
        double yyy_min = Util.min(yyy);
        if (yyy_min < -2.0)
            throw new Exception("Error calculating curve (negative value)");
        
        double[] yy = Bins.ppy2yearly(yyy, ppy);

        if (!Util.isNonNegative(yy))
            throw new Exception("Error calculating curve (negative value)");

        validate_means(yy, bins);
        
        return yyy;
    }
    
    /*
     * Verify that the curve preserves mean values as indicated by the bins
     */
    static void validate_means(double[] yy, Bin... bins) throws Exception
    {
        for (Bin bin : bins)
        {
            double[] y = Util.splice(yy, bin.age_x1, bin.age_x2);
            if (Util.differ(Util.average(y), bin.avg, 0.001))
                throw new Exception("Curve does not preserve mean values of the bins");
        }
    }
}
