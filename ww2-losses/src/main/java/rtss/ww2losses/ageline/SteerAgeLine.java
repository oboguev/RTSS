package rtss.ww2losses.ageline;

import rtss.data.population.forward.ForwardPopulation;
import rtss.data.selectors.Gender;
import rtss.util.Util;
import rtss.ww2losses.HalfYearEntries;
import rtss.ww2losses.HalfYearEntry;

import static rtss.data.population.forward.ForwardPopulation.years2days;

/*
 * Проводка возрастной линии от середины 1941 года до начала 1946 года.
 * 
 * При проводке учитывается естественная смертность по мирным таблицам смертности
 * и избыточная смертность военного времени сверх неё.
 */
public class SteerAgeLine
{
    private final HalfYearEntries<HalfYearEntry> halves;
    private double[] ac_general;
    private double[] ac_conscripts;

    private static final double CONSCRIPT_AGE_FROM = 18.5;
    private static final double CONSCRIPT_AGE_TO = 54.5;

    /*
     * @halves         = данные для полугодий, от начала 1941 до начала 1946 года
     * @ac_conscripts  = удельные весовые коэфициенты военной сверхсмертности (по полугодиям) для призывников  
     * @ac_general     = удельные весовые коэфициенты военной сверхсмертности (по полугодиям) для непризывного населения   
     */
    public SteerAgeLine(HalfYearEntries<HalfYearEntry> halves, double[] ac_general, double[] ac_conscripts)
    {
        this.halves = halves;
        this.ac_general = ac_general;
        this.ac_conscripts = ac_conscripts;
    }

    /*
     * Вычислить остаток населения данного возраста к началу 1946 года.
     * 
     * initial_age_ndays = начальный возраст в середине 1941 года
     * gender = пол
     * initial_population = начальная численность населения в середине 1941 года
     * loss_intensity = интенсивность военной сверхсмертности
     * 
     * Военные потери в полугодии вычисляются как ac_xxx * initial_population * loss_intensity.     
     */
    public double steer(
            int initial_age_ndays,
            Gender gender,
            double initial_population,
            double loss_intensity) throws Exception
    {
        double population = initial_population;
        int nd_age = initial_age_ndays;
        int span = ForwardPopulation.years2days(0.5);

        for (HalfYearEntry he = halves.get("1941.2"); he.year != 1946; he = he.next)
        {
            int nd1 = nd_age;
            int nd2 = nd1 + span;
            int ndm = (nd1 + nd2) / 2;

            if (population > 0)
                population *= survivalRatio(he, gender, nd1, nd2);

            double[] ac = ac(gender, ndm);
            population -= ac[ac_index(he)] * initial_population * loss_intensity;

            nd_age += span;
        }

        return population;
    }

    private double[] ac(Gender gender, int nd)
    {
        if (gender == Gender.MALE &&
            nd >= years2days(CONSCRIPT_AGE_FROM) &&
            nd <= years2days(CONSCRIPT_AGE_TO))
        {
            return ac_conscripts;
        }
        else
        {
            return ac_general;
        }
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

    private double deathRatio(HalfYearEntry he, Gender gender, int nd1, int nd2) throws Exception
    {
        return 1 - survivalRatio(he, gender, nd1, nd2);
    }

    /* индекс в массивы ac_xxx */
    private int ac_index(HalfYearEntry he) throws Exception
    {
        return (he.year - 1941) * 2 + he.halfyear.seq(0);
    }

}
