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
        PostProcess pp = new PostProcess ();
        pp.initCensusSource(loadCensusSource());
        pp.initInterpolationData(loadInterpolationData());
    }
    
    private List<String[]> loadCensusSource() throws Exception
    {
        String s = Util.loadResource("census_1920_source.txt");
        s = s.replace("\t", ",");
        try (CSVReader reader = new CSVReader(new StringReader(s)))
        {
            return reader.readAll();
        }
    }
    
    private List<String[]> loadInterpolationData() throws Exception
    {
        String s = Util.loadResource("interpolation.txt");
        s = s.replace("\t", " ").replaceAll(" +", " ").replace(" ", ",");
        try (CSVReader reader = new CSVReader(new StringReader(s)))
        {
            return reader.readAll();
        }
    }
}
