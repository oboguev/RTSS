package rtss.ww2losses.ageline;

import rtss.data.DoubleArray;
import rtss.data.ValueConstraint;
import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.plot.PopulationChart;

/*
 * Хранит отображение (Gender, nd_age) -> loss intensity или immigration intensity
 */
public class AgeLineFactorIntensities
{
    private static final int NYEARS = Population.MAX_AGE + 1;
    private static final int DAYS_PER_YEAR = 365;
    // private static final int MAX_YEAR = NYEARS - 1;
    private static final int NDAYS = NYEARS * DAYS_PER_YEAR;
    private static final int MAX_DAY = NDAYS - 1;

    private DoubleArray male = newDoubleArray(ValueConstraint.NONE);
    private DoubleArray female = newDoubleArray(ValueConstraint.NONE);

    private DoubleArray newDoubleArray(ValueConstraint vc)
    {
        return new DoubleArray(MAX_DAY, vc);
    }
    
    private DoubleArray forGender(Gender gender)
    {
        switch (gender)
        {
        case MALE:
            return male;
            
        case FEMALE:
            return female;
            
        default:
            return null;
        }
    }

    public Double get(Gender gender, int nd) throws Exception
    {
        return forGender(gender).getNullable(nd);
    }

    public void set(Gender gender, int nd, double v) throws Exception
    {
        forGender(gender).set(nd, v);
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

    public PopulationContext toPopulationContext() throws Exception
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
