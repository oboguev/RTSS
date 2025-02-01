package rtss.ww2losses.helpers;

import rtss.data.ValueConstraint;
import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Gender;
import rtss.util.Util;
import rtss.ww2losses.HalfYearEntries;
import rtss.ww2losses.HalfYearEntry;
import rtss.ww2losses.HalfYearEntries.HalfYearSelector;

import static rtss.data.population.forward.ForwardPopulation.years2days;

/*
 * Проверка самосогласованности вычисленных значений
 */
public class VerifyHalfYears
{
    public VerifyHalfYears()
    {
    }
    
    private VerifyHalfYears(HalfYearEntries<HalfYearEntry> halves)
    {
        this.halves = halves;
    }

    private HalfYearEntries<HalfYearEntry> halves;

    public void verify(HalfYearEntries<HalfYearEntry> halves) throws Exception
    {
        Util.out("Верификация:");

        this.halves = halves;

        for (HalfYearEntry he : halves)
            verify(he);
    }

    public void verify(HalfYearEntry he) throws Exception
    {
        if (he.year == 1946)
            return;

        PopulationContext p1 = he.actual_population;
        PopulationContext p2 = he.next.actual_population;

        same(he.actual_deaths.sum(), he.actual_peace_deaths.sum() + he.actual_excess_wartime_deaths.sum());
        same(he.actual_deaths.sum(Gender.MALE), he.actual_peace_deaths.sum(Gender.MALE) + he.actual_excess_wartime_deaths.sum(Gender.MALE));
        same(he.actual_deaths.sum(Gender.FEMALE), he.actual_peace_deaths.sum(Gender.FEMALE) + he.actual_excess_wartime_deaths.sum(Gender.FEMALE));

        // ### проверка
        // ### PopulationContext px2 = p2.moveDown(0.5);
        // ### PopulationContext p = px2.sub(p1, ValueConstraint.NONE);
        // ### p = p.add(he.actual_deaths, ValueConstraint.NONE);
        // ### нули кроме 0-й строки, а 0-я строка равна ...

        double v1 = -1 * (p2.sum() - p1.sum());
        double v2 = -1 * (he.actual_births - he.actual_deaths.sum());

        out(String.format("%d.%d %8s %8s %8s = %s",
                          he.year, he.halfyear.seq(1),
                          f2k(v1), f2k(v2), f2k(v1 - v2), f2s(v1 - v2)));

        if (he.year == 1941 && he.halfyear == HalfYearSelector.FirstHalfYear)
        {
            // ###
        }
        else
        {
            // same(p2.sum() - p1.sum(), he.actual_births - he.actual_deaths.sum());
        }

        if (he.year == 1941 && he.halfyear == HalfYearSelector.SecondHalfYear)
        {
            /*
             * ### при продвижке на 2-е полугодие 1941 года
             * почему-то лишнее (2 тыс.) в 4 старших годах
             * 
             * при 1941.2 -> 1942.1 возрастают группы 96 97 98
             * 
             */
            verify_2();

            PopulationContext px2 = p2.moveDown(0.5);
            PopulationContext p = px2.sub(p1, ValueConstraint.NONE);
            p = p.add(he.actual_deaths, ValueConstraint.NONE);
            Util.noop();
        }
        // ###
    }

    private void same(double a, double b) throws Exception
    {
        Util.assertion(Util.same(a, b));
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

    /* ====================================================================== */

    private void verify_2() throws Exception
    {
        verify_2(95);
        verify_2(96);
        verify_2(97);
        verify_2(98);
    }

    private void verify_2(double age) throws Exception
    {
        Util.out("");
        Util.out(String.format("Проводка возраста %.1f", age));
        
        int nd1 = years2days(age);

        for (HalfYearEntry he : halves)
        {
            PopulationContext p = he.actual_population;
            
            int nd2 = nd1 + p.DAYS_PER_YEAR - 1;
            
            nd1 = Math.min(nd1, p.MAX_DAY);
            nd2 = Math.min(nd2, p.MAX_DAY);
            double v = p.sumDays(nd1, nd2);
            
            out(String.format("%d.%d %5.1f %f", he.year, he.halfyear.seq(1), age, v));
            
            age += 0.5;
            nd1 += years2days(0.5);
        }
    }
    
    /* ====================================================================== */
    
    private static boolean enabled = false; // ###

    public static void catch_bug_1_enable() throws Exception
    {
        enabled = true; // ###
    }
    
    public static void catch_bug_1(HalfYearEntries<HalfYearEntry> halves) throws Exception
    {
        if (!enabled)
            return;

        VerifyHalfYears vfy = new VerifyHalfYears(halves);
        vfy.cb(96);
        vfy.cb(97);
        vfy.cb(98);
        // Util.err("NO BUG YET");
    }
    
    private void cb(double age) throws Exception
    {
        HalfYearEntry he1 = halves.get("1941.2");
        HalfYearEntry he2 = halves.get("1942.1");
        
        double age1 = age + he1.offset_start1941();
        double age2 = age + he2.offset_start1941();
                                       
        double v1 = get_for_age(he1.actual_population, age1);
        double v2 = get_for_age(he2.actual_population, age2);
        
        if (v2 > v1)
            throw new Exception("BUG!!!");
    }
    
    private double get_for_age(PopulationContext p, double age) throws Exception
    {
        int nd1 = years2days(age);
        int nd2 = nd1 + p.DAYS_PER_YEAR - 1;
        nd1 = Math.min(nd1, p.MAX_DAY);
        nd2 = Math.min(nd2, p.MAX_DAY);
        return p.sumDays(nd1, nd2);
    }
}
