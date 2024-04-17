package rtss.data.population.synthetic;

import org.apache.commons.lang3.mutable.MutableDouble;

import rtss.data.population.Population;
import rtss.data.population.PopulationByLocality;
import rtss.data.population.RescalePopulation;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;

public class PopulationADH
{
    public static Population getPopulation(Area area, int year) throws Exception
    {
        return getPopulation(area, "" + year);
    }
    
    public static Population getPopulation(Area area, String year) throws Exception
    {
        String path = String.format("population_data/%s/%s-population-ADH.xlsx", area.name(), area.name());
        
        MutableDouble m_unknown = new MutableDouble(); 
        double[] m = PopulationFromExcel.loadCounts(path, Gender.MALE, year, m_unknown);

        MutableDouble f_unknown = new MutableDouble(); 
        double[] f = PopulationFromExcel.loadCounts(path, Gender.FEMALE, year, f_unknown);
        
        Population p = new Population(Locality.TOTAL, m, m_unknown.doubleValue(), f, f_unknown.doubleValue());
        
        /* population data in AHD book (and Excel file) is in thousands */
        p = RescalePopulation.scaleBy(p, 1000);
        
        return p;
    }
    
    /* ========================================================================== */

    public static PopulationByLocality getPopulationByLocality(Area area, int year) throws Exception
    {
        return getPopulationByLocality(area, "" + year);
    }
    
    public static PopulationByLocality getPopulationByLocality(Area area, String year) throws Exception
    {
        return new PopulationByLocality(getPopulation(area, year));  
    }
}
