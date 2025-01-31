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
        new PrintYears().do_print(halves);
    }

    private double sum_exd = 0;
    private double sum_births_shortfall = 0;

    public void do_print(HalfYearEntries<HalfYearEntry> halves) throws Exception
    {
        Util.out("");
        Util.out("Годовые величины:");
        Util.out("");
        Util.out("    р       = рождаемость, промилле");
        Util.out("    с       = смертность, промилле");
        Util.out("    н.нач   = численность населения в начале года, тыс. чел");
        Util.out("    н.сред  = среднегодовая численность населения, тыс. чел");
        Util.out("    н.кон   = численность населения в конце года, тыс. чел");
        Util.out("    с.изб   = число избыточных смертей за год, тыс. чел");
        Util.out("    р.нехв  = дефицит рождений в году, тыс. новорожденных");
        Util.out("");
        Util.out(" год     р    с     н.нач   н.сред    н.кон   с.изб  р.нехв");
        Util.out("=====  ==== =====  =======  =======  =======  ====== ======");

        for (HalfYearEntry he = halves.get("1941.1"); he.year != 1946; he = he.next.next)
            print(he, he.next);

        Util.out(String.format("%5s %5s %5s %8s %8s %8s" + " %7s %6s",
                               "всего", "", "", "", "", "",
                               f2k(sum_exd),
                               f2k(sum_births_shortfall)));
    }

    private void print(HalfYearEntry he1, HalfYearEntry he2) throws Exception
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

        Util.out(String.format("%5d %5.1f %5.1f %8s %8s %8s" + " %7s %6s",
                               he1.year, cbr, cdr,
                               f2k(p1.sum()),
                               f2k(pavg.sum()),
                               f2k(p2.sum()),
                               //
                               f2k(exd),
                               f2k(births_shortfall)
        //
        ));

        sum_exd += exd;
        sum_births_shortfall += births_shortfall;
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
