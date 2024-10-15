package rtss.pre1917.tools;

import rtss.pre1917.data.Taxon;
import rtss.util.Util;

public class ShowAllAreasValues extends ShowAreaValues
{
    public static void main(String[] args)
    {
        try
        {
            new ShowAllAreasValues().show_values_all();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private ShowAllAreasValues() throws Exception
    {
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
