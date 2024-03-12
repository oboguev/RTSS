package data.population;

import java.util.HashMap;
import java.util.Map;

import data.selectors.Gender;
import data.selectors.Locality;
import my.Util;

public class Population
{
    public static final int MAX_AGE = 100;

    private void reinit()
    {
        rural = total = urban = null;
        male = female = both = null;
        locality = null;
    }

    static public Population newSinglePopulation()
    {
        Population p = new Population();
        p.reinit();
        p.male = new HashMap<>();
        p.female = new HashMap<>();
        p.both = new HashMap<>();
        return p;
    }

    static public Population newCombinedPopulation()
    {
        Population p = new Population();
        p.reinit();
        p.rural = newSinglePopulation();
        p.urban = newSinglePopulation();
        p.total = newSinglePopulation();
        return p;
    }

    public Population clone()
    {
        Population p = new Population();

        p.locality = locality;
        p.male_unknown = male_unknown;
        p.male_total = male_total;
        p.female_unknown = female_unknown;
        p.female_total = female_total;
        p.both_unknown = both_unknown;
        p.both_total = both_total;

        if (male != null)
            p.male = new HashMap<>(male);
        if (female != null)
            p.female = new HashMap<>(female);
        if (both != null)
            p.both = new HashMap<>(both);

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

    public double get(Gender gender, int age) throws Exception
    {
        Map<Integer, Double> m = forGender(gender);
        if (!m.containsKey(age))
            throw new Exception("Missing data for age " + age);
        return m.get(age);
    }

    public double sum(Gender gender, int age1, int age2) throws Exception
    {
        Map<Integer, Double> m = forGender(gender);

        double sum = 0;

        for (int age = age1; age <= age2; age++)
        {
            if (!m.containsKey(age))
                throw new Exception("Missing data for age " + age);
            sum += m.get(age);
        }

        return sum;
    }

    public void set(Gender gender, int age, double value) throws Exception
    {
        Map<Integer, Double> m = forGender(gender);
        m.put(age, value);
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

    private Map<Integer, Double> forGender(Gender gender)
    {
        switch (gender)
        {
        case MALE:
            return male;
        case FEMALE:
            return female;
        case BOTH:
            return both;
        default:
            return null;
        }
    }

    public void recalcTotal() throws Exception
    {
        total = new Population();
        total.makeSingleTotal(rural, urban);
    }

    public void resetUnknown() throws Exception
    {
        if (rural != null)
            rural.resetUnknown();
        if (urban != null)
            urban.resetUnknown();
        if (total != null)
            total.resetUnknown();

        male_unknown = 0;
        female_unknown = 0;
        both_unknown = 0;
    }

    public void resetTotal() throws Exception
    {
        if (rural != null)
            rural.resetTotal();
        if (urban != null)
            urban.resetTotal();
        if (total != null)
            total.resetTotal();

        male_total = 0;
        female_total = 0;
        both_total = 0;

        if (male != null)
            male_total = this.sum(Gender.MALE, 0, MAX_AGE);
        if (female != null)
            female_total = this.sum(Gender.FEMALE, 0, MAX_AGE);
        if (both != null)
            both_total = this.sum(Gender.BOTH, 0, MAX_AGE);
    }

    /****************************************************************************************************/

    private Population rural;
    private Population total;
    private Population urban;

    public void loadCombined(String path) throws Exception
    {
        reinit();

        rural = loadCombined(path, Locality.RURAL);
        urban = loadCombined(path, Locality.URBAN);
        if (haveFile(path, Locality.TOTAL))
        {
            total = loadCombined(path, Locality.TOTAL);
        }
        else
        {
            recalcTotal();
        }

        validateCombined();
    }

    public Population loadCombined(String path, Locality locality) throws Exception
    {
        Population p = new Population();
        p.locality = locality;
        p.loadSingle(combinedFilePath(path, locality));
        return p;
    }

    private String combinedFilePath(String path, Locality locality)
    {
        return String.format("%s/%s.txt", path, locality.toString());
    }

    private boolean haveFile(String path, Locality locality)
    {
        return null != Util.class.getClassLoader().getResource(combinedFilePath(path, locality));
    }

    public void validateCombined() throws Exception
    {
        rural.validateSingle();
        urban.validateSingle();
        total.validateSingle();

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

    /****************************************************************************************************/

    public double male(int age) throws Exception
    {
        if (!male.containsKey(age))
            throw new Exception("Missing data");
        return male.get(age);
    }

    public double female(int age) throws Exception
    {
        if (!female.containsKey(age))
            throw new Exception("Missing data");
        return female.get(age);
    }

    public double fm(int age) throws Exception
    {
        return both(age);
    }

    public double both(int age) throws Exception
    {
        if (!both.containsKey(age))
            throw new Exception("Missing data");
        return both.get(age);
    }

    private Map<Integer, Double> male;
    private Map<Integer, Double> female;
    private Map<Integer, Double> both;
    private Locality locality;

    private double male_unknown = 0;
    private double male_total = 0;
    private double female_unknown = 0;
    private double female_total = 0;
    private double both_unknown = 0;
    private double both_total = 0;

    public void loadSingle(String path) throws Exception
    {
        reinit();

        male = new HashMap<>();
        female = new HashMap<>();
        both = new HashMap<>();

        String rdata = Util.loadResource(path);
        rdata = rdata.replace("\r\n", "\n");
        for (String line : rdata.split("\n"))
        {
            char unicode_feff = '\uFEFF';
            line = line.replace("" + unicode_feff, "");

            int k = line.indexOf('#');
            if (k != -1)
                line = line.substring(0, k);
            line = line.replace("\t", " ").replaceAll(" +", " ").trim();
            if (line.length() == 0)
                continue;

            String[] el = line.split(" ");
            if (el.length != 3 && el.length != 4)
                throw new Exception("Invalid format of population table");

            String age = el[0];
            if (age.contains("Итого") || age.contains("-"))
                continue;
            if (age.equals("" + MAX_AGE + "+"))
                age = "" + MAX_AGE;

            int m = asInt(el[1]);
            int f = asInt(el[2]);
            int b;

            if (el.length == 4)
                b = asInt(el[3]);
            else
                b = m + f;

            if (age.equals("unknown"))
            {
                male_unknown = m;
                female_unknown = f;
                both_unknown = b;
            }
            else if (age.equals("total"))
            {
                male_total = m;
                female_total = f;
                both_total = b;
            }
            else
            {
                int a = asInt(age);
                if (a < 0 || a > MAX_AGE)
                    throw new Exception("Invalid value in population table");

                if (male.containsKey(a))
                    throw new Exception("Duplicate value in population table");

                male.put(a, (double) m);
                female.put(a, (double) f);
                both.put(a, (double) b);
            }
        }

        validateSingle();
    }

    private void makeSingleTotal(Population rural, Population urban) throws Exception
    {
        reinit();

        male = new HashMap<>();
        female = new HashMap<>();
        both = new HashMap<>();

        for (int age = 0; age <= MAX_AGE; age++)
        {
            male.put(age, rural.male.get(age) + urban.male.get(age));
            female.put(age, rural.female.get(age) + urban.female.get(age));
            both.put(age, rural.both.get(age) + urban.both.get(age));
        }

        male_total = rural.male_total + urban.male_total;
        female_total = rural.female_total + urban.female_total;
        both_total = rural.both_total + urban.both_total;

        male_unknown = rural.male_unknown + urban.male_unknown;
        female_unknown = rural.female_unknown + urban.female_unknown;
        both_unknown = rural.both_unknown + urban.both_unknown;

        validateSingle();
    }

    private void validateSingle() throws Exception
    {
        double sum_m = 0;
        double sum_f = 0;
        double sum_b = 0;

        for (int age = 0; age <= MAX_AGE; age++)
        {
            if (!male.containsKey(age) || !female.containsKey(age) || !both.containsKey(age))
                throw new Exception("Mising entry in population table");

            double m = male.get(age);
            double f = female.get(age);
            double b = both.get(age);

            if (differ(m + f, b))
                mismatch();

            sum_m += m;
            sum_f += f;
            sum_b += b;
        }

        if (male_total == 0)
            male_total = sum_m + male_unknown;

        if (female_total == 0)
            female_total = sum_f + female_unknown;

        if (both_total == 0)
            both_total = sum_b + both_unknown;

        if (differ(male_total + female_total, both_total))
            mismatch();

        if (differ(male_unknown + female_unknown, both_unknown))
            mismatch();

        if (differ(sum_m + male_unknown, male_total))
            mismatch();

        if (differ(sum_f + female_unknown, female_total))
            mismatch();

        if (differ(sum_b + both_unknown, both_total))
            mismatch();
    }

    public void makeBoth(Locality locality) throws Exception
    {
        forLocality(locality).makeBoth();
    }

    public void makeBoth() throws Exception
    {
        both = new HashMap<>();

        for (int age = 0; age <= MAX_AGE; age++)
        {
            double m = get(Gender.MALE, age);
            double f = get(Gender.FEMALE, age);
            set(Gender.BOTH, age, m + f);
            both_unknown = male_unknown + female_unknown;
            both_total = male_total + female_total;
        }
    }

    /****************************************************************************************************/

    private void mismatch() throws Exception
    {
        throw new Exception("Mismatching data in population table");
    }

    private int asInt(String s)
    {
        return Integer.parseInt(s.replace(",", ""));
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