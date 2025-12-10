package rtss.rosbris;

import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.rosbris.core.RosBrisDataSet;
import rtss.rosbris.core.RosBrisDataSet.DataEntry;

/*
 * Загрузить из РосБРИС половозрастную структуру населения на середину указанного года
 * для расчёта смертности 
 */
public class RosBrisPopulationMidyearForDeaths
{
    private static RosBrisDataSet ds_1989_2014;
    private static RosBrisDataSet ds_2015_2022;

    private static RosBrisDataSet forYear(int year) throws Exception
    {
        if (year >= 1989 && year <= 2014)
        {
            if (ds_1989_2014 == null)
                ds_1989_2014 = RosBrisDataSet.load("RosBRIS/PopDa/PopDa1989-2014.txt");

            return ds_1989_2014.selectEq("Year", year);
        }
        else if (year >= 2015 && year <= 2022)
        {
            if (ds_2015_2022 == null)
                ds_2015_2022 = RosBrisDataSet.load("RosBRIS/PopDa/PopDa2015-2022.txt");
            
            return ds_2015_2022.selectEq("Year", year);
        }
        else
        {
            throw new RuntimeException("Invalid year selector");
        }
    }
    
    public static PopulationByLocality getPopulationByLocality(int territory, int year) throws Exception
    {
        Population total = getPopulation(territory, year, Locality.TOTAL);
        Population urban = getPopulation(territory, year, Locality.URBAN);
        Population rural = getPopulation(territory, year, Locality.RURAL);
        
        // allow minor diveregnce or divergence in senior ages
        PopulationByLocality p = new PopulationByLocality(total, urban, rural, 1.0 / 40_000, 96);
        p.recalcTotalLocalityFromUrbanRural();
        return p;
    }
    
    public static Population getPopulation(int territory, int year, Locality locality) throws Exception
    {
        Population p = Population.newPopulation(locality);
        p.setYearHint(year);

        addGender(p, territory, year, locality, Gender.MALE);
        addGender(p, territory, year, locality, Gender.FEMALE);
        addGender(p, territory, year, locality, Gender.BOTH);
        
        p.validateBMF(1.0 / 50_000, 96);  // allow minor diveregnce or divergence in senior ages
        p.validate(1.0 / 40_000, 96);
        p.makeBoth();

        return p;
    }

    private static void addGender(Population p, int territory, int year, Locality locality, Gender gender) throws Exception
    {
        RosBrisDataSet ds = forYear(year)
                .selectEq("Reg", territory)
                .selectEq("Group", group(locality))
                .selectEq("Sex", sex(gender));

        int size = ds.entries().size();
        if (size != 1)
            throw new Exception("No uniquely matching RosBRIS data record");
        DataEntry de = ds.entries().get(0);
        
        double[] v = new double[Population.MAX_AGE + 1];
        
        for (int age = 0; age <= Population.MAX_AGE; age++)
        {
            String key = "PopDa" + age;
            if (!de.has(key))
                throw new Exception("Missing RosBRIS data element for age " + age);
            v[age] = de.asDouble(key);
        }
        
        p.fromArray(gender, v);
    }

    private static String group(Locality locality)
    {
        switch (locality)
        {
        case RURAL:
            return "R";
        case URBAN:
            return "U";
        case TOTAL:
            return "T";
        default:
            throw new RuntimeException("Invalid locality selector");
        }
    }

    private static String sex(Gender gender)
    {
        switch (gender)
        {
        case MALE:
            return "M";
        case FEMALE:
            return "F";
        case BOTH:
            return "B";
        default:
            throw new RuntimeException("Invalid gender selector");
        }
    }
}
