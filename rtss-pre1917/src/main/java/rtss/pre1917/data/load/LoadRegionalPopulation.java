package rtss.pre1917.data.load;

import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.data.WhichPopulation;
import rtss.util.excel.Excel;
import rtss.util.excel.ExcelRC;

public class LoadRegionalPopulation
{
    public static void load(Territory t, WhichPopulation which) throws Exception
    {
        load(t, t.name, which);
    }

    public static void load(Territory t, String tname, WhichPopulation which) throws Exception
    {
        ExcelRC rc = Excel.readSheet("regional-population-estimates.xlsx", true, tname);
        int ncYear = rc.columnTitleToIndex("год");
        int ncPopulation = rc.columnTitleToIndex("население");

        for (int nr = 1; nr < rc.size() && !rc.isEndRow(nr); nr++)
        {
            if (rc.isEmpty(nr, ncYear))
                continue;
                    
            int year = rc.asInt(nr, ncYear);
            long pop = rc.asLong(nr, ncPopulation);
            
            TerritoryYear ty = t.territoryYear(year);
            switch (which)
            {
            case POPULATION:
                ty.population.total.both = pop;
                break;

            case PROGRESSIVE_POPULATION:
                ty.progressive_population.total.both = pop;
                break;
            }
        }
    }
}
