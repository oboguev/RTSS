package rtss.ww2losses;

import java.util.ArrayList;
import java.util.List;

import rtss.data.selectors.Area;
import rtss.util.Util;
import rtss.ww2losses.model.Automation;
import rtss.ww2losses.model.Model;
import rtss.ww2losses.model.ModelParameters;
import rtss.ww2losses.model.ModelResults;

public class MultiModelMain
{
    public static class AreaModel
    {
        public AreaModel(ModelParameters params)
        {
            this.params = params;
        }

        ModelParameters params;
        ModelResults ussr;
        ModelResults rsfsr;
    }

    public static void main(String[] args)
    {
        try
        {
            new MultiModelMain().main();
            Util.out("");
            Util.out("=== Конец расчёта моделей ===");
        }
        catch (Exception ex)
        {
            Util.err("*** Exception: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    }

    private List<AreaModel> areaModels = new ArrayList<>();

    private void main() throws Exception
    {
        // warmupCaches();
        
        for (double aw_conscript_combat = 0.6; aw_conscript_combat <= 0.8; aw_conscript_combat += 0.1)
        {
            for (double aw_civil_combat = 0.1; aw_civil_combat <= 0.4; aw_civil_combat += 0.1)
            {
                ModelParameters params = new ModelParameters();
                params.wamp.aw_conscript_combat = aw_conscript_combat;
                params.wamp.aw_civil_combat = aw_civil_combat;
                params.exportDirectory = null;
                AreaModel am = run(params);
                areaModels.add(am);
            }
        }

        print();
    }

    private AreaModel run(ModelParameters params) throws Exception
    {
        AreaModel am = new AreaModel(params);
        am.ussr = run(params, Area.USSR);
        am.rsfsr = run(params, Area.RSFSR);
        return am;
    }

    private ModelResults run(ModelParameters params, Area area) throws Exception
    {
        params = new ModelParameters(params);
        params.area = area;
        params.PrintDiagnostics = false;

        Model model = new Model();
        model.params = params;

        Main main = new Main(model);
        main.exportDirectory = null;
        
        Util.out("");
        Util.out("Исполнение для параметров: " + model.params.toString());
        Automation.setAutomated(true);
        
        main.main();
        
        return model.results;
    }
    
    @SuppressWarnings("unused")
    private void warmupCaches() throws Exception
    {
        ModelParameters params = new ModelParameters();
        params.wamp.aw_conscript_combat = 0.7;
        params.wamp.aw_civil_combat = 0.2;
        params.exportDirectory = null;
        AreaModel am = run(params);
    }

    /* =================================================================================== */

    private void print()
    {
        Util.out("");
        Util.out("Расчёт моделей (сумма с середины 1941 по конец 1945):");
        Util.out("");
        Util.out("   с42 = смертность в 1942 году, промилле");
        Util.out("");

        printHeaders();

        for (AreaModel am : areaModels)
            print(am);
    }

    private void printHeaders()
    {
        String a1 = "СССР                                         ";
        String a2 = "РСФСР                                        ";

        String h1 = " с.изб   с.прз   с.инов  р.факт иммигр   с42 ";
        String h2 = "======= ======= ======= ======= =======  ====";

        Util.out(String.format("        * %s * %s", a1, a2));
        Util.out(String.format("прз окк * %s * %s", h1, h1));
        Util.out(String.format("=== === * %s * %s", h2, h2));
    }

    private void print(AreaModel am)
    {
        String ussr = result2str(am.ussr);
        String rsfsr = result2str(am.rsfsr);

        String s = String.format("%3.1f %3.1f * %s * %s",
                                 am.params.wamp.aw_conscript_combat,
                                 am.params.wamp.aw_civil_combat,
                                 ussr, rsfsr);
        Util.out(s);
    }

    private String result2str(ModelResults r)
    {
        return String.format("%7s %7s %7s %7s %7s %5.1f",
                             f2k(r.actual_excess_wartime_deaths),
                             f2k(r.exd_conscripts),
                             f2k(r.excess_warborn_deaths),
                             f2k(r.actual_births),
                             f2k(r.immigration),
                             r.cdr_1942);
    }

    private static String f2s(double v)
    {
        String s = String.format("%,15.0f", v);
        while (s.startsWith(" "))
            s = s.substring(1);
        return s;
    }

    private static String f2k(double v)
    {
        return f2s(v / 1000.0);
    }
}
