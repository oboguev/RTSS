package rtss.pre1917.validate;

import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.data.migration.TotalMigration;
import rtss.util.Util;

public class ValidateProgressive
{
    public static void validate(Territory t) throws Exception
    {
        switch (t.name)
        {
        case "Волынская":
            break;
            
        default:
            break;
        }
        
        for (int year : t.years())
        {
            TerritoryYear ty = t.territoryYearOrNull(year);
            TerritoryYear ty2 = t.territoryYearOrNull(year + 1);

            if (ty == null || ty.births.total.both == null || ty.deaths.total.both == null || ty.progressive_population.total.both == null)
                continue;
            if (ty2 == null || ty2.progressive_population.total.both == null)
                continue;
            
            TotalMigration totalMigration = TotalMigration.getTotalMigration();
            
            long migr = totalMigration.saldo(t.name, ty.year);
            
            if (ty.migration.total.both != null && ty.migration.total.both != migr)
                throw new Exception("Incoherent migration");
            
            long pop = ty.progressive_population.total.both + ty.births.total.both - ty.deaths.total.both + migr; 
            
            if (ty2.progressive_population.total.both != pop)
                throw new Exception("Incoherent progressive population");
        }
    }

    public static void validate(TerritoryDataSet ts) throws Exception
    {
        for (String tname : Util.sort(ts.keySet()))
            validate(ts.get(tname));
    }
}
