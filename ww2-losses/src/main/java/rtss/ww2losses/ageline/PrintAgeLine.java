package rtss.ww2losses.ageline;

import rtss.ww2losses.HalfYearEntry;
import rtss.ww2losses.Main.Phase;

import static rtss.data.population.projection.ForwardPopulation.years2days;

import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

public class PrintAgeLine
{
    private static final int ndays = years2days(0.5);
    private static PopulationContext p1946_actual;

    private static Area currentArea = null;
    private static Phase currentPhase = null;

    private static Gender traceGender = null;
    private static Integer traceAgeDay = null;
    private static Area traceArea = null;

    /* на начало 1941 */
    public static void traceAgeYear(Area area, Gender gender, double ageYears)
    {
        traceAgeDay(area, gender, years2days(ageYears));
    }

    /* на начало 1941 */
    public static void traceAgeDay(Area area, Gender gender, int ageDays)
    {
        traceArea = area;
        traceGender = gender;
        traceAgeDay = ageDays;
    }

    public static void setAreaPhase(Area area, Phase phase, PopulationContext arg_p1946_actual)
    {
        currentArea = area;
        currentPhase = phase;
        p1946_actual = arg_p1946_actual;
    }

    public static void printSteerActual(
            Gender gender,
            int age_ndays_mid1941,
            HalfYearEntry he,
            int nd1, int nd2,
            double start_population,
            double end_population, double peace_deaths, double excess_war_deaths, double immigration) throws Exception
    {
        if (currentPhase == Phase.ACTUAL &&
            currentArea == traceArea &&
            gender == traceGender &&
            traceAgeDay != null && traceAgeDay + ndays == age_ndays_mid1941)
        {
            // below
        }
        else
        {
            return;
        }
        
        String heid = he.id();
        if (heid.equals("1941.2"))
        {
            Util.out(String.format("Проводка линии %s возрасте nd.age=%d (years.age=%.3f) на начало 1941", gender.name(), traceAgeDay, traceAgeDay/365.0));
            Util.out(String.format("    >>> halfyear p_start => p_end total_deaths immigration peace_deaths excess_war_deaths"));
        }
        
        Util.out(String.format("    >>> %s %.3f => %.3f  %.3f  %.3f  %.3f  %.3f", he.id(), start_population, end_population, peace_deaths + excess_war_deaths, immigration, peace_deaths, excess_war_deaths));
        
        if (heid.equals("1945.2"))
        {
            Util.out(String.format("    >>> %s %.3f", "1946", p1946_actual.getDay(Locality.TOTAL, gender, traceAgeDay + 10 * ndays)));
        }
    }
}
