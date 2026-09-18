package rtss.pre1917.validate;

import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.data.migration.TotalMigration;

public class ValidateProgressive
{
    public static void validate(Territory t) throws Exception
    {
        for (int year : t.years())
        {
            TerritoryYear ty = t.territoryYearOrNull(year);
            TerritoryYear ty2 = t.territoryYearOrNull(year + 1);

            if (ty == null || ty.births.total.both == null || ty.deaths.total.both == null || ty.progressive_population.total.both == null)
                continue;
            if (ty2 == null || ty2.progressive_population.total.both == null)
                continue;
            
            TotalMigration totalMigration = TotalMigration.getTotalMigration();
        }
    }
}
