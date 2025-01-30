package rtss.tools;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.util.Clipboard;
import rtss.util.Util;

/**
 * Сгладить возрастное распределение населения.
 *  
 * Берёт с clipboard файл распределения в формате строчек
 *    "возраст число"
 * и оставляет на clipboard преобразованный файл того же формата,
 * но со сглаженным распределением.
 */
public class SmoothPopulation
{
    public static void main(String[] args)
    {
        try 
        {
            SmoothPopulation m = new SmoothPopulation();
            m.do_main();
        }
        catch (Throwable ex)
        {
            Util.err("Exception: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            System.exit(1);
        }
        
        Util.out("");
        Util.out("*** Smoothed population distribution was placed on the clipboard.");
    }
    
    private void do_main() throws Exception
    {
        String text = null;
        try
        {
            text = Clipboard.getText();
        }
        catch (Throwable ex)
        {
            Util.noop();
        }
        
        if (text == null)
        {
            Util.err("There is no text on the clipboard");
            return;
        }
        
        Bin[] bins = Bins.parseBinsYearly(text, " ");
        if (bins.length == 0)
            fatal("No data on the clipboard");
        if (bins[0].age_x1 != 0)
            fatal("Data does not start at age 0");
        for (Bin bin: bins)
        {
            if (bin.widths_in_years != 1) 
                fatal("Data is not per-year");
        }
        
        int[] ages = Bins.start_x(bins);
        double[] counts = Bins.midpoint_y(bins);
        counts = rtss.data.population.calc.SmoothPopulation.smooth(counts);
        Clipboard.put(" ", ages, counts);
    }
    
    private void fatal(String msg)
    {
        Util.err(msg);
        System.exit(1);
    }    
}
