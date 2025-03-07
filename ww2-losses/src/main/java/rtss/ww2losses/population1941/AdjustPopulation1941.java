package rtss.ww2losses.population1941;

import java.util.ArrayList;
import java.util.List;

import rtss.data.bin.Bin;
import rtss.data.curves.InterpolatePopulationAsMeanPreservingCurve.InterpolationOptions;
import rtss.data.population.struct.Population;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.util.Util;
import rtss.util.plot.PopulationChart;
import rtss.ww2losses.population194x.AdjustPopulation;

/*
 * Ручная крупнозернстая (coarse) коррекция раскладки численности населения СССР/РСФСР в 1941 году внутри 5-летних групп.
 * Прилагаеся после автоматической дезагрегации 5-летних групп в 1-годовые значения. 
 * 
 * Разбивка по 5-летним группам не меняется, но значения для некоторых возрастов перераспределяются 
 * по годам внутри групп так, чтобы избежать артефакта отрицательной величины потерь в 1941-1945 гг.
 */
public class AdjustPopulation1941 extends AdjustPopulation
{
    private final Area area;

    public AdjustPopulation1941(Area area)
    {
        this.area = area;
    }

    @Override
    public String name()
    {
        return area.name();
    }

    @Override
    public Population adjust(Population p) throws Exception
    {
        Population p0 = p;
        List<Integer> isolateAgesMale = new ArrayList<>();
        List<Integer> isolateAgesFemale = new ArrayList<>();

        switch (area)
        {
        case USSR:
            p = p.clone();
            adjust_USSR(p, isolateAgesMale, isolateAgesFemale);
            break;

        case RSFSR:
            p = p.clone();
            adjust_RSFSR(p, isolateAgesMale, isolateAgesFemale);
            break;
        }

        if (Util.False)
        {
            new PopulationChart("Население " + area.toString() + " на начало 1941 года, этап 1")
                    .show("p0", p0)
                    .show("p", p)
                    .display();
            Util.noop();
        }

        Bin[] male = p.binSumByAge(Gender.MALE, rebin(ADH_binning, isolateAgesMale));
        Bin[] female = p.binSumByAge(Gender.FEMALE, rebin(ADH_binning, isolateAgesFemale));
        p = new Population(male,
                           female,
                           1941,
                           new InterpolationOptions().secondaryRefineYearlyAgesSmoothness(0.50),
                           new InterpolationOptions().secondaryRefineYearlyAgesSmoothness(0.50));

        if (Util.False)
        {
            new PopulationChart("Население " + area.toString() + " на начало 1941 года, этап 2")
                    .show("p0", p0)
                    .show("p", p)
                    .display();
            Util.noop();
        }

        Util.assertion(Util.same(p.sum(Gender.MALE), p0.sum(Gender.MALE)));
        Util.assertion(Util.same(p.sum(Gender.FEMALE), p0.sum(Gender.FEMALE)));
        verifyBinning(ADH_binning, p0, p);

        return p;
    }

    private void adjust_USSR(Population p, List<Integer> isolateAgesMale, List<Integer> isolateAgesFemale) throws Exception
    {
        // нельзя изменять значения 5-летних групповых корзин,
        // в частности нельзя перераспределять из 0..4 в 5+
        // или из 5+ в 0...4 

        if (Util.True)
        {
            redistribute_to(p, Gender.MALE, 3, 9_103 + 44_000, 2);
            redistribute_to(p, Gender.MALE, 4, 70_125 + 44_000, 2);
            isolateAgesMale.add(3);
            isolateAgesMale.add(4);
        }

        if (Util.True)
        {
            redistribute_to(p, Gender.MALE, 8, 14_037, 7);
            redistribute_to(p, Gender.MALE, 9, 24_651, 7);

            redistribute_to(p, Gender.MALE, 5, 40_955 + 10_000, 6, 7);

            isolateAgesMale.add(5);
            isolateAgesMale.add(6);
            isolateAgesMale.add(7);
            isolateAgesMale.add(8);
            isolateAgesMale.add(9);
        }

        /* ------------------------------------------------- */

        if (Util.True)
        {
            redistribute_to(p, Gender.FEMALE, 3, 37_471 + 24_000, 2, 1);
            redistribute_to(p, Gender.FEMALE, 4, 89_171 + 24_000, 2, 1);
            isolateAgesFemale.add(3);
            isolateAgesFemale.add(4);
        }

        if (Util.True)
        {
            // redistribute_to(p, Gender.FEMALE, 5, 50_734 + 10_000, 6, 7, 8, 9);
            redistribute_to(p, Gender.FEMALE, 5, 50_734 + 10_000, 7, 8, 9);

            isolateAgesFemale.add(5);
            isolateAgesFemale.add(6);
            isolateAgesFemale.add(7);
            isolateAgesFemale.add(8);
            isolateAgesFemale.add(9);
        }

        if (Util.True)
        {
            redistribute_to(p, Gender.FEMALE, 15, 5_069 + 500, 17, 18, 19);

            isolateAgesFemale.add(15);
            isolateAgesFemale.add(16);
        }

        p.makeBoth();
        p.recalcTotal();
    }

    private void adjust_RSFSR(Population p, List<Integer> isolateAgesMale, List<Integer> isolateAgesFemale) throws Exception
    {
        if (Util.True)
        {
            redistribute_to(p, Gender.FEMALE, 10, 2_691 + 600, 14);
            redistribute_to(p, Gender.FEMALE, 11, 8_224 + 300, 14);
            redistribute_to(p, Gender.FEMALE, 12, 5_427 + 300, 14);
            redistribute_to(p, Gender.FEMALE, 13, 1_934 + 200, 14);

            isolateAgesFemale.add(10);
            isolateAgesFemale.add(11);
            isolateAgesFemale.add(12);
            isolateAgesFemale.add(13);
        }

        p.makeBoth();
        p.recalcTotal();
    }
}
