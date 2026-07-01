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
            double mx = MortalityUtil.do_not_use__qx2mx(qx);
            
            mx = 0.243;
            qx = MortalityUtil.do_not_use__mx2qx(mx);
            
            Util.noop();
        }
        catch (Exception ex)
        {
            Util.err("** Exception: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    }
}
