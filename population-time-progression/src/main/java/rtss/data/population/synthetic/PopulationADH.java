package rtss.data.population.synthetic;

import org.apache.commons.lang3.mutable.MutableDouble;

import rtss.data.population.Population;
import rtss.data.population.PopulationByLocality;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;

public class PopulationADH
{
    public static Population getPopulation(Area area, int year) throws Exception
    {
        String path = String.format("population_data/%s/%s-population-ADH.xlsx", area.name(), area.name());
        
        MutableDouble m_unknown = new MutableDouble(); 
        double[] m = PopulationFromExcel.loadCounts(path, Gender.MALE, year, m_unknown);

        MutableDouble f_unknown = new MutableDouble(); 
        double[] f = PopulationFromExcel.loadCounts(path, Gender.FEMALE, year, f_unknown);
        
        return new Population(Locality.TOTAL, m, m_unknown.doubleValue(), f, f_unknown.doubleValue());
    }

    public static PopulationByLocality getPopulationByLocality(Area area, int year) throws Exception
    {
        return new PopulationByLocality(getPopulation(area, year));  
    }
}
