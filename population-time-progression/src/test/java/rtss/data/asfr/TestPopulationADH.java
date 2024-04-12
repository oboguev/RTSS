package rtss.data.asfr;

import rtss.data.population.synthetic.PopulationADH;
import rtss.data.selectors.Area;

public class TestPopulationADH
{
    public static void main(String[] args)
    {
        try
        {
            // ###
            PopulationADH.getPopulationByLocality(Area.RSFSR, 1940);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
