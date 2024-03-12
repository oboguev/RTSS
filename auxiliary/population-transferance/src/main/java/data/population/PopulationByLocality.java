package data.population;

import data.selectors.Gender;
import data.selectors.Locality;
import my.Util;

public class PopulationByLocality
{
    public static final int MAX_AGE = Population.MAX_AGE;

    private Population rural;
    private Population total;
    private Population urban;

    private void reinit()
    {
        rural = total = urban = null;
    }

    static public PopulationByLocality newPopulationByLocality()
    {
        PopulationByLocality p = new PopulationByLocality();
        p.reinit();
        p.rural = Population.newPopulation();
        p.urban = Population.newPopulation();
        p.total = Population.newPopulation();
        return p;
    }

    public PopulationByLocality clone()
    {
        PopulationByLocality p = new PopulationByLocality();

        if (rural != null)
            p.rural = rural.clone();
        if (urban != null)
            p.urban = urban.clone();
        if (total != null)
            p.total = total.clone();

        return p;
    }

    /****************************************************************************************************/

    public double get(Locality locality, Gender gender, int age) throws Exception
    {
        return forLocality(locality).get(gender, age);
    }

    public double sum(Locality locality, Gender gender, int age1, int age2) throws Exception
    {
        return forLocality(locality).sum(gender, age1, age2);
    }

    public void set(Locality locality, Gender gender, int age, double value) throws Exception
    {
        forLocality(locality).set(gender, age, value);
    }

    public Population forLocality(Locality locality)
    {
        switch (locality)
        {
        case RURAL:
            return rural;
        case URBAN:
            return urban;
        case TOTAL:
            return total;
        default:
            return null;
        }
    }

    public void resetUnknown() throws Exception
    {
        if (rural != null)
            rural.resetUnknown();
        if (urban != null)
            urban.resetUnknown();
        if (total != null)
            total.resetUnknown();
    }

    public void resetTotal() throws Exception
    {
        if (rural != null)
            rural.resetTotal();
        if (urban != null)
            urban.resetTotal();
        if (total != null)
            total.resetTotal();
    }

    public void makeBoth(Locality locality) throws Exception
    {
        forLocality(locality).makeBoth();
    }

    /****************************************************************************************************/

    public static PopulationByLocality load(String path) throws Exception
    {
        PopulationByLocality p = new PopulationByLocality();
        p.do_load(path);
        return p;
    }

    private void do_load(String path) throws Exception
    {
        reinit();

        rural = load(path, Locality.RURAL);
        urban = load(path, Locality.URBAN);
        if (haveFile(path, Locality.TOTAL))
        {
            total = load(path, Locality.TOTAL);
        }
        else
        {
            total = calcTotal(rural, urban);
        }

        validate();
    }

    public void recalcTotal() throws Exception
    {
        total = calcTotal(rural, urban);
    }

    private Population load(String path, Locality locality) throws Exception
    {
        return Population.load(filePath(path, locality), locality);
    }

    private String filePath(String path, Locality locality)
    {
        return String.format("%s/%s.txt", path, locality.toString());
    }

    private boolean haveFile(String path, Locality locality)
    {
        return null != Util.class.getClassLoader().getResource(filePath(path, locality));
    }

    public void validate() throws Exception
    {
        rural.validate();
        urban.validate();
        total.validate();

        for (int age = 0; age <= MAX_AGE; age++)
        {
            if (differ(rural.male(age) + urban.male(age), total.male(age)))
                mismatch();

            if (differ(rural.female(age) + urban.female(age), total.female(age)))
                mismatch();

            if (differ(rural.fm(age) + urban.fm(age), total.fm(age)))
                mismatch();
        }
    }

    static private Population calcTotal(Population rural, Population urban) throws Exception
    {
        Population total = Population.newPopulation();

        for (int age = 0; age <= MAX_AGE; age++)
        {
            total.male.put(age, rural.male.get(age) + urban.male.get(age));
            total.female.put(age, rural.female.get(age) + urban.female.get(age));
            total.both.put(age, rural.both.get(age) + urban.both.get(age));
        }

        total.male_total = rural.male_total + urban.male_total;
        total.female_total = rural.female_total + urban.female_total;
        total.both_total = rural.both_total + urban.both_total;

        total.male_unknown = rural.male_unknown + urban.male_unknown;
        total.female_unknown = rural.female_unknown + urban.female_unknown;
        total.both_unknown = rural.both_unknown + urban.both_unknown;

        total.validate();

        return total;
    }

    /****************************************************************************************************/

    private void mismatch() throws Exception
    {
        throw new Exception("Mismatching data in population table");
    }

    private boolean differ(double a, double b)
    {
        return differ(a, b, 0.00001);
    }

    private boolean differ(double a, double b, double diff)
    {
        return Math.abs(a - b) / Math.max(Math.abs(a), Math.abs(b)) > diff;
    }
}
