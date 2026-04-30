package rtss.pre1917.merge;

import java.util.Collection;
import java.util.List;

import rtss.pre1917.data.TerritoryDataSet;
import rtss.util.Util;

public class VerifyNoTerritoryDuplication
{
    /*
     * Verify that @tds does not contain any territories twice:
     * in merged and unmerged form 
     */
    public static void verify(TerritoryDataSet tds) throws Exception
    {
        boolean error = false;
        error = verify(tds, MergePost1897Regions.MergePost1897Descriptors) || error;
        error = verify(tds, MergeCities.MergeCitiesDescriptors) || error;
        
        if (error)
            throw new Exception("Двойная копия территорий");
    }

    private static boolean verify(TerritoryDataSet tds, List<MergeDescriptor> mds)
    {
        boolean error = false;
        
        for (MergeDescriptor md : mds)
        {
            if (tds.containsKey(md.combined))
            {
                error = verifyNotBoth(tds, md.combined, md.parent) || error;
                error = verifyNotBoth(tds, md.combined, md.children) || error;
            }
        }

        return error;
    }

    private static boolean verifyNotBoth(TerritoryDataSet tds, String contained, Collection<String> check)
    {
        boolean error = false;
        
        for (String c : check)
            error = verifyNotBoth(tds, contained, c) || error;
        
        return error;
    }

    private static boolean verifyNotBoth(TerritoryDataSet tds, String contained, String check) 
    {
        if (check != null && tds.containsKey(check))
        {
            Util.err(String.format("Набор территорий одновременно содержит [%s] и [%s]", contained, check));
            return true;
        }
        else
        {
            return false;
        }
    }
}
