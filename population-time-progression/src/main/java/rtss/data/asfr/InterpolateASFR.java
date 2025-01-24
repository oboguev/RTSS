package rtss.data.asfr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtss.data.bin.Bin;

/*
 * ВременнАя интерполяция коэффциентов плодовитости из годового в суб-годовой диапазон 
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
    
    private static double[] yearly2timepoints(double[] yearly, int ppy, int ppinterval)
    {
        // ####@@@@@
        return null;
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
    
    /* ======================================================================================= */
    
    private static double[] yearly2points(double[] yearly, int npoints)
    {
        // ####@@@@@ see InterpolatePopulationAsMeanPreservingCurve
        return null;
    }
    
}
