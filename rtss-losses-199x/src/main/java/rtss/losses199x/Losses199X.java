package rtss.losses199x;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.synthetic.MortalityTableGKS;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.selectors.Area;
import rtss.util.Util;

public class Losses199X
{
    public static void main(String[] args)
    {
        try
        {
            PopulationByLocality p1989 = PopulationByLocality.census(Area.RSFSR, 1989);
            CombinedMortalityTable cmt = MortalityTableGKS.getMortalityTable(Area.RSFSR, "1986-1987");
            Util.noop();
        }
        catch (Exception ex)
        {
            Util.err("** Exception: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    }
}
