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
            test_1();
            test_2();
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
        }
    }

    private static void test_1() throws Exception
    {
        CombinedMortalityTable mt = new CombinedMortalityTable("mortality_tables/USSR/1938-1939");
        double[] yearly = mt.getSingleTable(Locality.TOTAL, Gender.BOTH).lx();
        yearly = Util.splice(yearly, 0, 10);
        double[] daily = InterpolateYearlyToDailyAsValuePreservingMonotoneCurve.yearly2daily(yearly);
        Util.unused(daily);
    }

    private static void test_2() throws Exception
    {
        CombinedMortalityTable mt = new CombinedMortalityTable("mortality_tables/USSR/1926-1927");
        double[] yearly = mt.getSingleTable(Locality.RURAL, Gender.MALE).lx();
        yearly = Util.splice(yearly, 0, 9);
        double[] daily = InterpolateYearlyToDailyAsValuePreservingMonotoneCurve.yearly2daily(yearly);
        Util.unused(daily);
    }
}
