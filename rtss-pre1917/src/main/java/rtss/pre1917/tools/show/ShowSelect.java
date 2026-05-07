package rtss.pre1917.tools.show;

import rtss.pre1917.calc.EvalCountryTaxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.eval.EvalProgressive;
import rtss.pre1917.util.PrintProgressive;
import rtss.util.Util;

public class ShowSelect
{
    public static void main(String[] args)
    {
        try
        {
            new ShowSelect().do_show_1();
        }
        catch (Exception ex)
        {
            Util.err("** Exception:");
            ex.printStackTrace();
        }
    }
    
    private void do_show_1() throws Exception
    {
        TerritoryDataSet tds = EvalCountryTaxon.getFinalEmpirePopulationSet(); 
        Territory t = tds.get("Астраханская");
        
        Util.out("");
        Util.out("Астраханская до");
        Util.out("");
        PrintProgressive.print(t);
        
        // EvalProgressive.evalProgressive(t, 660_173);
        EvalProgressive.evalProgressive(t, 800_000);
        Util.out("");
        Util.out("Астраханская после");
        Util.out("");
        PrintProgressive.print(t);
    }
}
