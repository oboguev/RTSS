package rtss.ww2losses.helpers;

import static rtss.data.population.forward.ForwardPopulation.years2days;

import rtss.data.ValueConstraint;
import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Gender;
import rtss.util.Util;
import rtss.ww2losses.Constants;
import rtss.ww2losses.HalfYearEntries;
import rtss.ww2losses.HalfYearEntry;

public class PrintHalfYears
{
    public static void print(HalfYearEntries<HalfYearEntry> halves) throws Exception
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
        Util.out("    с.нов   = число избыточных смертей за полугодие среди родившихся после середины 1941 года, тыс. чел");
        Util.out("");
        Util.out("    р.ожид    = ожидаемое число рождений в условиях мира (за полугодие)");
        Util.out("    р.факт    = фактическое число рождений (за полугодие)");
        Util.out("    р.нехв    = дефицит рождений за полугодие, тыс. новорожденных");
        Util.out("    фcр.мир   = число смертей (в данном полугодии) от фактических рождений во время войны, ожидаемое при смертности мирного времени");
        Util.out("    фcр       = фактическое число смертей (в данном полугодии) от фактических рождений во время войны, при фактической военной смертности");
        Util.out("");
        Util.out("    р         = рождаемость (промилле, для полугодия, но в нормировке на год");
        Util.out("    с         = смертность (промилле, для полугодия, но в нормировке на год)");
        Util.out("");

        for (HalfYearEntry he : halves)
        {
            if (he.year != 1946)
                print(he);
        }
    }

    private static void print(HalfYearEntry he) throws Exception
    {
        PopulationContext p1 = he.actual_population;
        PopulationContext p2 = he.next.actual_population;
        PopulationContext pavg = p1.avg(p2, ValueConstraint.NONE);

        /* к середине полугодия исполнится FROM/TO */
        int conscript_age_from = years2days(Constants.CONSCRIPT_AGE_FROM - 0.25);
        int conscript_age_to = years2days(Constants.CONSCRIPT_AGE_TO - 0.25);
        double exd_conscripts = he.actual_excess_wartime_deaths.sumDays(Gender.MALE, conscript_age_from, conscript_age_to);

        String s = String.format("%d.%d %8s %8s %8s" + " %7s %7s %7s %7s",
                                 he.year, he.halfyear.seq(1),
                                 f2k(p1.sum()),
                                 f2k(pavg.sum()),
                                 f2k(p2.sum()),
                                 //
                                 f2k(he.actual_deaths.sum()),
                                 f2k(he.actual_excess_wartime_deaths.sum()),
                                 f2k(exd_conscripts),
                                 f2k((he.actual_warborn_deaths - he.actual_warborn_deaths_baseline))
        //
        );

        Util.out(s);
        // ###
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
