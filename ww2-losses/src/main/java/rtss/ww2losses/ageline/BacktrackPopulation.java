package rtss.ww2losses.ageline;

import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;
import rtss.ww2losses.HalfYearEntries;
import rtss.ww2losses.HalfYearEntry;

import static rtss.data.population.forward.ForwardPopulation.years2days;

import rtss.data.ValueConstraint;

/*
 * Определить структуру населения в начале 1941 года, которая при данных коэффициентах
 * интенсивности военных потерь (в т.ч. например нулевых) даёт известное население на начало 1946 года. 
 */
public class BacktrackPopulation
{
    private final HalfYearEntries<HalfYearEntry> halves;
    private final WarAttritionModel wam;
    private final PopulationContext p1946_actual;
    private final int ndays_halfyear = years2days(0.5);

    public BacktrackPopulation(HalfYearEntries<HalfYearEntry> halves, WarAttritionModel wam, final PopulationContext p1946_actual)
    {
        this.halves = halves;
        this.wam = wam;
        this.p1946_actual = p1946_actual;
    }

    /* ========================================================================================= */

    /*
     * Преобразования для возраста в днях
     */
    public int nd_mid1941_to_1946(int nd)
    {
        return nd + 9 * ndays_halfyear;
    }

    public int nd_1946_to_mid1941(int nd)
    {
        return nd - 9 * ndays_halfyear;
    }

    public int nd_mid1941_to_early1941(int nd)
    {
        return nd - ndays_halfyear;
    }

    public int nd_early1941_to_mid1941(int nd)
    {
        return nd + ndays_halfyear;
    }

    /* ========================================================================================= */

    /*
     * Определить численность возрастной линии на начало 1941 года (в начале первого полугодия 1941), 
     * которая при данной интенсивности военных потерь даёт требуемую численность в начале 1946 года.
     * 
     * @initial_age_ndays = возраст на середину 1941
     */
    public double population_1946_to_early1941(
            int initial_age_ndays,
            Gender gender,
            double loss_intensity) throws Exception
    {
        double v_mid1941 = population_1946_to_mid1941(initial_age_ndays, gender, loss_intensity);
        double v_early1941 = population_mid1941_to_early1941(initial_age_ndays, gender, v_mid1941);
        return v_early1941;
    }

    /*
     * Определить численность возрастной линии на середину 1941 года (в начале второго полугодия 1941), 
     * которая при данной интенсивности военных потерь даёт требуемую численность в начале 1946 года.
     * 
     * @initial_age_ndays = возраст на середину 1941
     */
    public double population_1946_to_mid1941(
            int initial_age_ndays,
            Gender gender,
            double loss_intensity) throws Exception
    {
        Util.assertion(initial_age_ndays >= 0 && initial_age_ndays <= p1946_actual.MAX_DAY);

        // Util.assertion(nd_mid_1941_to_1946(initial_age_ndays) <= p1946_actual.MAX_DAY);
        
        SteerAgeLine steer = new SteerAgeLine(halves, wam, null);

        double initial_population = 1.0;
        Double immigration_intensity = null;

        double v1946_steer = steer.steerPreliminary(initial_age_ndays, gender, initial_population, loss_intensity, immigration_intensity);
        
        double v1946_actual;
        if (nd_mid1941_to_1946(initial_age_ndays) >= p1946_actual.MAX_DAY)
        {
            /* 
             * avoid topmost age day, as it accumulaytes the carryovers from the ages gone off range
             */
            int nd2  = p1946_actual.MAX_DAY - 1;
            int nd1 = nd2 - 100;
            v1946_actual = p1946_actual.sumDays(gender, nd1, nd2) / (nd2 - nd1 + 1);
        }
        else
        {
            v1946_actual = p1946_actual.getDay(Locality.TOTAL, gender, nd_mid1941_to_1946(initial_age_ndays));
        }

        double v_mid1941 = v1946_actual * (initial_population / v1946_steer);

        return v_mid1941;
    }

    /*
     * Определить численность возрастной линии на начало 1941 года, 
     * которая даёт требуемую численность в середине 1941 года.
     * 
     * @initial_age_ndays = возраст на середину 1941
     */
    public double population_mid1941_to_early1941(int initial_age_ndays, Gender gender, double v_mid1941) throws Exception
    {
        /* возраст в начале 1941 */
        int nd = nd_mid1941_to_early1941(initial_age_ndays);
        if (nd < 0)
            return 0;

        HalfYearEntry he = halves.get("1941.1");
        double[] lx = null;

        switch (gender)
        {
        case MALE:
            lx = he.peace_lx_male;
            break;

        case FEMALE:
            lx = he.peace_lx_female;
            break;

        default:
            throw new IllegalArgumentException();
        }

        double v_early1941 = v_mid1941 * lx[nd] / lx[initial_age_ndays];

        return v_early1941;
    }

    /* ========================================================================================= */

    /*
     * Определить структуру населения в начале 1941 года, которая при данных коэффициентах
     * интенсивности военных потерь (в т.ч. например нулевых) даёт фактическое население на начало 1946 года.  
     */
    public PopulationContext population_1946_to_early1941(AgeLineFactorIntensities alis) throws Exception
    {
        if (alis == null)
            alis = new AgeLineFactorIntensities();
        
        HalfYearEntry he1941_2 = halves.get("1941.2");
        PopulationContext p1941_2 = he1941_2.p_nonwar_without_births;
        PopulationContext p = PopulationContext.newTotalPopulationContext(ValueConstraint.NONE);

        for (Gender gender : Gender.TwoGenders)
        {
            for (int nd_mid1941 = nd_early1941_to_mid1941(0); nd_mid1941 <= p1941_2.MAX_DAY; nd_mid1941++)
            {
                double loss_intensity = alis.get(gender, nd_mid1941);
                double v = population_1946_to_early1941(nd_mid1941, gender, loss_intensity);

                int nd_early1941 = nd_mid1941_to_early1941(nd_mid1941);
                p.setDay(Locality.TOTAL, gender, nd_early1941, v);
            }
        }

        return p;
    }
}
