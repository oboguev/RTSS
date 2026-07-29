package rtss.pre1917;

import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.util.Util;

public class LoadDataTest
{
    public static void main(String[] args)
    {
        try
        {
            test_1();
        }
        catch (Exception ex)
        {
            Util.err("** Exception");
            ex.printStackTrace();
        }
    }

    private static void test_1() throws Exception
    {
        LoadOptions[] options = { LoadOptions.DONT_VERIFY, LoadOptions.MERGE_POST1897_REGIONS, LoadOptions.EVAL_SPLIT_ASTRAKHAN,
                                  LoadOptions.EVAL_PROGRESSIVE, LoadOptions.FILL_MISSING_BD, LoadOptions.ADJUST_FEMALE_BIRTHS,
                                  LoadOptions.MERGE_CITIES };
        
        TerritoryDataSet census = new LoadData().loadCensus1897(options);        
        
        Territory t = census.get("Московская с Москвой");
        Util.unused(t);
    }
}
