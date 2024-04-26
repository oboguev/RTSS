package rtss.tools;

import java.util.HashSet;
import java.util.Set;

import rtss.util.Clipboard;
import rtss.util.Util;

public class AverageUniqueValues
{
    public static void main(String[] args)
    {
        try 
        {
            new AverageUniqueValues().do_main();
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
        
        StringBuilder sb = new StringBuilder(); 
        
        for (String line : text.replace("\r", "").split("\n"))
        {
            String res = "";
            line = Util.despace(line).trim();
            if (line.length() != 0)
                res = average(line);
            sb.append(res);
            sb.append("\n");
        }
        
        Clipboard.put(sb.toString());
    }
    
    private String average(String line) throws Exception
    {
        Set<Double> unique = new HashSet<>();
        
        for (String sv : line.split(" "))
        {
            double v = Double.parseDouble(sv.replace(",", ""));
            
            boolean duplicate = false;
            for (Double xv : unique)
            {
                if (Math.abs(v - xv) < 1)
                    duplicate = true;
            }
            
            if (!duplicate)
                unique.add(v);
        }
        
        double sum = 0;
        for (Double xv : unique)
            sum += xv;
        sum /= unique.size();
        
        return Util.f2s(sum);
    }
}
