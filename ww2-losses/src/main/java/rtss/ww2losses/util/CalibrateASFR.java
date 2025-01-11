package rtss.ww2losses.util;

import rtss.data.asfr.AgeSpecificFertilityRatesByYear;
import rtss.data.population.PopulationByLocality;
import rtss.data.population.synthetic.PopulationADH;
import rtss.ww2losses.params.AreaParameters;

public class CalibrateASFR
{
    public static double calibrate1940(AreaParameters ap, AgeSpecificFertilityRatesByYear asfrs) throws Exception
    {
        PopulationByLocality p1940 = PopulationADH.getPopulationByLocality(ap.area, 1940);
        PopulationByLocality p1941 = PopulationADH.getPopulationByLocality(ap.area, 1941);
        PopulationByLocality p = p1940.avg(p1941);
        
        double cbr = asfrs.getForYear(1940).birthRate(p);
        double multiplier = ap.CBR_1940_MIDYEAR / cbr;
        return multiplier;
    }
}
