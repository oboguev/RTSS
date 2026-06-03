package rtss.tools;

import java.io.File;

import rtss.math.interpolate.disaggregate.wcsasra.DecomposeCbrCdr;
import rtss.tools.util.UnbinCbrCdrCommand;
import rtss.tools.util.UnbinCbrCdrCommand.Command;
import rtss.util.Clipboard;
import rtss.util.Util;

/*
 * Disaggregate CBR and CDR values.
 * 
 * Input data on the clipboard is a sequence of lines: 
 *  
 *      sigma    1.25   # smoothing sigma    
 *      year-year value
 *      year-year value
 *      year      value
 *      year      value
 *      year-year value
 *      year-year value
 *  
 */
public class UnbinCbrCdr
{
    public static void main(String[] args)
    {
        try
        {
            new UnbinCbrCdr().do_main();
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

        Command c = UnbinCbrCdrCommand.parse(text);
        if (c.binlist.size() != 2)
            throw new IllegalArgumentException("Requires two values");
    
        DecomposeCbrCdr dc = new DecomposeCbrCdr();
        if (c.sigma != null)
            dc.smoothingSigma = c.sigma;
        dc.maxInnerIterations = 500_000;
        dc.decompose(c.binlist.get(0), c.binlist.get(1));
        
        StringBuilder sb = new StringBuilder();
        for (int ix = 0; ix < dc.cbr.length; ix++)
        {
            int year = c.binlist.get(0)[0].age_x1 + ix;
            
            sb.append(String.format("%d %.3f %.3f\n", year, dc.cbr[ix], dc.cdr[ix]));
        }
        
        text = sb.toString();
        
        if (File.separatorChar == '\\')
            text = text.replace("\n", "\r\n");

        Clipboard.put(text);
    }
}
