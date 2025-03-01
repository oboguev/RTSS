package rtss.ww2losses.ageline;

import rtss.data.population.projection.ForwardPopulation;
import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;
import rtss.ww2losses.HalfYearEntries;
import rtss.ww2losses.HalfYearEntry;
import rtss.ww2losses.ageline.warmodel.WarAttritionModel;

/*
 * Проводка возрастной линии от середины 1941 года до начала 1946 года.
 * 
 * При проводке учитывается естественная смертность по мирным таблицам смертности
 * и избыточная смертность военного времени сверх неё.
 */
public class SteerAgeLine
{
    private final HalfYearEntries<HalfYearEntry> halves;
    private final WarAttritionModel wam;
    private final double[] ac_immigration;

    /*
     * @halves           = данные для полугодий, от начала 1941 до начала 1946 года
     * @wam              = модель военных потерь
     * @ac_immigration   = распределение иммиграционной интенсивность по полугодиям   
     */
    public SteerAgeLine(HalfYearEntries<HalfYearEntry> halves, WarAttritionModel wam, double[] ac_immigration)
    {
        this.halves = halves;
        this.wam = wam;
        this.ac_immigration = ac_immigration;
    }

    /*
     * Вычислить остаток населения данного возраста и пола к началу 1946 года.
     * 
     * @initial_age_ndays        = начальный возраст линии в середине 1941 года
     * @gender                   = пол
     * @initial_population       = начальная численность населения линии в середине 1941 года
     * @loss_intensity           = интенсивность военной сверхсмертности для данной линии
     * @immigration_intensity    = интенсивность иммиграции для данной линии
     * @immigration_halves       = численная величина иммиграции
     * 
     * Только один из параметров @immigration_intensity и @immigration_halves может быть указан как non-null.
     * 
     * Военные потери в полугодии вычисляются как ac_xxx * initial_population * loss_intensity.     
     */
    public double steerPreliminary(
            int initial_age_ndays,
            Gender gender,
            double initial_population,
            double loss_intensity,
            Double immigration_intensity,
            HalfYearEntries<HalfYearEntry> immigration_halves) throws Exception
    {
        if (ac_immigration == null)
        {
            Util.assertion(immigration_intensity == null || immigration_intensity == 0);
        }
        else
        {
            Util.assertion(immigration_intensity != null);
        }
        
        Util.assertion(immigration_halves == null || immigration_intensity == null);

        Util.assertion(initial_population >= 0);

        double population = initial_population;
        double delta = 0;
        int nd_age = initial_age_ndays;
        int span = ForwardPopulation.years2days(0.5);

        for (HalfYearEntry he = halves.get("1941.2"); he.year != 1946; he = he.next)
        {
            int nd1 = nd_age;
            int nd2 = nd1 + span;
            int ndm = (nd1 + nd2) / 2;

            double peace_deaths = (population <= 0) ? 0 : population * deathRatio(he, gender, nd1, nd2);

            double excess_war_deaths = loss_intensity * wam.excessWarDeaths(gender, ndm, he, initial_population);

            double immigration = 0;
            if (ac_immigration != null && immigration_intensity != null)
            {
                immigration = ac_immigration[ac_index(he)] * initial_population * immigration_intensity;
            }
            else if (immigration_halves != null)
            {
                HalfYearEntry he_imm = immigration_halves.get(he.year, he.halfyear);
                if (he_imm.immigration != null)
                {
                    setcap(he_imm.immigration);
                    immigration = he_imm.immigration.getDay(Locality.TOTAL, gender, cap(nd1));
                }
            }

            population += immigration;
            population -= peace_deaths;
            population -= excess_war_deaths;

            if (population < 0)
            {
                delta += population;
                population = 0;
            }

            Util.assertion(population >= 0);

            nd_age += span;
        }

        return population + delta;
    }

    /*
     * Доля выживающих при перемещении из возраста @nd1 в @nd2
     */
    private double survivalRatio(HalfYearEntry he, Gender gender, int nd1, int nd2) throws Exception
    {
        double[] lx = (gender == Gender.MALE) ? he.peace_lx_male : he.peace_lx_female;
        int back = 0;
        if (nd2 >= lx.length)
            back = nd2 - lx.length + 1;
        double r = lx[nd2 - back] / lx[nd1 - back];
        Util.assertion(r >= 0 && r <= 1);
        return r;
    }

