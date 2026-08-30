package rtss.pre1917.tools;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.util.Util;

/*
 * Пометить сильные уклонения в числе рождений или смертей в губернии от соседних лет (ЦСК, 50 губерний)
 */
public class FlagOutliers_CSK50
{
    public static void main(String[] args)
    {
        try
        {
            TerritoryDataSet tds = new LoadData().loadEvroChast(LoadOptions.APPLY_PATCHES,
                                                                LoadOptions.DONT_MERGE_CITIES,
                                                                LoadOptions.DONT_MERGE_POST1897_REGIONS,
                                                                LoadOptions.DONT_ADJUST_FEMALE_BIRTHS,
                                                                LoadOptions.DONT_VERIFY);
            new FlagOutliers(tds, "births", 0.28).flagOutliers();
            new FlagOutliers(tds, "deaths", 0.28).flagOutliers();

            Util.out("** Done");
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }
}
