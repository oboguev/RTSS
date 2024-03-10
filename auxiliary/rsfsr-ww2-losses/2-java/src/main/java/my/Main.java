package my;

import java.io.StringReader;
import java.util.List;
import com.opencsv.CSVReader;

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
        Util.out("Compute minimum births window ...");
        Util.out("");
        PostProcess pp = new PostProcess ();
        pp.initCensusSource(loadCensusSource());
        pp.initInterpolationData(loadInterpolationData());
        pp.postProcess();
        Util.out("");
        Util.out("====================================================================");
        Util.out("");
        
        Util.out("Compute constant CDR and CBR ...");
        Util.out("");
        EvaluatePopulationLossBase epl = new EvaluatePopulationLossVariantA();
        epl.evaluate();
        Util.out("");
        Util.out("====================================================================");
        Util.out("");

        Util.out("Compute at constant excess deaths number ...");
        Util.out("");
        epl = new EvaluatePopulationLossVariantB();
        epl.evaluate();
    }
    
    private List<String[]> loadCensusSource() throws Exception
    {
        String s = Util.loadResource("census_1959_data.txt");
        s = s.replace("\t", ",");
        try (CSVReader reader = new CSVReader(new StringReader(s)))
        {
            return reader.readAll();
        }
    }
    
    private List<String[]> loadInterpolationData() throws Exception
    {
        String s = Util.loadResource("census_1959_interpolation.txt");
        s = s.replace("\t", " ").replaceAll(" +", " ").replace(" ", ",");
        try (CSVReader reader = new CSVReader(new StringReader(s)))
        {
            return reader.readAll();
        }
    }
}
