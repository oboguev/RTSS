package rtss.ww2losses.helpers;

import rtss.data.ValueConstraint;
import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.util.Util;
import rtss.ww2losses.HalfYearEntries;
import rtss.ww2losses.HalfYearEntry;
import rtss.ww2losses.params.AreaParameters;

import static rtss.data.population.forward.ForwardPopulation.years2days;

/*
 * Проверка самосогласованности вычисленных значений
 */
public class VerifyHalfYears
{
    private final HalfYearEntries<HalfYearEntry> halves;
    private final AreaParameters ap;

    private static final int MAX_AGE  = Population.MAX_AGE;

    public VerifyHalfYears(AreaParameters ap, HalfYearEntries<HalfYearEntry> halves)
    {
        this.ap = ap;
        this.halves = halves;
    }

    public void verify(boolean print) throws Exception
    {
        if (print)
            Util.out("Верификация:");

        for (HalfYearEntry he : halves)
            verify(he, print);
    }

    public void verify(HalfYearEntry he, boolean print) throws Exception
    {
        if (he.year == 1946)
            return;

        PopulationContext p1 = he.actual_population;
        PopulationContext p2 = he.next.actual_population;
        int ndays = years2days(0.5);

        if (Util.True)
        {
            same(he.actual_deaths.sum(), he.actual_peace_deaths.sum() + he.actual_excess_wartime_deaths.sum());
            same(he.actual_deaths.sum(Gender.MALE), he.actual_peace_deaths.sum(Gender.MALE) + he.actual_excess_wartime_deaths.sum(Gender.MALE));
            same(he.actual_deaths.sum(Gender.FEMALE), he.actual_peace_deaths.sum(Gender.FEMALE) + he.actual_excess_wartime_deaths.sum(Gender.FEMALE));

            same(he.actual_peace_deaths_from_newborn.sum(), he.actual_warborn_deaths_baseline);
        }

        if (print)
        {
            double v1 = -1 * (p2.sum() - p1.sum());
            double v2 = -1 * (he.actual_births - he.actual_deaths.sum());

            out(String.format("%d.%d %8s %8s %8s = %s",
                              he.year, he.halfyear.seq(1),
                              f2k(v1), f2k(v2), f2k(v1 - v2), f2s(v1 - v2)));
        }

        if (Util.True)
        {
            double v1 = -1 * (p2.sum() - p1.sum());
            double v2 = -1 * (he.actual_births - he.actual_deaths.sum());
            double abs_dv = Math.abs(v1 - v2);

            if (he.index().equals("1941.1") && ap.area == Area.USSR)
                Util.assertion(abs_dv < 500);
            else
                Util.assertion(abs_dv < 10);
        }

        if (Util.True)
        {
            PopulationContext p = p1.sub(he.actual_deaths, ValueConstraint.NONE);
            p = p2.moveDown(0.5).sub(p, ValueConstraint.NONE);

            double v = p.sumDays(ndays, p.MAX_DAY);
            Util.assertion(Math.abs(v) < 10);
        }

        if (Util.True)
        {
            PopulationContext p = p1.sub(he.actual_deaths, ValueConstraint.NONE);
            p = p2.sub(p.moveUp(0.5), ValueConstraint.NONE);
            
            // округления
            Util.assertion(Math.abs(p.sum(1, MAX_AGE - 1)) < 10);
            
            // накопление возрастов более MAX_AGE, но не должно становиться очень большим 
            Util.assertion(Math.abs(p.getYearValue(Gender.BOTH, MAX_AGE)) < 1000);
            
            if (he.index().equals("1941.1"))
            {
                same(p.getYearValue(Gender.BOTH, 0), he.actual_births, 0.0002);
                same(p.sumDays(0, ndays), he.actual_births, 0.0002);
            }
            else
            {
                same(p.getYearValue(Gender.BOTH, 0), he.actual_births);
                same(p.sumDays(0, ndays), he.actual_births);
            }
        }
    }

    private void same(double a, double b) throws Exception
    {
        Util.assertion(Util.same(a, b));
    }

    private void same(double a, double b, double diff) throws Exception
    {
        Util.assertion(Util.same(a, b, diff));
    }

    /* ====================================================================== */

    private void out(String what)
    {
        Util.out(what);
    }

    @SuppressWarnings("unused")
    private void outk(String what, double v)
    {
        out(what + ": " + f2k(v));
    }

    private String f2k(double v)
    {
        return f2s(v / 1000.0);
    }

    private String f2s(double v)
    {
        String s = String.format("%,15.0f", v);
        while (s.startsWith(" "))
            s = s.substring(1);
        return s;
    }
}
