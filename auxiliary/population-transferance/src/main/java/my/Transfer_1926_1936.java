package my;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import data.mortality.CombinedMortalityTable;
import data.mortality.MortalityInfo;
import data.population.Population;
import data.population.PopulationByLocality;
import data.selectors.Gender;
import data.selectors.Locality;

public class Transfer_1926_1936
{
    private static final int MAX_AGE = Population.MAX_AGE;

    private CombinedMortalityTable mt1926;
    private PopulationByLocality p1926;
    private PopulationByLocality p1937;

    private Map<Integer, Double> urban_male_fraction_yyyy;
    private Map<Integer, Double> urban_female_fraction_yyyy;

    private final double BirthRate = 44.0; // for the whole USSR in 1926
    private final double MaleFemaleBirthRatio = 1.05;

    public void transfer() throws Exception
    {
        mt1926 = new CombinedMortalityTable("mortality_tables/USSR/1926-1927");
        p1926 = PopulationByLocality.load("population_data/USSR/1926");
        p1937 = PopulationByLocality.load("population_data/USSR/1937");

        /*
         * 1926 census was on 1926-12-17
         * 1937 census was on 1937-01-06, almost the end of 1936 
         */

        double urban_female_fraction_1926 = urban_fraction(p1926, Gender.FEMALE);
        double urban_female_fraction_1936 = urban_fraction(p1937, Gender.FEMALE);

        double urban_male_fraction_1926 = urban_fraction(p1926, Gender.MALE);
        double urban_male_fraction_1936 = urban_fraction(p1937, Gender.MALE);

        urban_male_fraction_yyyy = interpolate_linear(1926, urban_male_fraction_1926, 1936,
                                                      urban_male_fraction_1936);
        urban_female_fraction_yyyy = interpolate_linear(1926, urban_female_fraction_1926, 1936,
                                                        urban_female_fraction_1936);

        /*
         * Forward the population for whole years 
         */
        PopulationByLocality p = p1926;
        int year = 1926;
        double yfraction = 1.0;
        for (;;)
        {
            year++;
            p = forward(p, mt1926, yfraction);

            /*
             * Redistribute population between rural and urban 
             */
            p = urbanize(p, Gender.MALE, urban_male_fraction_yyyy.get(year));
            p = urbanize(p, Gender.FEMALE, urban_female_fraction_yyyy.get(year));

            if (year == 1936)
                break;
        }
        
        /*
         * Forward the population for a fractional part of a year 
         */
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Date d1936 = df.parse("1936-12-17");
        Date d1937 = df.parse("1937-01-06");
        long ndays = Duration.between(d1936.toInstant(), d1937.toInstant()).toDays();
        yfraction = ndays / 365.0;
        p = forward(p, mt1926, yfraction);

        Util.out(String
                .format("Population expected to survive from the end of 1926 till the end of 1936 and be 10+: %,d ",
                        Math.round(p.sum(Locality.TOTAL, Gender.BOTH, 10, MAX_AGE))));

        Util.out(String.format("Actual early 1937 population ages 10 years and older: %,d",
                               Math.round(p1937.sum(Locality.TOTAL, Gender.BOTH, 10, MAX_AGE))));
        Util.out("");
        Util.out(String.format("Actual early 1937 population all ages: %,d",
                               Math.round(p1937.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE))));

        Util.out("");
        Util.out(String.format("Expected population ages 0-9 in early 1937: %,d",
                               Math.round(p.sum(Locality.TOTAL, Gender.BOTH, 0, 9))));

