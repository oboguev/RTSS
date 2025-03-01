package rtss.ww2losses.ageline;

import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;
import rtss.ww2losses.HalfYearEntries;
import rtss.ww2losses.HalfYearEntry;
import rtss.ww2losses.ageline.warmodel.WarAttritionModel;

import static rtss.data.population.projection.ForwardPopulation.years2days;

import rtss.data.population.struct.PopulationContext;

/*
 * Выичслить интенсивности потерь возрастных линий
 */
public class EvalAgeLineLossIntensities
{
    private final HalfYearEntries<HalfYearEntry> halves;
    private final WarAttritionModel wam; 
    private double[] ac_immigration;

    public EvalAgeLineLossIntensities(HalfYearEntries<HalfYearEntry> halves, WarAttritionModel wam)
    {
        this(halves, wam, null);
    }

    public EvalAgeLineLossIntensities(HalfYearEntries<HalfYearEntry> halves, WarAttritionModel wam , double[] ac_immigration)
    {
        this.halves = halves;
        this.wam = wam;
        this.ac_immigration = ac_immigration;
    }

    public void setImmigration(double[] ac_immigration)
    {
        this.ac_immigration = ac_immigration;
    }

    /* ========================================================================================================== */

    /*
     * Для каждой из возрастных линий вычислить интенсивность потерь (отдельную для каждой линии), которая связует 
     * начальное население этой линии (в середине 1941) с конечным (в началу 1946) при данной модели военных потерь @wam.
     * 
     * Для некоторых линий интенсивность может быть отрицательной (если исходное население линии слишком мало,
     * или конечное слишком велико). 
     */
    public AgeLineFactorIntensities evalPreliminaryLossIntensity(
            final PopulationContext p1946_actual, 
            HalfYearEntries<HalfYearEntry> immigration_halves) throws Exception
    {
        AgeLineFactorIntensities intensities = new AgeLineFactorIntensities();
        HalfYearEntry he1941_2 = halves.get("1941.2");
        PopulationContext p1941_2 = he1941_2.p_nonwar_without_births;

        SteerAgeLine steer = new SteerAgeLine(halves, wam, null);
        EvalAgeLinelLossIntensity eval = new EvalAgeLinelLossIntensity(steer);

        int ndays = 9 * years2days(0.5);

        double v, initial_population, final_population;

        for (int nd = 0; nd <= p1941_2.MAX_DAY; nd++)
        {
            if (nd + ndays > p1946_actual.MAX_DAY)
                break;
            
            for (Gender gender : Gender.TwoGenders)
            {
                initial_population = p1941_2.getDay(Locality.TOTAL, gender, nd);
                final_population = p1946_actual.getDay(Locality.TOTAL, gender, nd + ndays);
                v = eval.evalPreliminaryLossIntensity(nd, gender, initial_population, final_population, immigration_halves);
                intensities.set(gender, nd, v);
            }
        }

        return intensities;
    }

    /* ========================================================================================================== */

