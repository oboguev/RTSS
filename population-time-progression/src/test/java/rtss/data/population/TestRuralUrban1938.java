package rtss.data.population;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.MortalityInfo;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

public class TestRuralUrban1938
{
    private final boolean do_smooth = false;
    private PopulationByLocality p = PopulationByLocality.census(Area.USSR, 1939).smooth(do_smooth);
    protected CombinedMortalityTable mt = new CombinedMortalityTable("mortality_tables/USSR/1938-1939");

    public static void main(String[] args)
    {
        try
        {
            new TestRuralUrban1938().eval();
            Util.out("*** Completed");
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private TestRuralUrban1938() throws Exception
    {
    }

    private void eval() throws Exception
    {
        eval(Gender.MALE);
        eval(Gender.FEMALE);
        eval(Gender.BOTH);
    }
    
    private void eval(Gender gender) throws Exception
    {
        for (int age = 0; age < Population.MAX_AGE; age++)
        {
            MortalityInfo mr = mt.getSingleTable(Locality.RURAL, gender).get(age);
            MortalityInfo mu = mt.getSingleTable(Locality.URBAN, gender).get(age);
            MortalityInfo m = mt.getSingleTable(Locality.TOTAL, gender).get(age);
            double pct_m = pct(m.qx, mu.qx, mr.qx);
            
            double pr = p.get(Locality.RURAL, gender, age);
            double pu = p.get(Locality.URBAN, gender, age);
            double pct_p = 100 * pu / (pr + pu);
            
            Util.unused(pct_m);
            Util.unused(pct_p);
        }
    }
    
    private double pct(double v, double urban, double rural) throws Exception
    {
        double pct = (v - rural) / (urban - rural);
        if (v < 0 || v > 1)
            throw new Exception("Not within urban-rural range");
        return pct * 100;
    }
}
