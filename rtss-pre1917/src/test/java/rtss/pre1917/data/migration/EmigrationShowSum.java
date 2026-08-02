package rtss.pre1917.data.migration;

import rtss.pre1917.LoadData;
import rtss.util.Util;

public class EmigrationShowSum
{
    public static void main(String[] args)
    {
        try
        {
            new EmigrationShowSum().do_main();
        }
        catch (Exception ex)
        {
            Util.err("** Exception:");
            ex.printStackTrace();
        }
    }
    
    private void do_main() throws Exception
    {
        Util.out("Общая эмиграция из Империи по годам");
        Util.out("");
        
        Emigration emigration = new LoadData().loadEmigration();
        
        for (int year = 1881; year <= 1916; year++) 
        {
            long emigrants = emigration.emigrants(year);
            Util.out(String.format("%d %,d", year, emigrants));
        }
    }
}
