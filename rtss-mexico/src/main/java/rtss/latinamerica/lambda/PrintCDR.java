package rtss.latinamerica.lambda;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.struct.Population;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
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
        Util.out("Смертность по LAMBdA");
        Util.out("");
        
        for (String rname : CountryName.rnames())
        {
            for (int year : LambdaPopulation.countryPopulationYears(rname))
            {
                Population p = LambdaPopulation.countryPopulation(rname, year);
                CombinedMortalityTable cmt = LambdaMortalityTable.countryMortalityTable(rname, year);
                if (p == null || cmt == null)
                    continue;
                
                double deaths = deaths(p, cmt);
                double total = p.sum();
                
                Util.out(String.format("%s %d %.1f", rname, year, deaths/total * 1000.0));
            }
        }
    }
    
    private double deaths(Population p, CombinedMortalityTable cmt) throws Exception
    {
        return deaths(p, cmt, Gender.MALE) + deaths(p, cmt, Gender.FEMALE);
    }

    private double deaths(Population p, CombinedMortalityTable cmt, Gender gender) throws Exception
    {
        double sum = 0;
        
        for (int age = 0; age <= Population.MAX_AGE; age++)
        {
            sum += p.get(gender, age) * cmt.getSingleTable(Locality.TOTAL, gender).qx()[age];
        }
        
        
        
        return sum;
    }
}
