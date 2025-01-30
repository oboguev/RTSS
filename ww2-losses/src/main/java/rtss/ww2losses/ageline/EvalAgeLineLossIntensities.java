package rtss.ww2losses.ageline;

import rtss.data.population.forward.PopulationContext;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.ww2losses.HalfYearEntries;
import rtss.ww2losses.HalfYearEntry;

import static rtss.data.population.forward.ForwardPopulation.years2days;

/*
 * Выичслить интенсивности потерь возрастных линий
 */
public class EvalAgeLineLossIntensities
{
    private final HalfYearEntries<HalfYearEntry> halves;
    private final double[] ac_general;
    private final double[] ac_conscripts;

    public EvalAgeLineLossIntensities(HalfYearEntries<HalfYearEntry> halves, double[] ac_general, double[] ac_conscripts)
    {
        this.halves = halves;
        this.ac_general = ac_general;
        this.ac_conscripts = ac_conscripts;
    }

    public AgeLineLossIntensities eval(PopulationContext p1946_actual) throws Exception
    {
        AgeLineLossIntensities intensities = new AgeLineLossIntensities();
        HalfYearEntry he1941_2 = halves.get("1941.2");
        PopulationContext p1941_2 = he1941_2.p_nonwar_without_births;
        SteerAgeLine steer = new SteerAgeLine(halves, ac_general, ac_conscripts);
        EvaAgeLinelLossIntensity eval = new EvaAgeLinelLossIntensity(steer);

        int ndays = 9 * years2days(0.5);

        double v, initial_population, final_population;

        for (int nd = 0; nd <= p1941_2.MAX_DAY; nd++)
        {
            if (nd + ndays > p1946_actual.MAX_DAY)
                break;

            initial_population = p1941_2.getDay(Locality.TOTAL, Gender.MALE, nd);
            final_population = p1946_actual.getDay(Locality.TOTAL, Gender.MALE, nd + ndays);
            v = eval.evalLossIntensity(nd, Gender.MALE, initial_population, final_population);
            intensities.set(Gender.MALE, nd, v);

            initial_population = p1941_2.getDay(Locality.TOTAL, Gender.FEMALE, nd);
            final_population = p1946_actual.getDay(Locality.TOTAL, Gender.FEMALE, nd + ndays);
            v = eval.evalLossIntensity(nd, Gender.FEMALE, initial_population, final_population);
            intensities.set(Gender.FEMALE, nd, v);
        }

        return intensities;
    }
}
