package rtss.data.asfr;

import rtss.data.selectors.Area;
import rtss.util.Util;

public class ShowASFRInterpolatedByAge
{
    public static void main(String[] args)
    {
        try
        {
            AgeSpecificFertilityRatesByYear yearly_asfrs = null;

            yearly_asfrs = AgeSpecificFertilityRatesByYear.load(Area.RSFSR, "Total");
            // yearly_asfrs = AgeSpecificFertilityRatesByYear.load(Area.USSR);
            // yearly_asfrs = AgeSpecificFertilityRatesByYear.load("age_specific_fertility_rates/survey-1960.xlsx");
            
            AgeSpecificFertilityRates asfr = yearly_asfrs.getForYear(1939);
            AgeSpecificFertilityRates xasfr = InterpolateASFR_ByAge.interpolate(asfr);
            
            if (Util.False)
            {

                for (int year : yearly_asfrs.years())
                {
                    AgeSpecificFertilityRates asfr2 = yearly_asfrs.getForYear(year);
                    asfr2.display("Возростная плодовитость для " + year + " года");
                }
            }

            Util.unused(yearly_asfrs);
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
        }
    }
}
