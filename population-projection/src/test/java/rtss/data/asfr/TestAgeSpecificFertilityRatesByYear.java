package rtss.data.asfr;

import rtss.data.selectors.Area;
import rtss.util.Util;

public class TestAgeSpecificFertilityRatesByYear
{
    public static void main(String[] args)
    {
        try
        {
            AgeSpecificFertilityRatesByYear asfry = AgeSpecificFertilityRatesByYear.load(Area.USSR);
            Util.unused(asfry);
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
        }
    }
}
