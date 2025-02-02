package rtss.ww2losses.ageline;

import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;
import rtss.ww2losses.HalfYearEntries;
import rtss.ww2losses.HalfYearEntry;

import static rtss.data.population.forward.ForwardPopulation.years2days;

import rtss.data.population.struct.PopulationContext;

/*
 * Выичслить интенсивности потерь возрастных линий
 */
public class EvalAgeLineLossIntensities
{
    private final HalfYearEntries<HalfYearEntry> halves;
    private final double[] ac_general;
    private final double[] ac_conscripts;
    private double[] ac_immigration;

    public EvalAgeLineLossIntensities(HalfYearEntries<HalfYearEntry> halves, double[] ac_general, double[] ac_conscripts)
    {
        this(halves, ac_general, ac_conscripts, null);
    }

    public EvalAgeLineLossIntensities(HalfYearEntries<HalfYearEntry> halves, double[] ac_general, double[] ac_conscripts, double[] ac_immigration)
    {
        this.halves = halves;
        this.ac_general = ac_general;
        this.ac_conscripts = ac_conscripts;
        this.ac_immigration = ac_immigration;
    }

    public void setImmigration(double[] ac_immigration)
    {
        this.ac_immigration = ac_immigration;
    }

    /* ========================================================================================================== */

    public AgeLineFactorIntensities evalPreliminaryLossIntensity(PopulationContext p1946_actual) throws Exception
    {
        AgeLineFactorIntensities intensities = new AgeLineFactorIntensities();
        HalfYearEntry he1941_2 = halves.get("1941.2");
        PopulationContext p1941_2 = he1941_2.p_nonwar_without_births;

        SteerAgeLine steer = new SteerAgeLine(halves, ac_general, ac_conscripts, null);
        EvalAgeLinelLossIntensity eval = new EvalAgeLinelLossIntensity(steer);

        int ndays = 9 * years2days(0.5);

        double v, initial_population, final_population;

        for (int nd = 0; nd <= p1941_2.MAX_DAY; nd++)
        {
            if (nd + ndays > p1946_actual.MAX_DAY)
                break;

            initial_population = p1941_2.getDay(Locality.TOTAL, Gender.MALE, nd);
            final_population = p1946_actual.getDay(Locality.TOTAL, Gender.MALE, nd + ndays);
            v = eval.evalPreliminaryLossIntensity(nd, Gender.MALE, initial_population, final_population);
            intensities.set(Gender.MALE, nd, v);

            initial_population = p1941_2.getDay(Locality.TOTAL, Gender.FEMALE, nd);
            final_population = p1946_actual.getDay(Locality.TOTAL, Gender.FEMALE, nd + ndays);
            v = eval.evalPreliminaryLossIntensity(nd, Gender.FEMALE, initial_population, final_population);
            intensities.set(Gender.FEMALE, nd, v);
        }

        return intensities;
    }

    /* ========================================================================================================== */

    public void evalMigration(
            PopulationContext p1946_actual,
            AgeLineFactorIntensities amig,
            final AgeLineFactorIntensities alis,
            Gender gender, double age1, double age2) throws Exception
    {
        int nd1 = years2days(age1);
        int nd2 = years2days(age2);

        HalfYearEntry he1941_2 = halves.get("1941.2");
        PopulationContext p1941_2 = he1941_2.p_nonwar_without_births;

        SteerAgeLine steer = new SteerAgeLine(halves, ac_general, ac_conscripts, ac_immigration);
        EvalAgeLinelLossIntensity eval = new EvalAgeLinelLossIntensity(steer);

        int ndays = 9 * years2days(0.5);

        double v, initial_population, final_population, loss_intensity;
        
        for (int nd = nd1; nd <= nd2; nd++)
        {
            if (nd + ndays > p1946_actual.MAX_DAY)
                break;

            initial_population = p1941_2.getDay(Locality.TOTAL, gender, nd);
            final_population = p1946_actual.getDay(Locality.TOTAL, gender, nd + ndays);
            loss_intensity = alis.get(gender, nd);
            v = eval.evalMigrationIntensity(nd, gender, initial_population, final_population, loss_intensity);
            amig.set(gender, nd, v);
        }
    }

    /* ========================================================================================================== */

    public void processAgeLines(AgeLineFactorIntensities alis,
            AgeLineFactorIntensities amig,
            PopulationContext p1946_actual) throws Exception
    {
        Util.assertion((amig != null) == (ac_immigration != null));

        HalfYearEntry he1941_2 = halves.get("1941.2");
        PopulationContext p1941_2 = he1941_2.p_nonwar_without_births;

        SteerAgeLine steer = new SteerAgeLine(halves, ac_general, ac_conscripts, ac_immigration);
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
