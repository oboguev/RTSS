package rtss.tools;

import rtss.data.mortality.MortalityUtil;
import rtss.util.Util;

public class Miscellaneous
{
    public static void main(String[] args)
    {
        try
        {
            double qx = 0.216;
            double mx = MortalityUtil.qx2mx(qx);
            
            mx = 0.243;
            qx = MortalityUtil.mx2qx(mx);
            
            final double lambda = 0.000123;
            String s1 = String.format("%s", lambda);
            String s2 = String.format("%f", lambda);
            String s3 = String.format("%g", lambda);
            String s4 = String.format("%e", lambda);
            
            Util.noop();
        }
        catch (Exception ex)
        {
            Util.err("** Exception: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    }
}
