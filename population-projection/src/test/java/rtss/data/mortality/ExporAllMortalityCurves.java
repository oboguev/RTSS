package rtss.data.mortality;

import rtss.data.mortality.synthetic.MortalityTableADH;
import rtss.data.mortality.synthetic.MortalityTableGKS;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.selectors.Area;
import rtss.util.Util;

public class ExporAllMortalityCurves
{
    public static void main(String[] args)
    {
        try
        {
            new ExporAllMortalityCurves().do_main();
            
            Util.out("** Completed");
        }
        catch (Exception ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }
    
    private void do_main() throws Exception
    {
        for (int year = 1927; year <= 1958; year++)
        {
            if (year >= 1941 && year <= 1945)
                continue;
            CombinedMortalityTable cmt = MortalityTableADH.getMortalityTable(Area.RSFSR, year);
        }
        
        if (Util.True)
        {
            PopulationByLocality p1989 = PopulationByLocality.census(Area.RSFSR, 1989);
            CombinedMortalityTable cmt = MortalityTableGKS.getMortalityTable(Area.RSFSR, "1986-1987", p1989);
        }

        CombinedMortalityTable.loadMF("mortality_tables/Russian-Empire/no63-50governorships-orthodox-1874-1883");
        CombinedMortalityTable.loadMF("mortality_tables/Russian-Empire/no62-50governorships-orthodox-1896-1897");
        CombinedMortalityTable.loadMF("mortality_tables/Russian-Empire/no64-50governorships-orthodox-1896-1897-variant-1");
        CombinedMortalityTable.loadMF("mortality_tables/Russian-Empire/no64-50governorships-orthodox-1896-1897-variant-1");
        CombinedMortalityTable.loadMF("mortality_tables/Russian-Empire/no67-50governorships-orthodox-1907-1910-variant-2");
        CombinedMortalityTable.loadMF("mortality_tables/Russian-Empire/novoselsky-1896-1897");
    }
}
