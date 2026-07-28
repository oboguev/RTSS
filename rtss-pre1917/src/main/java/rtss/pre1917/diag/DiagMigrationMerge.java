package rtss.pre1917.diag;

import rtss.pre1917.data.migration.ImmigrationYear.LumpImmigration;
import rtss.util.Util;

public class DiagMigrationMerge
{
    private static boolean active = Util.True;
    
    public static final String Population = "Population";
    public static final String VitalRates = "VitalRates";
    private static String which;

    public static void lump(String taxonName, int year, long lumpYearSum, LumpImmigration lump)
    {
        if (!active || !interestedYear(taxonName, year))
            return;

        Util.err(String.format("LUMP-MIGR: %s %d = %d", taxonName, year, lumpYearSum));
    }

    public static void which(String which_arg)
    {
        which = which_arg;
    }

    public static void merged(String selector, String tnameTarget, int year, String tnameSource, double amount)
    {
        if (!active || !interestedYear(tnameTarget, year))
            return;

        if (!selector.equals("migration.total.both") || which == null || !Population.equals(which))
            return;

        long rounded = Math.round(amount);

        if (amount == rounded)
            Util.err(String.format("MERGE-MIGR: %s %d <== %s %d", tnameTarget, year, tnameSource, rounded));
        else
            Util.err(String.format("MERGE-MIGR: %s %d <== %s %.1f", tnameTarget, year, tnameSource, amount));
    }

    private static boolean interestedYear(String tnameTarget, int year)
    {
        switch (tnameTarget)
        {
        case "Империя":
            break;
        
        default:
            return false;
        }
        
        return year == 1896 || year == 1905 || year == 1906;
    }
}
