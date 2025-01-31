package rtss.ww2losses.population_194x;

import java.util.ArrayList;
import java.util.List;

import rtss.data.bin.Bin;
import rtss.data.population.struct.Population;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.util.Util;
import rtss.util.plot.PopulationChart;

public class AdjustPopulation1941 extends AdjustPopulation
{
    private static final int[] ADH_refined_widths = { 1, 1, 1, 1, 1, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 16 };
    private final Area area;

    public AdjustPopulation1941(Area area)
    {
        this.area = area;
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

        if (Util.True && area == Area.USSR)
        {
            new PopulationChart("Население " + area.toString() + " на начало 1941 года, этап 1")
                    .show("p0", p0)
                    .show("p", p)
                    .display();
            Util.noop();
        }

        Bin[] male = p.binSumByAge(Gender.MALE, ADH_refined_widths);
        Bin[] female = p.binSumByAge(Gender.FEMALE, ADH_refined_widths);
        p = new Population(male, female);

        if (Util.True && area == Area.USSR)
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

        redistribute_to(p, Gender.MALE, 3, 9_103 + 44_000, 2);
        redistribute_to(p, Gender.MALE, 4, 70_125 + 44_000, 2);
        isolateAgesMale.add(3);
        isolateAgesMale.add(4);

        // redistribute_to(p, Gender.MALE, 5, 1_327, 6);

        redistribute_to(p, Gender.FEMALE, 3, 37_471 + 24_000, 2, 1);
        redistribute_to(p, Gender.FEMALE, 4, 89_171 + 24_000, 2, 1);
        isolateAgesFemale.add(3);
        isolateAgesFemale.add(4);

        // redistribute_to(p, Gender.FEMALE, 5, 13_434, 6);

        p.makeBoth();
        p.recalcTotal();
    }

    private void adjust_RSFSR(Population p, List<Integer> isolateAgesMale, List<Integer> isolateAgesFemale) throws Exception
    {
        // ###
    }
}
