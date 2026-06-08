package rtss.losses199x.util;

import rtss.data.population.struct.PopulationByLocality;
import rtss.losses199x.LoadData;
import rtss.util.Util;

public class PrintPopulation
{
    public static void main(String[] atgs)
    {
        try
        {
            new PrintPopulation().do_main();
        }
        catch (Exception ex)
        {
            Util.err("** Exception");
            ex.printStackTrace();
        }
    }
    
    private void do_main() throws Exception 
    {
        Util.out("Численность России в границах 1991-2014 годов на начало календарного года");
        Util.out("");
        
        for (int year = 1990; year <= 2022; year++)
        {
            PopulationByLocality p = LoadData.actualPopulation(year);
            Util.out(String.format("%d %,d", year, Math.round(p.sum())));
        }
    }
}
