package rtss.mexico.agri.loader;

import java.util.Map;

import rtss.mexico.agri.entities.Culture;
import rtss.mexico.agri.entities.CultureYear;
import rtss.mexico.agri.entities.Cultures;
import rtss.util.Util;

public class ValidateSARH
{
    public static void main(String[] args)
    {
        try
        {
            new ValidateSARH().validate();
            Util.out("** Done");
        }
        catch (Exception ex)
        {
            Util.err("** Exception");
            ex.printStackTrace();
        }
    }
    
    private Cultures cultures  = LoadSARH.load();
    private Map<Integer, Long> population = LoadSARH.loadPopulation();
    
    private ValidateSARH() throws Exception
    {
    }

    private void validate() throws Exception
    {
        for (String cname : cultures.names())
        {
            Culture c = cultures.get(cname);

            for (int year : c.years())
                validate(c.cultureYear(year));
            
            for (CultureYear cy : c.averageCultureYears.values())
                validate(cy);

            for (CultureYear cy : c.averageCultureYears.values())
                validateAverage(cy);
        }
    }
    
    private void validate(CultureYear cy) throws Exception
    {
        // ### validate per capita
    }

    private void validateAverage(CultureYear cy) throws Exception
    {
        // ### 
    }
}
