package rtss.ww2losses;

import rtss.ww2losses.params.AreaParameters;
import rtss.data.selectors.Area;
import rtss.util.Util;

public class Main
{
    public static void main(String[] args)
    {
        try 
        {
            Main m = new Main();
            m.do_main();
        }
        catch (Exception ex)
        {
            Util.err("Exception: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            System.exit(1);
        }
        
        Util.out("");
        Util.out("*** Completed.");
    }
    
    private void do_main() throws Exception
    {
        do_main(Area.RSFSR, 4);

        Util.out("");
        Util.out("====================================================================");
        Util.out("");
        Util.out("РСФСР: defactor 1940 birth rates from 1940-1944 birth rates ...");
        Util.out("");
        EvaluatePopulationLossBase epl = new Defactor(AreaParameters.forArea(Area.RSFSR, 4));
        epl.evaluate();

        do_main(Area.USSR, 4);
        do_main(Area.USSR, 5);
    }
    
    private void do_main(Area area, int nyears) throws Exception
    {
        String syears = null;
        if (nyears == 4)
        {
            syears = "за 4 года (середина 1941 - середина 1945)";
        }
        else if (nyears == 5)
        {
            syears = "за 5 лет (начало 1941 - начало 1946)";
        }
        else
        {
            throw new IllegalArgumentException();
        }
        
        Util.out("");
        Util.out("*********************************************************************************************");
        Util.out("*****   Расчёт для " + area.toString() + " " + syears + ":");
        Util.out("*********************************************************************************************");
        Util.out("");
        Util.out("Compute minimum births window ...");
        Util.out("");
        new BirthTrough().calcTrough(area);
        
        AreaParameters params = AreaParameters.forArea(area, nyears);
        
        Util.out("");
        Util.out("====================================================================");
        Util.out("");
        
        Util.out("Compute at constant CDR and CBR ...");
        Util.out("");
        EvaluatePopulationLossBase epl = new EvaluatePopulationLossVariantA(params);
        epl.evaluate();

        Util.out("");
        Util.out("====================================================================");
        Util.out("");

        Util.out("Compute at constant excess deaths number ...");
        Util.out("");
        epl = new EvaluatePopulationLossVariantB(params);
        epl.evaluate();

        Util.out("");
        Util.out("====================================================================");
        Util.out("");

        Util.out("Recombining half-year rates ...");
        Util.out("");
        epl = new RecombineRates(params);
        epl.evaluate();
    }
}
