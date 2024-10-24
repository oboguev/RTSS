package rtss.pre1917.tools;

import rtss.util.Util;

public class ShowAllAreasValuesRaw extends ShowAllAreasValues 
{
    public static void main(String[] args)
    {
        try
        {
            // new ShowAllAreasValues().show_values_all();
            // new ShowAllAreasValues(LoadOptions.MERGE_POST1897_REGIONS).show_values_all();
            rawShowAllAreasValues().show_values_all();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }
    
    private ShowAllAreasValuesRaw() throws Exception
    {
    }
}
