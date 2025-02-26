package rtss.ww2losses.ageline;

import rtss.data.DoubleArray;
import rtss.data.ValueConstraint;
import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;
import rtss.util.plot.PopulationChart;

import static rtss.data.population.projection.ForwardPopulation.years2days;

import java.util.ArrayList;
import java.util.List;

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
        Double v = forGender(gender).getNullable(nd);
        if (v == null)
            v = 0.0;
        return v;
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
                .show("loss intensity", toPopulationContext())
                .display();

    }

    /* ========================================================== */

    /*
     * Проложить интерполирующую линию от @age1 до @age2.
     * Намерение -- устанить отрицательные значения в диапазоне [age1 ... age2],
     * проведя линию между двумя положительными точками.  
     */
    public void unnegInterpolateMidYears(Gender gender, int age1, int age2) throws Exception
    {
        Util.assertion(age1 >= 0 && age2 >= 0 && age1 < age2);

        unnegInterpolateYears(gender, age1 + 0.5, age2 + 0.5);
    }

    private void check_unneg(Gender gender, double age1, double age2) throws Exception
    {
        Double[] a = forGender(gender).get();

        int nd1 = years2days(age1);
        int nd2 = years2days(age2);
        Util.assertion(nd1 >= 0 && nd2 >= 0 && nd1 <= nd2);

        for (int nd = nd1; nd <= nd2; nd++)
        {
            if (a[nd] == null || a[nd] <= 0)
                throw new IllegalArgumentException("doubtful unneg pivot points");
        }
    }

    public void unnegInterpolateYears(Gender gender, double age1, double age2) throws Exception
    {
        Util.assertion(age1 >= 0 && age2 >= 0 && age1 < age2);

        check_unneg(gender, Math.floor(age1), age1);
        check_unneg(gender, age2, Math.ceil(age2));

        int nd1 = years2days(age1);
        int nd2 = years2days(age2);
        unnegInterpolateDays(gender, nd1, nd2);
    }

    public void unnegInterpolateDays(Gender gender, int nd1, int nd2) throws Exception
    {
        Util.assertion(nd1 >= 0 && nd2 >= 0 && nd1 < nd2);

        Double[] a = forGender(gender).get();
        double a1 = a[nd1];
        double a2 = a[nd2];

        if (a1 <= 0 || a2 <= 0)
            throw new IllegalArgumentException("doubtful unneg pivot points");

        for (int nd = nd1 + 1; nd < nd2; nd++)
            a[nd] = a1 + (nd - nd1) * (a2 - a1) / (nd2 - nd1);
    }

    /* ========================================================== */

    public String dumpYear(Gender gender, int year) throws Exception
    {
        Double[] a = forGender(gender).get();

        int nd = year * DAYS_PER_YEAR;

        StringBuilder sb = new StringBuilder();

        for (int k = 0; k < DAYS_PER_YEAR; k++)
        {
            if (a[nd + k] == null)
                sb.append(String.format("%-3d null", k));
            else
                sb.append(String.format("%-3d %f", k, a[nd + k]));
            sb.append(Util.nl);
        }

        return sb.toString();
    }

    /* ========================================================== */

    /*
     * Locate and display regions with negative intensities
     */
    public static class Region
    {
        public int nd1;
        public int nd2;
    }

    public List<Region> negRegions(Gender gender) throws Exception
    {
        List<Region> list = new ArrayList<>();

        double[] a = forGender(gender).asUnboxedArray(0.0);

        for (int nd = 0; nd < a.length;)
        {
            // find first negative point
            while (nd < a.length && a[nd] >= 0)
                nd++;
            if (nd == a.length)
                break;

            Region r = new Region();
            r.nd1 = nd;

            // find first positive point
            while (nd < a.length && a[nd] < 0)
                nd++;
            r.nd2 = nd - 1;
            list.add(r);
        }

        return list;
    }

    public String dumpNegRegions(Gender gender) throws Exception
    {
        List<Region> list = negRegions(gender);
        if (list.size() == 0)
            return NoNegativeRegions;

        StringBuilder sb = new StringBuilder();

        for (Region r : list)
        {
            sb.append(String.format("%5d - %5d  [%5d] =  %7.3f - %7.3f  = %7.3f - %7.3f",
                                    r.nd1, r.nd2, r.nd2 - r.nd1 + 1,
                                    day2year(r.nd1), day2year(r.nd2),
                                    day2year(r.nd1) - 0.5, day2year(r.nd2) - 0.5));
            sb.append(Util.nl);
        }

        return sb.toString();
    }

    private double day2year(int nd)
    {
        return nd / 365.0;
    }
    
    public boolean hasNegativeRegions(String regs)
    {
        return !regs.equals(NoNegativeRegions);
    }
    
    private final String NoNegativeRegions = "No negative regions"; 
}
