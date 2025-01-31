package rtss.ww2losses.population_194x;

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

        return p;
    }

    private void adjust_USSR(Population p) throws Exception
    {
        // нельзя перераспределять из 0..4 в 5+
        // или из 5+ в 0...4 
        
        redistribute(p, Gender.MALE, 3, 9_103, 2);
        redistribute(p, Gender.MALE, 4, 70_125, 2);
        redistribute(p, Gender.MALE, 5, 1_327, 6);

        redistribute(p, Gender.FEMALE, 3, 37_471, 2);
        redistribute(p, Gender.FEMALE, 4, 89_171, 2);
        redistribute(p, Gender.FEMALE, 5, 13_434, 6);
        
        p.makeBoth();
        p.recalcTotal();
    }

    private void adjust_RSFSR(Population p) throws Exception
    {
        // ###
    }
    
    /* ================================================= */
    
    private void redistribute(Population p, Gender gender, int from_age, double amount, int ... to_ages) throws Exception
    {
        p.sub(gender, from_age, amount);
        
        for (int age : to_ages)
            p.add(gender, age, amount / to_ages.length);
    }
}
