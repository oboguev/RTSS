package rtss.tools;

import rtss.data.selectors.SumOrAverage;
import rtss.util.Util;

/*
 * Disaggregate data expressed as sum per bin,
 * such as population per age group 
 * 
 * Input data on the clipboard is a sequence of lines: 
 *  
 *      year-year value
 *      year-year value
 *      year      value
 *      year      value
 *      year-year value
 *      year-year value
 *  
 * First part of a line can be a range of a single-year value (bin width = 1). 
 *  
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
