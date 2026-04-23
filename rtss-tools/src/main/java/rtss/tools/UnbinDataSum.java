package rtss.tools;

import rtss.data.selectors.SumOrAverage;
import rtss.util.Util;

/*
 * Disaggregate data expressed as sum per bin,
 * such as population per age group 
 */
public class UnbinDataSum
{
    public static void main(String[] args)
    {
        try
        {
            new UnbinDataAverage().do_main(SumOrAverage.SUM);
            Util.out("*** Result was placed on the clipboard.");
        }
        catch (Throwable ex)
        {
            Util.err("Exception: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
