package rtss.data.mortality.synthetic;

import rtss.data.selectors.Area;
import rtss.util.Util;

public class TestMortalityRatesADH
{
    public static void main(String[] args)
    {
        try
        {
            new TestMortalityRatesADH().do_main();
            Util.out("*** Completed");
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
        }
    }

    private void do_main() throws Exception
    {
        MortalityTableADH.getMortalityTable(Area.RSFSR, 1946); // ###
        
        if (Util.False) // ###
            return;

        for (int year = 1927; year <= 1958; year++)
        {
            if (year >= 1941 && year <= 1945)
                continue;

            MortalityTableADH.getMortalityTable(Area.RSFSR, year);
        }
    }
}
