package rtss.ww2losses.helpers;

import rtss.data.ValueConstraint;
import rtss.data.population.struct.PopulationContext;
import rtss.util.Util;
import rtss.ww2losses.HalfYearEntries;
import rtss.ww2losses.HalfYearEntry;

public class PrintYears
{
    private static double PROMILLE = 1000.0;

    public static void print(HalfYearEntries<HalfYearEntry> halves) throws Exception
    {
        Util.out("");
        Util.out("Годовые величины");
        Util.out("");
        Util.out("    р       = рождаемость, промилле");
        Util.out("    с       = смертность, промилле");
        Util.out("    н.нач   = население в начале года, тыс. чел");
        Util.out("    н.сред  = среднегодовое население, тыс. чел");
        Util.out("    н.кон   = население в конце года, тыс. чел");
        Util.out("    изб.ум  = число избыточных смертей, тыс. чел");
        Util.out("    н.рожд  = дефицит рождений в году, тыс. новорожденных");
        Util.out("");
        Util.out("год     р    с     н.нач   н.сред    н.кон   изб.ум н.рожд");
        Util.out("====  ==== =====  =======  =======  =======  ====== ======");

        for (HalfYearEntry he = halves.get("1941.1"); he.year != 1946; he = he.next.next)
            print(he, he.next);
    }

    private static void print(HalfYearEntry he1, HalfYearEntry he2) throws Exception
    {
        PopulationContext p1 = he1.actual_population;
        PopulationContext p2 = he2.next.actual_population;

        PopulationContext pavg = p1.avg(p2, ValueConstraint.NONE);

        double d1 = he1.actual_deaths.sum();
        double d2 = he2.actual_deaths.sum();
        double cdr = PROMILLE * (d1 + d2) / pavg.sum();

        double b1 = he1.actual_births;
        double b2 = he2.actual_births;
        double cbr = PROMILLE * (b1 + b2) / pavg.sum();

        double exd = he1.actual_excess_wartime_deaths.sum() + he2.actual_excess_wartime_deaths.sum();

        double births_shortfall = (he1.expected_nonwar_births - he1.actual_births) + (he2.expected_nonwar_births - he2.actual_births);

        Util.out(String.format("%d %5.1f %5.1f %8s %8s %8s %7s %6s",
                               he1.year, cbr, cdr,
                               f2k(p1.sum() / 1000.0),
                               f2k(pavg.sum() / 1000.0),
                               f2k(p2.sum() / 1000.0),
                               f2k(exd / 1000.0),
                               f2k(births_shortfall / 1000.0)));

    }

    /* ======================================================================================== */

    private static String f2k(double v)
    {
        String s = String.format("%,15.0f", v);
        while (s.startsWith(" "))
            s = s.substring(1);
        return s;
    }
}