        Util.out(String.format("Actual population ages 0-9 in early 1937: %,d",
                               Math.round(p1937.sum(Locality.TOTAL, Gender.BOTH, 0, 9))));
    }

    private double urban_fraction(PopulationByLocality p, Gender gender) throws Exception
    {
        double total = p.sum(Locality.TOTAL, gender, 0, MAX_AGE);
        double urban = p.sum(Locality.URBAN, gender, 0, MAX_AGE);
        return urban / total;
    }

    private Map<Integer, Double> interpolate_linear(int y1, double v1, int y2, double v2)
            throws Exception
    {
        Map<Integer, Double> m = new HashMap<>();

        if (y1 > y2)
            throw new Exception("Invalid arguments");

        if (y1 == y2)
        {
            if (v1 != v2)
                throw new Exception("Invalid arguments");
            m.put(y1, v1);
        }
        else
        {
            for (int y = y1; y <= y2; y++)
            {
                double v = v1 + (v2 - v1) * (y - y1) / (y2 - y1);
                m.put(y, v);
            }
        }

        return m;
    }

    public PopulationByLocality forward(PopulationByLocality p, CombinedMortalityTable mt, double yfraction)
            throws Exception
    {
        PopulationByLocality pto = PopulationByLocality.newPopulationByLocality();
        forward(pto, p, Locality.RURAL, mt, yfraction);
        forward(pto, p, Locality.URBAN, mt, yfraction);
        pto.recalcTotal();
        pto.validate();
        return pto;
    }

    public void forward(PopulationByLocality pto,
                        PopulationByLocality p,
                        Locality locality,
                        CombinedMortalityTable mt, 
                        double yfraction)
            throws Exception
    {
        double sum = p.sum(locality, Gender.BOTH, 0, MAX_AGE);
        double births = sum * yfraction * BirthRate / 1000;
        double m_births = births * MaleFemaleBirthRatio / (1 + MaleFemaleBirthRatio);
        double f_births = births * 1.0 / (1 + MaleFemaleBirthRatio);

        forward(pto, p, locality, Gender.MALE, mt, yfraction);
        forward(pto, p, locality, Gender.FEMALE, mt, yfraction);

        pto.set(locality, Gender.MALE, 0, m_births);
        pto.set(locality, Gender.FEMALE, 0, f_births);

        pto.makeBoth(locality);
    }

    public void forward(PopulationByLocality pto,
                        PopulationByLocality p,
                        Locality locality,
                        Gender gender,
                        CombinedMortalityTable mt,
                        double yfraction)
            throws Exception
    {
        pto.set(locality, gender, 0, 0);

        for (int age = 0; age <= MAX_AGE; age++)
        {
            MortalityInfo mi = mt.get(locality, gender, age);
            double initial = p.get(locality, gender, age);
            double deaths = initial * (1.0 - mi.px) * yfraction;
            double v = initial - deaths;
            if (age == MAX_AGE)
            {
                pto.set(locality, gender, MAX_AGE, v + pto.get(locality, gender, MAX_AGE));
            }
            else
            {
                pto.set(locality, gender, age + 1, v);
            }
        }
    }

    public PopulationByLocality urbanize(PopulationByLocality p,
                                         Gender gender,
                                         double target_urban_level)
            throws Exception
    {
        PopulationByLocality pto = p.clone();

        /*
         * Target and current amounts of urban population
         */
        double target_urban = target_urban_level * p.sum(Locality.TOTAL, gender, 0, MAX_AGE);
        double current_urban = p.sum(Locality.URBAN, gender, 0, MAX_AGE);

        /*
         * Amount of population to move 
         */
        double move = target_urban - current_urban;
        if (move < 0)
            throw new Exception("Negative rural -> urban movement amount");

        /*
         * We only move population in age groups 0-49
         */
        double rural049 = p.sum(Locality.RURAL, gender, 0, 49);
        double factor = move / rural049;
        for (int age = 0; age <= 49; age++)
        {
            double r = p.get(Locality.RURAL, gender, age);
            double u = p.get(Locality.URBAN, gender, age);
            move = r * factor;
            pto.set(Locality.RURAL, gender, age, r - move);
            pto.set(Locality.URBAN, gender, age, u + move);
        }

        pto.makeBoth(Locality.RURAL);
        pto.makeBoth(Locality.URBAN);
        pto.resetUnknown();
        pto.resetTotal();

        pto.validate();

        return pto;
    }
}
