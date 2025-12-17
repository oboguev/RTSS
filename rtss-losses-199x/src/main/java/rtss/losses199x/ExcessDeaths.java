package rtss.losses199x;

import java.util.HashMap;
import java.util.Map;

// import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.data.selectors.LocalityGender;
import rtss.losses199x.util.PrintTable;
import rtss.rosbris.RosBrisDeathRates;
import rtss.rosbris.RosBrisPopulationExposureForDeaths;
import rtss.rosbris.RosBrisTerritory;
import rtss.util.Util;

public class ExcessDeaths
{
    private Map<Integer, PopulationByLocality> year2deaths_actual_rates = new HashMap<>();;
    private Map<Integer, PopulationByLocality> year2deaths_reference_rates = new HashMap<>();

    private final int yy1 = 1989;
    private final int yy2 = 2015;

    public void eval() throws Exception
    {
        year2deaths_actual_rates.clear();
        year2deaths_reference_rates.clear();

        RosBrisPopulationExposureForDeaths.use2021census(true);
        RosBrisDeathRates.use2021census(true);

        // CombinedMortalityTable cmt = LoadData.mortalityTable1986();
        // RosBrisDeathRates reference_rates = RosBrisDeathRates.from(cmt, RosBrisTerritory.RF_BEFORE_2014, 1986);
        RosBrisDeathRates reference_rates = RosBrisDeathRates.loadMX(RosBrisTerritory.RF_BEFORE_2014, 1989);
        
        if (Util.False)
        {
            Util.out("");
            Util.out("==== age mx_reference mx_89");
            Util.out("");
            RosBrisDeathRates rates89 = RosBrisDeathRates.loadMX(RosBrisTerritory.RF_BEFORE_2014, 1989);
            for (int age = 0; age <= 50; age++)
            {
                double mx_reference = reference_rates.mx(Locality.URBAN, Gender.MALE, age);
                double mx_89 = rates89.mx(Locality.URBAN, Gender.MALE, age);
                Util.out(String.format("%3d %.5f %.5f", age, mx_reference, mx_89));
            }
            Util.out("===================");
        }
        
        for (int year = yy1; year <= yy2; year++)
        {
            PopulationByLocality exposure = RosBrisPopulationExposureForDeaths.getPopulationByLocality(RosBrisTerritory.RF_BEFORE_2014, year);
            RosBrisDeathRates rates = RosBrisDeathRates.loadMX(RosBrisTerritory.RF_BEFORE_2014, year);

            PopulationByLocality d_actual_rates = deaths(exposure, rates);
            PopulationByLocality d_reference_rates = deaths(exposure, reference_rates);

            year2deaths_actual_rates.put(year, d_actual_rates);
            year2deaths_reference_rates.put(year, d_reference_rates);
        }

        print(Locality.URBAN);
        print(Locality.RURAL);
        print(Locality.TOTAL);
    }

    /*
     * Число и струкура смертей при данной экспозиции и уровнях смертности
     */
    private PopulationByLocality deaths(PopulationByLocality exposure, RosBrisDeathRates rates) throws Exception
    {
        LocalityGender[] lgs = { LocalityGender.URBAN_MALE, LocalityGender.URBAN_FEMALE, LocalityGender.RURAL_MALE, LocalityGender.RURAL_FEMALE };

        PopulationByLocality deaths = PopulationByLocality.newPopulationByLocality();

        for (LocalityGender lg : lgs)
        {
            for (int age = 0; age <= Population.MAX_AGE; age++)
            {
                double mx = rates.mx(lg.locality, lg.gender, age);
                double pop = exposure.get(lg.locality, lg.gender, age);
                deaths.set(lg.locality, lg.gender, age, pop * mx);
            }
        }

        deaths.recalcFromUrbanRuralBasic();

        return deaths;
    }

    private void print(Locality locality) throws Exception
    {
        Util.out("");
        Util.out("Избыточные смерти для местности " + locality);
        Util.out("");

        PrintTable pt = new PrintTable(yy2 - yy1 + 1 + 3, 13);
        
        int nr = 0;

        {
            int nc = 0;
            
            pt.put(nr, nc++, "Year");
            pt.put(nr, nc++, "    ");
            
            pt.put(nr, nc++, "MALE");
            pt.put(nr, nc++, "");
            pt.put(nr, nc++, "");
            pt.put(nr, nc++, "    ");

            pt.put(nr, nc++, "FEMALE");
            pt.put(nr, nc++, "");
            pt.put(nr, nc++, "");
            pt.put(nr, nc++, "    ");
            
            pt.put(nr, nc++, "BOTH");
            pt.put(nr, nc++, "");
            pt.put(nr, nc++, "");
        }

        {
            nr++;
            int nc = 0;
            
            pt.put(nr, nc++, "");
            pt.put(nr, nc++, "    ");
            
            pt.put(nr, nc++, "expected");
            pt.put(nr, nc++, "actual");
            pt.put(nr, nc++, "excess");
            pt.put(nr, nc++, "    ");

            pt.put(nr, nc++, "expected");
            pt.put(nr, nc++, "actual");
            pt.put(nr, nc++, "excess");
            pt.put(nr, nc++, "    ");
            
            pt.put(nr, nc++, "expected");
            pt.put(nr, nc++, "actual");
            pt.put(nr, nc++, "excess");
        }

        for (int year = yy1; year <= yy2; year++)
        {
            PopulationByLocality d_reference = year2deaths_reference_rates.get(year);
            PopulationByLocality d_actual = year2deaths_actual_rates.get(year);

            nr = year - yy1 + 2;
            int nc = 0;

            // year
            pt.put(nr, nc++, "" + year);
            pt.put(nr, nc++, "");

            // male: expected -- actual -- excess
            put4(pt, nr, nc, d_reference, d_actual, locality, Gender.MALE, true);
            nc += 4;

            // female: expected -- actual -- excess
            put4(pt, nr, nc, d_reference, d_actual, locality, Gender.FEMALE, true);
            nc += 4;

            // both: expected -- actual -- excess
            put4(pt, nr, nc, d_reference, d_actual, locality, Gender.BOTH, false);
        }
        
        pt.print();
    }

    private void put4(PrintTable pt, int nr, int nc, PopulationByLocality d_reference, PopulationByLocality d_actual, Locality locality,
            Gender gender, boolean spacer) throws Exception
    {
        double expected = d_reference.sum(locality, gender, 0, Population.MAX_AGE);
        double actual = d_actual.sum(locality, gender, 0, Population.MAX_AGE);
        double excess = actual - expected;
        if (excess < 0) excess = 0;
        
        pt.put(nr, nc++, f2s(expected));
        pt.put(nr, nc++, f2s(actual));
        pt.put(nr, nc++, f2s(excess));

        if (spacer)
            pt.put(nr, nc++, "");

    }
    
    private String f2s(double v)
    {
        return String.format("%,d", Math.round(v));
    }
}
