package rtss.pre1917.tools;

import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Taxon;
import rtss.util.Util;

public class ShowAllAreasValues extends ShowAreaValues
{
    public static void main(String[] args)
    {
        try
        {
            // new ShowAllAreasValues().show_values_all();
            new ShowAllAreasValues(LoadOptions.MERGE_POST1897_REGIONS).show_values_all();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private ShowAllAreasValues(LoadOptions... options) throws Exception
    {
        super(options);
    }
    
    private ShowAllAreasValues() throws Exception
    {
        super(new LoadOptions[0]);
    }

    private void show_values_all() throws Exception
    {
        for (String tname : Util.sort(tdsUGVI.keySet()))
        {
            if (!Taxon.isComposite(tname))
                show_values(tname);
        }
    }
}
