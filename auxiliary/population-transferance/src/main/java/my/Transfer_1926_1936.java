package my;

import java.util.HashMap;
import java.util.Map;

import data.mortality.CombinedMortalityTable;
import data.mortality.MortalityInfo;
import data.population.Population;
import data.selectors.Gender;
import data.selectors.Locality;

public class Transfer_1926_1936
{
    private CombinedMortalityTable mt1926;
    private Population p1926 = new Population();
    private Population p1937 = new Population();
    
    private Map<Integer, Double> urban_male_fraction_yyyy;
    private Map<Integer, Double> urban_female_fraction_yyyy;
    
    public void transfer() throws Exception
    {
        mt1926 = new CombinedMortalityTable("mortality_tables/USSR/1926-1927");
        p1926.loadCombined("population_data/USSR/1926");
        p1937.loadCombined("population_data/USSR/1937");
        
        /*
         * 1926 census was on 1926-12-17
         * 1937 census was on 1937-01-06, almost the end of 1936 
         */
        
        double urban_female_fraction_1926 = urban_fraction(p1926, Gender.FEMALE);
        double urban_female_fraction_1936 = urban_fraction(p1937, Gender.FEMALE);
        
        double urban_male_fraction_1926 = urban_fraction(p1926, Gender.MALE);
        double urban_male_fraction_1936 = urban_fraction(p1937, Gender.MALE);
        
        urban_male_fraction_yyyy = interpolate_linear(1926, urban_male_fraction_1926, 1936, urban_male_fraction_1936);
        urban_female_fraction_yyyy = interpolate_linear(1926, urban_female_fraction_1926, 1936, urban_female_fraction_1936);
        
        Population p = p1926;
        int year = 1926;
        for (;;)
        {
            year++;
            p = transfer(p, mt1926);
            // ### re-break total as urban vs rural per urban_male_fraction_yyyy.get(year), urban_female_fraction_yyyy.get(year)
            if (year == 1936)
                break;
        }
        
        // ### print total
    }
    
    private double urban_fraction(Population p, Gender gender) throws Exception
    {
        double total = p.sum(Locality.TOTAL, gender, 0, Population.MAX_AGE);
        double urban = p.sum(Locality.URBAN, gender, 0, Population.MAX_AGE);
        return urban/total;
    }
    
    private Map<Integer, Double> interpolate_linear(int y1, double v1, int y2, double v2) throws Exception
    {
        Map<Integer, Double> m = new HashMap<>();
        
        if (y1 > y2)
            throw new Exception("Invalid arguments");
        
        if (y1 == y2)
        {
            if (v1 != v2)
                throw new Exception("Invalid arguments");
            m.put(y1,  v1);
        }
        else
        {
            for (int y = y1; y <= y2; y++)
            {
                double v = v1 + (v2 - v1) * (y - y1) / (y2 - y1);
                m.put(y,  v);
            }
        }
        
        return m;
    }
    
    public Population transfer(Population p, CombinedMortalityTable mt) throws Exception
    {
        Locality[] localities = { Locality.RURAL, Locality.URBAN };
        Gender[] genders = { Gender.MALE, Gender.FEMALE };
        
        for (Locality locality : localities)
        {
            for (Gender gender : genders)
            {
                for (int age = 0; age <= Population.MAX_AGE; age ++)
                {
                    MortalityInfo mi = mt.get(locality, gender, age);
                    double v = p.get(locality, gender, age);
                    // ###
                }
            }
            
            // ### calc Gender.BOTH
        }
        
        // ### calc Locality.TOTAL
        
        return null;
    }

    public void transfer(Population p, Locality locality, CombinedMortalityTable mt) throws Exception
    {
        // ###
    }

    public void transfer(Population p, Locality locality, Gender gender, CombinedMortalityTable mt) throws Exception
    {
        // ###
    }
}
