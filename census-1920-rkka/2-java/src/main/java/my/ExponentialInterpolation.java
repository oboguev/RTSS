package my;

public class ExponentialInterpolation
{
    /*
     * Modify yearly[yx1...yx2] such that average(yearly[yx1...yx2]) = avg and projected yearly[yx2 + 1] = v2 
     */
    public static void resample_next(double[] yearly, int yx1, int yx2, double avg, double v2)
    {
        double a = calc_a(yx1, yx2, avg, v2);
        
        double v = v2;
        for (int yx = yx2; yx >= yx1; yx--)
        {
            v /= a;
            yearly[yx] = v;
        }
    }
    
    private static double calc_avg(int yx1, int yx2, double v2, double a)
    {
        double sum = 0;
        double v = v2;

        for (int yx = yx2; yx >= yx1; yx--)
        {
            v /= a;
            sum += v;
        }
        
        return sum / (yx2 - yx1 + 1);
    }
    
    private static double calc_a(int yx1, int yx2, double avg, double v2)
    {
        double low = 0.00001;
        double high = 1;
        
        double avg_low = calc_avg(yx1, yx2, v2, low); 
        double avg_high = calc_avg(yx1, yx2, v2, high); 
        
        for (;;)
        {
            double mid = (low + high) / 2;
            double avg_mid = calc_avg(yx1, yx2, v2, mid);
            if (avg_mid >= avg)
            {
                low = mid;
                avg_low = avg_mid; 
            }
            else
            {
                high = mid;
                avg_high = avg_mid; 
            }
            
            if (Math.abs(avg_high - avg_low) < 0.0001)
                return mid;
        }
    }
}
