package rtss.pre1917.tools.show;

import rtss.pre1917.calc.EvalCountryTaxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.eval.EvalProgressive;
import rtss.pre1917.util.PrintProgressive;
import rtss.pre1917.util.data.YearDataSet;
import rtss.pre1917.util.data.YearDataSummary;
import rtss.util.Util;

public class ShowSelect
{
    private static TerritoryDataSet tdsEmpire;

    public static void main(String[] args)
    {
        try
        {
            tdsEmpire = EvalCountryTaxon.getFinalEmpirePopulationSet();
            new ShowSelect().do_show_1();
            new ShowSelect().do_show_2();
        }
        catch (Exception ex)
        {
            Util.err("** Exception:");
            ex.printStackTrace();
        }
    }

    private void do_show_1() throws Exception
    {
        print("Область войска Донского", 1896, 1913);
        print("Самарская", 1896, 1913);
        print("Саратовская", 1896, 1913);

        print("Область войска Донского", 1896, 1914);
        print("Самарская", 1896, 1914);
        print("Саратовская", 1896, 1914);

    }

    private void print(String tname, int y1, int y2) throws Exception
    {
        YearDataSet yds = new YearDataSet(tdsEmpire.get(tname), y1, y2);
        YearDataSummary sm = yds.getSummary(y1, y2);
        Util.out(String.format("%-30s %d-%d %.1f %.1f %.1f", tname, y1, y2, sm.cbr, sm.cdr, sm.ngr));
    }

    private void do_show_2() throws Exception
    {
        Territory t = tdsEmpire.get("Астраханская");

        Util.out("");
        Util.out("Астраханская до");
        Util.out("");
        PrintProgressive.print(t);
        print("Астраханская", 1896, 1913);
        print("Астраханская", 1896, 1914);

        EvalProgressive.evalProgressive(t, 660_173);
        // EvalProgressive.evalProgressive(t, 800_000);
        Util.out("");
        Util.out("Астраханская после");
        Util.out("");
        PrintProgressive.print(t);
        print("Астраханская", 1896, 1913);
        print("Астраханская", 1896, 1914);
    }
}
