package rtss.ww2losses.helpers;

import static rtss.data.population.forward.ForwardPopulation.years2days;

import rtss.data.ValueConstraint;
import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.util.Util;
import rtss.ww2losses.Constants;
import rtss.ww2losses.HalfYearEntries;
import rtss.ww2losses.HalfYearEntry;
import rtss.ww2losses.params.AreaParameters;

/*
 * Распечатать сводку данных для полугодий
 */
public class PrintHalfYears
{
    private static double PROMILLE = 1000.0;
    private static String EMPTY = "" + (char) 0xA0;

    private double sum_actual_deaths = 0;
    private double sum_actual_excess_wartime_deaths = 0;
    private double sum_exd_conscripts = 0;
    private double sum_excess_warborn_deaths = 0;
    private double sum_expected_nonwar_births = 0;
    private double sum_actual_births = 0;
    private double sum_birth_shortfall = 0;
    private double sum_actual_warborn_deaths_baseline = 0;
    private double sum_actual_warborn_deaths = 0;

    public static void print(AreaParameters ap, HalfYearEntries<HalfYearEntry> halves) throws Exception
    {
        new PrintHalfYears().do_print(ap, halves);
    }

    public void do_print(AreaParameters ap, HalfYearEntries<HalfYearEntry> halves) throws Exception
    {
        Util.out("");
        Util.out("Величины для полугодий:");
        Util.out("");
        Util.out("    н.нач   = численность населения в начале полугодия, тыс. чел");
        Util.out("    н.сред  = средняя численность населения за полугодие, тыс. чел");
        Util.out("    н.кон   = численность населения в конце полугодия, тыс. чел");
        Util.out("");
        Util.out("    ум      = общее число смертей за полугодие, тыс. чел");
        Util.out("    с.изб   = число избыточных смертей за полугодие, тыс. чел");
        Util.out("    с.прз   = число избыточных смертей за полугодие среди мужчин призывного возраста, тыс. чел");
        Util.out("    с.инов  = число избыточных смертей за полугодие среди родившихся после середины 1941 года, тыс. чел, равняется (фcр - фcр.мир)");
        Util.out("");
        Util.out("    р.ожид    = ожидаемое число рождений в условиях мира (за полугодие)");
        Util.out("    р.факт    = фактическое число рождений (за полугодие)");
        Util.out("    р.нехв    = дефицит рождений за полугодие, тыс. новорожденных");
        Util.out("    фcр.мир   = число смертей (в данном полугодии) от фактических рождений c начала войны, ожидаемое при смертности мирного времени");
        Util.out("    фcр       = фактическое число смертей (в данном полугодии) от фактических рождений с начала войны, при фактической военной смертности");
        Util.out("");
        Util.out("    р         = рождаемость (промилле, для полугодия, но в нормировке на год");
        Util.out("    с         = смертность (промилле, для полугодия, но в нормировке на год)");
        Util.out("");

        Util.out("п/год   н.нач    н.сред    н.кон     ум    с.изб   с.прз   с.инов  р.ожид  р.факт  р.нехв   фср.мир    фср    р     с");
        Util.out("=====  =======   =======  =======  ======  ======  ======  ======  ======  ======  =======  =======  ======  ====  ====");

        for (HalfYearEntry he : halves)
        {
            if (he.year != 1946)
                print(he);
        }

        String s = String.format("%-6s %8s %8s %8s" + " %7s %7s %7s %7s" + " %7s %7s %8s %8s %7s" + " %5s %5s",
                                 "всего", EMPTY, EMPTY, EMPTY,
                                 //
                                 f2k(sum_actual_deaths),
                                 f2k(sum_actual_excess_wartime_deaths),
                                 f2k(sum_exd_conscripts),
                                 f2k(sum_excess_warborn_deaths),
                                 //
                                 f2k(sum_expected_nonwar_births),
                                 f2k(sum_actual_births),
                                 f2k(sum_birth_shortfall),
                                 f2k(sum_actual_warborn_deaths_baseline),
                                 f2k(sum_actual_warborn_deaths),
                                 //
                                 EMPTY, EMPTY
        //
        );

        Util.out(s);

        if (ap.area == Area.RSFSR)
        {
            Util.out("");
            Util.out("Напомним, что оценка потерь РСФСР занижена из-за неучёта межреспубликанского миграционного притока в 1941-1945 годах");
        }
    }

    private void print(HalfYearEntry he) throws Exception
    {
        PopulationContext p1 = he.actual_population;
        PopulationContext p2 = he.next.actual_population;
        PopulationContext pavg = p1.avg(p2, ValueConstraint.NONE);

        /* к середине полугодия исполнится FROM/TO */
        int conscript_age_from = years2days(Constants.CONSCRIPT_AGE_FROM - 0.25);
        int conscript_age_to = years2days(Constants.CONSCRIPT_AGE_TO - 0.25);
        double exd_conscripts = he.actual_excess_wartime_deaths.sumDays(Gender.MALE, conscript_age_from, conscript_age_to);

        double cdr = 2 * PROMILLE * he.actual_deaths.sum() / pavg.sum();
        double cbr = 2 * PROMILLE * he.actual_births / pavg.sum();

        sum_actual_deaths += he.actual_deaths.sum();
        sum_actual_excess_wartime_deaths += he.actual_excess_wartime_deaths.sum();
        sum_exd_conscripts += exd_conscripts;
        sum_excess_warborn_deaths += he.actual_warborn_deaths - he.actual_warborn_deaths_baseline;
        sum_expected_nonwar_births += he.expected_nonwar_births;
        sum_actual_births += he.actual_births;
        sum_birth_shortfall += he.expected_nonwar_births - he.actual_births;
        sum_actual_warborn_deaths_baseline += he.actual_warborn_deaths_baseline;
        sum_actual_warborn_deaths += he.actual_warborn_deaths;

        String s = String.format("%d.%d %8s %8s %8s" + " %7s %7s %7s %7s" + " %7s %7s %8s %8s %7s" + " %5.1f %5.1f",
                                 he.year, he.halfyear.seq(1),
                                 f2k(p1.sum()),
                                 f2k(pavg.sum()),
                                 f2k(p2.sum()),
                                 //
                                 f2k(he.actual_deaths.sum()),
                                 f2k(he.actual_excess_wartime_deaths.sum()),
                                 f2k(exd_conscripts),
                                 f2k(he.actual_warborn_deaths - he.actual_warborn_deaths_baseline),
                                 //
                                 f2k(he.expected_nonwar_births),
                                 f2k(he.actual_births),
                                 f2k(he.expected_nonwar_births - he.actual_births),
                                 f2k(he.actual_warborn_deaths_baseline),
                                 f2k(he.actual_warborn_deaths),
                                 //
                                 cbr,
                                 cdr
        //
        );

        Util.out(s);
    }

    /* ======================================================================================== */

    private static String f2s(double v)
    {
        String s = String.format("%,15.0f", v);
        while (s.startsWith(" "))
            s = s.substring(1);
        return s;
    }

    private static String f2k(double v)
    {
        return f2s(v / 1000.0);
    }
}
