package rtss.latinamerica.lambda;

import rtss.data.population.struct.Population;
import rtss.util.Util;

public class PrintCDR
{
    public static void main(String[] args)
    {
        try
        {
            new PrintCDR().do_main();
        }
        catch (Exception ex)
        {
            Util.err("** Exception");
            ex.printStackTrace();
        }
    }

    private void do_main() throws Exception
    {
        for (String rname : CountryName.rnames())
        {
            for (int year : LambdaPopulation.countryPopulationYears(rname))
            {
                Population p = LambdaPopulation.countryPopulation(rname, year);
            }
        }
        // ###
    }
}
