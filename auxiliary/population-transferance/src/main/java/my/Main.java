package my;

import data.mortality.CombinedMortalityTable;
import data.population.Population;

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
        new Transfer_1926_1936().transfer();
    }
    
    @SuppressWarnings("unused")
    private void load_all_data() throws Exception
    {
        new CombinedMortalityTable("mortality_tables/USSR/1926-1927");
        new CombinedMortalityTable("mortality_tables/USSR/1938-1939");
        
        Population p = new Population();
        p.loadCombined("population_data/USSR/1926");
        p.loadCombined("population_data/RSFSR/1926");
        p.loadCombined("population_data/USSR/1937");
    }
}
