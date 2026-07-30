package rtss.pre1917.data.migration;

import java.util.ArrayList;
import java.util.List;

import rtss.pre1917.LoadData;
import rtss.pre1917.data.Taxon;
import rtss.util.Util;

public class ShowMigrationSum
{
    public static void main(String[] args)
    {
        try
        {
            new ShowMigrationSum().do_main();
        }
        catch (Exception ex)
        {
            Util.err("** Exception:");
            ex.printStackTrace();
        }
    }
    
    private ShowMigrationSum() throws Exception
    {
    }

    private InnerMigration innerMigration = new LoadData().loadInnerMigration();
    
    private void do_main() throws Exception
    {
        List<String> e50 = gov_e50();
        
        for (int year = 1896; year <= 1903; year++)
        {
            Util.out(String.format("%d %,d %,d", year, outFlow(year, e50), inFlow(year, e50)));
        }
    }
    
    private List<String> gov_e50() throws Exception
    {
        List<String> tnames = new ArrayList<>();
        Taxon tx = Taxon.of("50 губерний Европейской России", 1913, null);
        for (String tname : Util.sort(tx.territories.keySet()))
        {
            if (Taxon.isComposite(tname))
                continue;
            
            switch (tname)
            {
            case "Московская с Москвой":
            case "Санкт-Петербургская с Санкт-Петербургом":
            case "Таврическая с Севастополем":
            case "Херсонская с Одессой":
            case "г. Москва":
            case "г. Николаев":
            case "г. Одесса":
            case "г. Санкт-Петербург":
            case "г. Севастополь":
                continue;
                
            default:
                break;
            }
            
            tnames.add(tname);
            // Util.out(tname);
        }
        
        return tnames;
    }
    
    private long inFlow(int year, List<String> tnames)
    {
        long v = 0;
        for (String tname : tnames)
            v += innerMigration.inFlow(tname, year);
        return v;
    }

    private long outFlow(int year, List<String> tnames)
    {
        long v = 0;
        for (String tname : tnames)
            v += innerMigration.outFlow(tname, year);
        return v;
    }
}
