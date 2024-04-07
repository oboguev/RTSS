package rtss.data.mortality.synthetic;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

public class TestYearlyToDailyMonotoneCurve
{
    public static void main(String[] args)
    {
        try
        {
            CombinedMortalityTable mt = new CombinedMortalityTable("mortality_tables/USSR/1938-1939");
            double[] yearly = mt.getSingleTable(Locality.TOTAL, Gender.BOTH).lx();
            yearly = Util.splice(yearly, 0, 10);
            double[] daily = YearlyToDailyMonotoneCurve.yearly2daily(yearly);
            Util.unused(daily);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
