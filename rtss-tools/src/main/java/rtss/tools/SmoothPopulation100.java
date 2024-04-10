package rtss.tools;

import java.util.ArrayList;
import java.util.List;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.util.Clipboard;
import rtss.util.Util;

/**
 * Сгладить возрастное распределение населения.
 *  
 * Берёт с clipboard файл распределения в формате строчек
 *    "число" 
 * со строчками для каждого возраста
 * и оставляет на clipboard преобразованный файл того же формата,
 * но со сглаженным распределением.
 */
public class SmoothPopulation100
{
    public static void main(String[] args)
    {
        try 
        {
            SmoothPopulation100 m = new SmoothPopulation100();
            m.do_main();
        }
        catch (Exception ex)
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
        catch (Exception ex)
        {
            Util.noop();
        }
        
        if (text == null)
        {
            Util.err("There is no text on the clipboard");
            return;
        }
        
        List<Bin> list = new ArrayList<Bin>();
        int age = 0;
        for (String s : text.replace("\r", "").split("\n"))
        {
            if (s == null)
                continue;
            s = s.trim();
            if (s.length() == 0)
                continue;
            list.add(new Bin(age, age, Double.parseDouble(s.replace(",", ""))));
            age++;
        }
        
        Bin[] bins = Bins.bins(list);
        
        if (bins.length == 0)
            fatal("No data on the clipboard");
        if (bins[0].age_x1 != 0)
            fatal("Data does not start at age 0");
        for (Bin bin: bins)
        {
            if (bin.widths_in_years != 1) 
                fatal("Data is not per-year");
        }
        
        double[] counts = Bins.midpoint_y(bins);
        counts = rtss.data.population.SmoothPopulation.smooth(counts);
        
        StringBuilder sb = new StringBuilder(); 
        for (double count : counts)
        {
            if (sb.length() != 0)
                sb.append("\n");
            sb.append(f2k(count));
        }
        
        Clipboard.put(sb.toString());
    }
    
    private void fatal(String msg)
    {
        Util.err(msg);
        System.exit(1);
    }    

    private String f2k(double v)
    {
        String s = String.format("%,15.0f", v);
        while (s.startsWith(" "))
            s = s.substring(1);
        return s;
    }
}
