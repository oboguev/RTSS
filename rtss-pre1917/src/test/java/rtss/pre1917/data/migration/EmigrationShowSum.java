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
            new EmigrationShowSum().do_main_2("Варшавская");
            new EmigrationShowSum().do_main_2("г. Варшава");
            new EmigrationShowSum().do_main_2("Варшавская с Варшавой");
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

    private void do_main_2(String tname) throws Exception
    {
        Util.out("");
        Util.out("Эмиграция Империи по годам из " + tname);
        Util.out("");
        
        Emigration emigration = new LoadData().loadEmigration();
        
        for (int year = 1881; year <= 1916; year++) 
        {
            long emigrants = emigration.emigrants(tname, year);
            Util.out(String.format("%d %,d", year, emigrants));
        }
    }
}
