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
        do_main(Area.RSFSR);

        Util.out("");
        Util.out("====================================================================");
        Util.out("");
        Util.out("РСФСР: defactor 1940 birth rates from 1940-1944 birth rates ...");
        Util.out("");
        EvaluatePopulationLossBase epl = new Defactor(AreaParameters.forArea(Area.RSFSR, 4));
        epl.evaluate();

        Util.out("");
        do_main(Area.USSR);
    }
    
    private void do_main(Area area) throws Exception
    {
        Util.out("*********************************************************************************************");
        Util.out("*****   Расчёт для " + area.toString() + ":");
        Util.out("*********************************************************************************************");
        Util.out("");
        Util.out("Compute minimum births window ...");
        Util.out("");
        new BirthTrough().calcTrough(area);
        
        AreaParameters params = AreaParameters.forArea(area, 4);
        
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
