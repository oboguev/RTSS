package rtss.ww2losses.ageline;

import java.util.HashMap;
import java.util.Map;

import rtss.data.ValueConstraint;
import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.plot.PopulationChart;

/*
 * Хранит отображение (Gender, nd_age) -> loss intensity
 */
public class AgeLineLossIntensities
{
    private Map<String, Double> m = new HashMap<>();

    private String key(Gender gender, int nd)
    {
        return gender.name() + "." + nd;
    }

    public Double get(Gender gender, int nd)
    {
        return m.get(key(gender, nd));
    }

    public void set(Gender gender, int nd, double v)
    {
        m.put(key(gender, nd), v);
    }

    public double average(Gender gender, int nd1, int nd2) throws Exception
    {
        int count = 0;
        int sum = 0;

        for (int nd = nd1; nd <= nd2; nd++)
        {
            sum += get(gender, nd);
            count++;
        }

        if (count == 0)
            throw new IllegalArgumentException();

        return sum / count;
    }

    /* ========================================================== */

    private PopulationContext toPopulationContext() throws Exception
    {
        PopulationContext p = PopulationContext.newTotalPopulationContext(ValueConstraint.NONE);

        for (Gender gender : Gender.TwoGenders)
        {
            for (int nd = 0; nd <= p.MAX_DAY; nd++)
            {
                Double v = get(gender, nd);
                if (v == null)
                    v = 0.0;
                p.setDay(Locality.TOTAL, gender, nd, v);
            }
        }

        return p;
    }

    public void display(String title) throws Exception
    {
        new PopulationChart(title)
                .show("loss intensity", toPopulationContext().toPopulation())
                .display();

    }
}
