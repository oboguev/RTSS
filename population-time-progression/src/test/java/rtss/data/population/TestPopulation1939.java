package rtss.data.population;

import rtss.data.selectors.Area;
import rtss.forward_1926_193x.Adjust_1939;
import rtss.util.Util;

public class TestPopulation1939
{
    public static void main(String[] args)
    {
        try
        {
            new TestPopulation1939().eval();
            Util.out("*** Completed");
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
        }
    }
    
    private void eval() throws Exception
    {
        eval(Area.USSR);
        eval(Area.RSFSR);
    }
    
    private void eval(Area area) throws Exception
    {
        PopulationByLocality p1939_original = PopulationByLocality.census(area, 1939).smooth(true);
        PopulationByLocality p1939 = new Adjust_1939().adjust(area, p1939_original);
        Util.unused(p1939);
    }
}
