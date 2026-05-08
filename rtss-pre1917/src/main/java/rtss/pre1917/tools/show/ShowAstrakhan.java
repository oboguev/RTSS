package rtss.pre1917.tools.show;

import rtss.pre1917.calc.EvalCountryTaxon;
import rtss.pre1917.data.DemographicConstants;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.eval.EvalProgressive;
import rtss.pre1917.util.PrintProgressive;
import rtss.pre1917.util.data.YearDataSet;
import rtss.pre1917.util.data.YearDataSummary;
import rtss.util.Util;

public class ShowAstrakhan
{
    private static TerritoryDataSet tdsEmpire;

    public static void main(String[] args)
    {
        try
        {
            tdsEmpire = EvalCountryTaxon.getFinalEmpirePopulationSet(new EvalCountryTaxon.Options()
                    .splitAstrakhan(false)
                    .verbose(false));
            new ShowAstrakhan().do_show_1();
            new ShowAstrakhan().do_show_2();
            new ShowAstrakhan().do_show_3();
        }
        catch (Exception ex)
        {
            Util.err("** Exception:");
            ex.printStackTrace();
        }
    }

    private void do_show_1() throws Exception
    {
        Util.out("Рождаемость, смертность и естественный прирост губерний соседних с Астраханской, средняя за период:");
        Util.out("");
        print("Область войска Донского", 1896, 1913);
        print("Самарская", 1896, 1913);
        print("Саратовская", 1896, 1913);

        Util.out("");
        print("Область войска Донского", 1896, 1914);
        print("Самарская", 1896, 1914);
        print("Саратовская", 1896, 1914);

    }

    private void print(String tname, int y1, int y2) throws Exception
    {
        print(tdsEmpire.get(tname), y1, y2);
    }

    private void print(Territory t, int y1, int y2) throws Exception
    {
        YearDataSet yds = new YearDataSet(t, y1, y2);
        YearDataSummary sm = yds.getSummary(y1, y2);
        Util.out(String.format("%-30s %d-%d %.1f %.1f %.1f", t.name, y1, y2, sm.cbr, sm.cdr, sm.ngr));
    }
    
    private void do_show_2() throws Exception
    {
        do_show_2(DemographicConstants.перепись1897_Астраханская_губ_оседлое_население);
        do_show_2(700_000);
        do_show_2(800_000);
        do_show_2(900_000);
    }

    private void do_show_2(long basePopulation) throws Exception
    {
        Territory t = tdsEmpire.get("Астраханская");
        EvalProgressive.evalProgressive(t, basePopulation);
        Util.out("");
        Util.out(String.format("Астраханской губернии при учётном населении в 1897 году %,d", basePopulation));
        print(t, 1896, 1913);
        print(t, 1896, 1914);
    }
    
    private void do_show_3() throws Exception
    {
        Util.out("");
        Territory t = tdsEmpire.get("Астраханская");

        Util.out("");
        Util.out("Астраханская до выделения оседлого населения");
        Util.out("");
        PrintProgressive.print(t);
        print("Астраханская", 1896, 1913);
        print("Астраханская", 1896, 1914);

        EvalProgressive.evalProgressive(t, 660_173);
        // EvalProgressive.evalProgressive(t, 800_000);
        Util.out("");
        Util.out("Астраханская после выделения оседлого населения");
        Util.out("");
        PrintProgressive.print(t);
        print("Астраханская", 1896, 1913);
        print("Астраханская", 1896, 1914);
    }
}
