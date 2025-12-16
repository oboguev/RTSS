package rtss.losses199x;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.selectors.LocalityGender;
import rtss.rosbris.RosBrisDeathRates;
import rtss.rosbris.RosBrisPopulationExposureForDeaths;
import rtss.rosbris.RosBrisTerritory;

public class ExcessDeaths
{
    public void eval() throws Exception
    {
        RosBrisPopulationExposureForDeaths.use2021census(true);
        RosBrisDeathRates.use2021census(true);

        CombinedMortalityTable cmt = LoadData.mortalityTable1986();
        RosBrisDeathRates dr1986 = RosBrisDeathRates.from(cmt, RosBrisTerritory.RF_BEFORE_2014, 1986);
        
        for (int year = 1989; year <= 2015; year++)
        {
            PopulationByLocality exposure = RosBrisPopulationExposureForDeaths.getPopulationByLocality(RosBrisTerritory.RF_BEFORE_2014, year);
            RosBrisDeathRates dr = RosBrisDeathRates.loadMX(RosBrisTerritory.RF_BEFORE_2014, year);
            
            // ### create PopulationByLocality deaths for dr
            // ### create PopulationByLocality deaths for dr1986
            
        }
        // ####
    }
    
    LocalityGender[] lgs = {LocalityGender.URBAN_MALE, LocalityGender.URBAN_FEMALE, LocalityGender.RURAL_MALE, LocalityGender.RURAL_FEMALE};
    
    private PopulationByLocality deaths(PopulationByLocality exposure, RosBrisDeathRates dr) throws Exception
    {
        PopulationByLocality deaths = PopulationByLocality.newPopulationByLocality();
        
        for (LocalityGender lg : lgs)
        {
            for (int age = 0; age <= Population.MAX_AGE; age++)
            {
                // ###
            }
        }
        
        return deaths;
    }
}
