package rtss.ww2losses.population_194x;

import rtss.data.bin.Bin;
import rtss.data.population.struct.Population;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.util.Util;
import rtss.util.plot.PopulationChart;

public class AdjustPopulation1941 extends AdjustPopulation
{
    private static final int[] ADH_binning = { 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 16 };
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

        switch (area)
        {
        case USSR:
            p = p.clone();
            adjust_USSR(p);
            break;

        case RSFSR:
            p = p.clone();
            adjust_RSFSR(p);
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

    private void adjust_USSR(Population p) throws Exception
    {
        // нельзя изменять значения 5-летних групповых корзин,
        // в частности нельзя перераспределять из 0..4 в 5+
        // или из 5+ в 0...4 

        redistribute_to(p, Gender.MALE, 3, 9_103 + 44_000, 2);
        redistribute_to(p, Gender.MALE, 4, 70_125 + 44_000, 2);

        // redistribute_to(p, Gender.MALE, 5, 1_327, 6);

        redistribute_to(p, Gender.FEMALE, 3, 37_471 + 24_000, 2, 1);
        redistribute_to(p, Gender.FEMALE, 4, 89_171 + 24_000, 2, 1);

        // redistribute_to(p, Gender.FEMALE, 5, 13_434, 6);

        p.makeBoth();
        p.recalcTotal();
    }

    private void adjust_RSFSR(Population p) throws Exception
    {
        // ###
    }

    /* ================================================= */

    /*
     * Перераспределить @amount человек в возраст to_age из возрастов from_ages
     */
    private void redistribute_to(Population p, Gender gender, int to_age, double amount, int... from_ages) throws Exception
    {
        p.add(gender, to_age, amount);

        // распределить перенос из "from" пропорционально численности в возрастах from_ages
        double from_sum = 0;
        for (int age : from_ages)
            from_sum += p.get(gender, age);

        for (int age : from_ages)
        {
            double v = p.get(gender, age);
            double share = amount * (v / from_sum);
            if (share > v)
                throw new Exception("невозможно перераспределить");
            p.sub(gender, age, share);
        }
    }

    private void verifyBinning(final int[] binning, Population p1, Population p2) throws Exception
    {
        verifyBinning(Gender.MALE, binning, p1, p2);
        verifyBinning(Gender.FEMALE, binning, p1, p2);
        verifyBinning(Gender.BOTH, binning, p1, p2);
    }

    private void verifyBinning(Gender gender, final int[] binning, Population p1, Population p2) throws Exception
    {
        Bin[] b1 = p1.binSumByAge(gender, binning);
        Bin[] b2 = p2.binSumByAge(gender, binning);
        Util.assertion(b1.length == b2.length);
        for (int k = 0; k < b1.length; k++)
        {
            if (!Util.same(b1[k].avg, b2[k].avg))
                throw new Exception("расклад по возрастным корзинам изменился");
        }
    }
}
