package rtss.ww2losses;

import java.io.StringReader;
import java.util.List;
import com.opencsv.CSVReader;

import rtss.ww2losses.params.AreaParameters;
import rtss.ww2losses.selectors.Area;
import rtss.ww2losses.util.Util;

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
        EvaluatePopulationLossBase epl = new Defactor(AreaParameters.forArea(Area.RSFSR));
        epl.evaluate();
    }
    
    private void do_main(Area area) throws Exception
    {
        Util.out("*********************************************************************************************");
        Util.out("*****   Расчёт для " + area.toString() + ":");
        Util.out("*********************************************************************************************");
        Util.out("");
        Util.out("Compute minimum births window ...");
        Util.out("");
        PostProcess pp = new PostProcess();
        pp.initCensusSource(loadCensusSource(area), 9, 20);
        pp.initInterpolationData(loadInterpolationData(area));
        pp.postProcess();
        
        AreaParameters params = AreaParameters.forArea(area);
        
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
    
    private List<String[]> loadCensusSource(Area area) throws Exception
    {
        String prefix = area.name() + "_";
        String s = Util.loadResource(prefix + "census_1959_data.txt");
        s = removeComments(s);
        s = s.replace("\t", " ").replaceAll(" +", " ").replace(" ", ",");
        try (CSVReader reader = new CSVReader(new StringReader(s)))
        {
            return reader.readAll();
        }
    }
    
    private List<String[]> loadInterpolationData(Area area) throws Exception
    {
        String prefix = area.name() + "_";
        String s = Util.loadResource(prefix + "census_1959_interpolation.txt");
        s = removeComments(s);
        s = s.replace("\t", " ").replaceAll(" +", " ").replace(" ", ",");
        try (CSVReader reader = new CSVReader(new StringReader(s)))
        {
            return reader.readAll();
        }
    }
    
    private String removeComments(String rdata)
    {
        StringBuilder sb = new StringBuilder();
        
        rdata = rdata.replace("\r\n", "\n");
        for (String line : rdata.split("\n")) 
        {
            String lt = line.trim();
            if (lt.startsWith("#") || lt.length() == 0)
                continue;
            sb.append(line);
            sb.append("\n");
        }
        
        return sb.toString();
    }
}
