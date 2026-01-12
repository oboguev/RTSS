package rtss.data.asfr;

import rtss.data.selectors.Area;
import rtss.util.Util;

public class ShowASFR
{
    public static void main(String[] args)
    {
        try
        {
            AgeSpecificFertilityRatesByYear yearly_asfrs = null;

            yearly_asfrs = AgeSpecificFertilityRatesByYear.load(Area.RSFSR, "Total");
            // yearly_asfrs = AgeSpecificFertilityRatesByYear.load(Area.USSR);
            // yearly_asfrs = AgeSpecificFertilityRatesByYear.load("age_specific_fertility_rates/survey-1960.xlsx");

            for (int year : yearly_asfrs.years())
            {
                AgeSpecificFertilityRates asfr = yearly_asfrs.getForYear(year);
                asfr.display("Возростная плодовитость для " + year + " года");
            }

            Util.unused(yearly_asfrs);
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
        }
    }
}