    /*
     * Для каждой из возрастных линий вычислить интенсивность иммиграции, которая заполняет (обнуляет) разрыв
     * между конечным насеелением на 1946 год по фактической величине и по проводке возрастной линии при 
     * интенсивности её потерь указанной в @alis.
     * 
     * Применяеется только для РСФСР, т.к. для СССР иммиграции нет.
     */
    public void evalMigration(
            final PopulationContext p1946_actual,
            AgeLineFactorIntensities amig,
            final AgeLineFactorIntensities alis,
            final AgeLineFactorIntensities alis_initial,
            final Gender gender, 
            final double age1, 
            final double age2) throws Exception
    {
        int nd1 = years2days(age1);
        int nd2 = years2days(age2);

        HalfYearEntry he1941_2 = halves.get("1941.2");
        PopulationContext p1941_2 = he1941_2.p_nonwar_without_births;

        SteerAgeLine steer = new SteerAgeLine(halves, wam, ac_immigration);
        EvalAgeLinelLossIntensity eval = new EvalAgeLinelLossIntensity(steer);

        int ndays = 9 * years2days(0.5);

        double v, initial_population, final_population, loss_intensity;
        double min_v = 0, min_vp = 0;

        for (int nd = nd1; nd <= nd2; nd++)
        {
            if (nd + ndays > p1946_actual.MAX_DAY)
                break;

            initial_population = p1941_2.getDay(Locality.TOTAL, gender, nd);
            final_population = p1946_actual.getDay(Locality.TOTAL, gender, nd + ndays);
            loss_intensity = alis.get(gender, nd);
            
            if (alis_initial != null && Util.same(loss_intensity, alis_initial.get(gender, nd), 1e-5))
                continue;
            
            v = eval.evalMigrationIntensity(nd, gender, initial_population, final_population, loss_intensity);

            if (v < 0)
            {
                /*
                 * Итерация в evalMigrationIntensity может прерваться, дав маленькое отрицательное значение иммиграции.
                 * Проверить, что оно мало и обнулить его.
                 */
                min_v = Math.min(min_v, v);
                min_vp = Math.min(min_vp, v * initial_population);

                if (Math.abs(v) < 5e-4 && Math.abs(initial_population * v) < 0.5)
                {
                    v = 0;
                }
                else
                {
                    // Util.err(String.format("evalMigration: v = %f, vp = %f", v, v * initial_population));
                }
            }
            
            if (v < 0)
            {
                /*
                 * Если по-прежнему отрицательное значение, значить миграция для данной линии не требуется 
                 */
                v = 0;
            }

            Util.assertion(v >= 0);

            amig.set(gender, nd, v);
        }
        
        if (min_v < 0)
        {
            // Util.err(String.format("evalMigration: min_v = %f, min_vp = %f", min_v, min_vp));
        }
    }

    /* ========================================================================================================== */

    /*
     * Обработать возрастные линии, вычислив на их основании для всех полугодий фактическое население
     * на начало полугодия, количество смертей за полугодие по категориям и иммиграцию за полугодие.
     */
    public void processAgeLines(
            final AgeLineFactorIntensities alis,
            final AgeLineFactorIntensities amig,
            final PopulationContext p1946_actual) throws Exception
    {
        Util.assertion((amig != null) == (ac_immigration != null));

        HalfYearEntry he1941_2 = halves.get("1941.2");
        PopulationContext p1941_2 = he1941_2.p_nonwar_without_births;

        SteerAgeLine steer = new SteerAgeLine(halves, wam, ac_immigration);
        int ndays = 9 * years2days(0.5);

        double initial_population;

        for (int nd = 0; nd <= p1941_2.MAX_DAY; nd++)
        {
            if (nd + ndays > p1946_actual.MAX_DAY)
                break;

            initial_population = p1941_2.getDay(Locality.TOTAL, Gender.MALE, nd);
            steer.steerActual(Gender.MALE, nd, alis, amig, initial_population);

            initial_population = p1941_2.getDay(Locality.TOTAL, Gender.FEMALE, nd);
            steer.steerActual(Gender.FEMALE, nd, alis, amig, initial_population);
        }

        processSeniorAgeLines(steer, Gender.MALE, alis, amig, p1941_2, p1946_actual);
        processSeniorAgeLines(steer, Gender.FEMALE, alis, amig, p1941_2, p1946_actual);
    }

    /*
     * Обработать возрастные линии не имеющие прямого конца в 1946 году,
     * т.е. в возрастах на начало середины 1941 старше чем MAX_AGE - 4.5 + 1.
     */
    private void processSeniorAgeLines(
            SteerAgeLine steer,
            Gender gender,
            AgeLineFactorIntensities alis,
            AgeLineFactorIntensities amig,
            PopulationContext p1941_2,
            PopulationContext p1946_actual) throws Exception
    {
        int ndays = 9 * years2days(0.5);

        int nd1 = p1946_actual.MAX_DAY - ndays;

        double senior_loss_intensity = alis.average(gender, nd1 - 1 - years2days(1.0), nd1 - 1);
        double initial_population;

        Double senior_immigration_intensity = null;
        if (amig != null)
            senior_immigration_intensity = amig.average(gender, nd1 - 1 - years2days(1.0), nd1 - 1);

        for (int nd = nd1; nd <= p1941_2.MAX_DAY; nd++)
        {
            initial_population = p1941_2.getDay(Locality.TOTAL, gender, nd);
            steer.steerActual(gender, nd, senior_loss_intensity, senior_immigration_intensity, initial_population);
        }
    }
}
