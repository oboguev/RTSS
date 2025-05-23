package rtss.ww2losses.ageline;

import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;
import rtss.ww2losses.ageline.warmodel.WarAttritionModel;
import rtss.ww2losses.helpers.PeacetimeMortalityTables;
import rtss.ww2losses.params.AreaParameters;
import rtss.ww2losses.struct.HalfYearEntries;
import rtss.ww2losses.struct.HalfYearEntry;
import rtss.ww2losses.struct.HalfYearEntries.HalfYearSelector;

import static rtss.data.population.projection.ForwardPopulation.years2days;

import rtss.data.ValueConstraint;

/*
 * Определить структуру населения в начале 1941 года, которая при данных коэффициентах
 * интенсивности военных потерь (в т.ч. например нулевых) даёт известное население на начало 1946 года. 
 */
public class BacktrackPopulation
{
    private final AreaParameters ap;
    private final HalfYearEntries<HalfYearEntry> halves;
    private final WarAttritionModel wam;
    private final PopulationContext p1946_actual;

    private final int ndays_halfyear = years2days(0.5);
    private final int DAYS_PER_YEAR = 365;
    private final int MAX_DAY = DAYS_PER_YEAR * (Population.MAX_AGE + 1) - 1;

    public BacktrackPopulation(AreaParameters ap, PeacetimeMortalityTables peacetimeMortalityTables, WarAttritionModel wam, final PopulationContext p1946_actual)
            throws Exception
    {
        this.ap = ap;
        this.halves = shellHalves(peacetimeMortalityTables);
        this.wam = wam;
        this.p1946_actual = p1946_actual;
    }

    /*
     * В каждом HalfYearEntry должны быть инициализованы:
     *     - year, halfyear
     *     - next, prev
     *     - peace_mt, peace_lx_male, peace_lx_female
     *     
     * Они могут быть созданы методом shellHalves.    
     */
    public BacktrackPopulation(AreaParameters ap, HalfYearEntries<HalfYearEntry> halves, WarAttritionModel wam, final PopulationContext p1946_actual) throws Exception
    {
        this.ap = ap;
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

        SteerAgeLine steer = new SteerAgeLine(ap, null, halves, wam, null);

        double initial_population = 1.0;
        Double immigration_intensity = null;

        double v1946_steer = steer.steerPreliminary(initial_age_ndays, gender, initial_population, loss_intensity, immigration_intensity, null);

        double v1946_actual;
        if (nd_mid1941_to_1946(initial_age_ndays) >= p1946_actual.MAX_DAY)
        {
            /* 
             * avoid topmost age day, as it accumulaytes the carryovers from the ages gone off range
             */
            int nd2 = p1946_actual.MAX_DAY - 1;
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

        int back = 0;
        back = Math.max(back, nd - (lx.length - 1));
        back = Math.max(back, initial_age_ndays - (lx.length - 1));

        double v_early1941 = v_mid1941 * lx[nd - back] / lx[initial_age_ndays - back];

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

        PopulationContext p = PopulationContext.newTotalPopulationContext(ValueConstraint.NONE);

        for (Gender gender : Gender.TwoGenders)
        {
            for (int nd_mid1941 = nd_early1941_to_mid1941(0); nd_mid1941 <= MAX_DAY; nd_mid1941++)
            {
                double loss_intensity = alis.get(gender, nd_mid1941);
                double v = population_1946_to_early1941(nd_mid1941, gender, loss_intensity);

                int nd_early1941 = nd_mid1941_to_early1941(nd_mid1941);
                p.setDay(Locality.TOTAL, gender, nd_early1941, v);
            }
        }

        return p;
    }

    /* ========================================================================================= */

    /*
     * Создать массив HalfYearEntry, инициализовав поля:
     *     - year, halfyear
     *     - next, prev
     *     - peace_mt, peace_lx_male, peace_lx_female
     */
    public static HalfYearEntries<HalfYearEntry> shellHalves(PeacetimeMortalityTables peacetimeMortalityTables) throws Exception
    {
        HalfYearEntries<HalfYearEntry> halves = new HalfYearEntries<HalfYearEntry>();

        for (int year = 1941; year <= 1945; year++)
        {
            halves.add(new HalfYearEntry(year, HalfYearSelector.FirstHalfYear, null, null));
            halves.add(new HalfYearEntry(year, HalfYearSelector.SecondHalfYear, null, null));
        }
        halves.add(new HalfYearEntry(1946, HalfYearSelector.FirstHalfYear, null, null));

        HalfYearEntry prev = null;
        for (HalfYearEntry curr : halves)
        {
            curr.prev = prev;
            if (prev != null)
                prev.next = curr;
            prev = curr;
        }

        for (HalfYearEntry he : halves)
        {
            he.peace_mt = peacetimeMortalityTables.getTable(he.year, he.halfyear);
            he.peace_lx_male = peacetimeMortalityTables.mt2lx(he.year, he.halfyear, he.peace_mt, Locality.TOTAL, Gender.MALE);
            he.peace_lx_female = peacetimeMortalityTables.mt2lx(he.year, he.halfyear, he.peace_mt, Locality.TOTAL, Gender.FEMALE);
        }

        return halves;
    }
}