    /*
     * Доля умирающих при перемещении из возраста @nd1 в @nd2
     */
    private double deathRatio(HalfYearEntry he, Gender gender, int nd1, int nd2) throws Exception
    {
        return 1 - survivalRatio(he, gender, nd1, nd2);
    }

    /* индекс в массивы ac_xxx */
    private int ac_index(HalfYearEntry he) throws Exception
    {
        return (he.year - 1941) * 2 + he.halfyear.seq(0);
    }

    /* ============================================================================== */

    /*
     * Обработка возрастной линии с учётом ранее найденных коэффициентов интенсивности военных потерь
     * и интенсивности иммиграции для этой линии.
     * 
     * Вычислить на их основании для всех полугодий фактическое население на начало полугодия, 
     * количество смертей за полугодие по категориям и иммиграцию за полугодие.
     */
    public void steerActual(Gender gender,
            int initial_age_ndays,
            AgeLineFactorIntensities alis,
            AgeLineFactorIntensities amig,
            double initial_population) throws Exception
    {
        double loss_intensity = alis.get(gender, initial_age_ndays);
        Double immigration_intensity = (amig == null) ? null : amig.get(gender, initial_age_ndays);
        steerActual(gender, initial_age_ndays, loss_intensity, immigration_intensity, initial_population);
    }

    public void steerActual(Gender gender,
            int initial_age_ndays,
            double loss_intensity,
            Double immigration_intensity,
            double initial_population) throws Exception
    {
        if (ac_immigration == null)
        {
            Util.assertion(immigration_intensity == null);
        }
        else
        {
            Util.assertion(immigration_intensity != null);
        }

        Util.assertion(initial_population >= 0);

        double population = initial_population;
        int nd_age = initial_age_ndays;
        int span = ForwardPopulation.years2days(0.5);

        for (HalfYearEntry he = halves.get("1941.2"); he.year != 1946; he = he.next)
        {
            int nd1 = nd_age;
            int nd2 = nd1 + span;
            int ndm = (nd1 + nd2) / 2;
            
            final double start_population = population;

            double peace_deaths = (population <= 0) ? 0 : population * deathRatio(he, gender, nd1, nd2);

            double excess_war_deaths = loss_intensity * wam.excessWarDeaths(gender, ndm, he, initial_population);

            double immigration = 0;
            if (ac_immigration != null)
                immigration = ac_immigration[ac_index(he)] * initial_population * immigration_intensity;

            population += immigration;
            population -= peace_deaths;
            population -= excess_war_deaths;

            Util.assertion(population >= 0);
            Util.assertion(peace_deaths >= 0);

            if (loss_intensity >= 0)
                Util.assertion(excess_war_deaths >= 0);
            else
                Util.assertion(excess_war_deaths <= 0);

            setcap(he.next.actual_population);

            he.next.actual_population.addDay(Locality.TOTAL, gender, cap(nd2), population);

            he.actual_peace_deaths.addDay(Locality.TOTAL, gender, cap(nd1), peace_deaths);
            he.actual_excess_wartime_deaths.addDay(Locality.TOTAL, gender, cap(nd1), excess_war_deaths);
            he.actual_deaths.addDay(Locality.TOTAL, gender, cap(nd1), peace_deaths + excess_war_deaths);
            he.immigration.addDay(Locality.TOTAL, gender, cap(nd1), immigration);
            
            PrintAgeLine.printSteerActual(gender, initial_age_ndays, he, nd1, nd2, start_population, population, peace_deaths, excess_war_deaths, immigration);

            nd_age += span;
        }
    }

    /*
     * Ограничение по возрасту. Аккумулировать суммы для групп с возрастом выше максимального
     * в максимальном возрасте.
     */
    private int maxday;

    private void setcap(PopulationContext p)
    {
        maxday = p.MAX_DAY;
    }

    private int cap(int nd)
    {
        return nd <= maxday ? nd : maxday;
    }
}
