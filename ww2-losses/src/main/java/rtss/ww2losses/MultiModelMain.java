package rtss.ww2losses;

import java.util.ArrayList;
import java.util.List;

import rtss.data.selectors.Area;
import rtss.util.Util;
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
        for (double aw_conscripts_rkka_loss = 0.5; aw_conscripts_rkka_loss <= 0.9; aw_conscripts_rkka_loss += 0.1)
        {
            for (double aw_general_occupation = 0.2; aw_general_occupation <= 0.5; aw_general_occupation += 0.1)
            {
                ModelParameters params = new ModelParameters();
                params.aw_conscripts_rkka_loss = aw_conscripts_rkka_loss;
                params.aw_general_occupation = aw_general_occupation;
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
        main.main();
        return model.results;
    }

    /* =================================================================================== */

    private void print()
    {
        Util.out("");
        Util.out("Распечатка моделей:");
        Util.out("");

        for (AreaModel am : areaModels)
            print(am);
    }

    private void print(AreaModel am)
    {
        String ussr = result2str(am.ussr);
        String rsfsr = result2str(am.rsfsr);
        
        String s = String.format("%3.1f %3.1f * %s * %s",
                                 am.params.aw_conscripts_rkka_loss,
                                 am.params.aw_general_occupation,
                                 ussr, rsfsr);
        Util.out(s);
    }

    private String result2str(ModelResults r)
    {
        return String.format("%7s %7s %7s %7s %5.1f",
                             f2k(r.actual_excess_wartime_deaths),
                             f2k(r.exd_conscripts),
                             f2k(r.excess_warborn_deaths),
                             f2k(r.actual_births),
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
