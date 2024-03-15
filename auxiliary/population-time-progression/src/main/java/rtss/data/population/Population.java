package rtss.data.population;

import java.util.HashMap;
import java.util.Map;

import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

/**
 * Holds male, female and both-genders populations by age
 */
public class Population
{
    public static final int MAX_AGE = 100;

    Map<Integer, Double> male = new HashMap<>();
    Map<Integer, Double> female = new HashMap<>();
    Map<Integer, Double> both = new HashMap<>();
    Locality locality;

    double male_unknown = 0;
    double male_total = 0;
    double female_unknown = 0;
    double female_total = 0;
    double both_unknown = 0;
    double both_total = 0;

    static public Population newPopulation()
    {
        Population p = new Population();
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

        return p;
    }

    /****************************************************************************************************/

    public double male(int age) throws Exception
    {
        return get(Gender.MALE, age);
    }

    public double female(int age) throws Exception
    {
        return get(Gender.FEMALE, age);
    }

    public double both(int age) throws Exception
    {
        return get(Gender.BOTH, age);
    }

    public double fm(int age) throws Exception
    {
        return both(age);
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

    public void add(Gender gender, int age, double value) throws Exception
    {
        Map<Integer, Double> m = forGender(gender);

        if (m.containsKey(age))
        {
            m.put(age, m.get(age) + value);
        }
        else
        {
            m.put(age, value);
        }
    }

    public void sub(Gender gender, int age, double value) throws Exception
    {
        Map<Integer, Double> m = forGender(gender);

        if (m.containsKey(age))
        {
            m.put(age, m.get(age) - value);
        }
        else
        {
            if (value > 0)
                throw new Exception("Negative population");
            m.put(age, -value);
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

    public void resetUnknown() throws Exception
    {
        male_unknown = 0;
        female_unknown = 0;
        both_unknown = 0;
    }

    public void resetTotal() throws Exception
    {
        male_total = 0;
        female_total = 0;
        both_total = 0;

        if (male != null)
            male_total = this.sum(Gender.MALE, 0, MAX_AGE) + male_unknown;
        if (female != null)
            female_total = this.sum(Gender.FEMALE, 0, MAX_AGE) + female_unknown;
        if (both != null)
            both_total = this.sum(Gender.BOTH, 0, MAX_AGE) + both_unknown;
    }

    /****************************************************************************************************/

    public static Population load(String path) throws Exception
    {
        return load(path, null);
    }

    public static Population load(String path, Locality locality) throws Exception
    {
        Population p = new Population();
        p.locality = locality;
        p.do_load(path);
        return p;
    }

    public static class Columns
    {
        public int age = -1;
        public int male = -1;
        public int female = -1;
        public int both = -1;

        public int ncolumns()
        {
            int nc = 0;
            if (age != -1)
                nc++;
            if (male != -1)
                nc++;
            if (female != -1)
                nc++;
            if (both != -1)
                nc++;
            return nc;
        }
    }

    private void do_load(String path) throws Exception
    {
        do_load(path, null);
    }

    private void do_load(String path, Columns cols) throws Exception
    {
        String rdata = Util.loadResource(path);
        rdata = rdata.replace("\r\n", "\n");
        for (String line : rdata.split("\n"))
        {
            char unicode_feff = '\uFEFF';
            line = line.replace("" + unicode_feff, "");

            int k = line.indexOf('#');
            if (k != -1)
            {
                if (cols == null)
                    cols = detectColumns(line.substring(k + 1));
                line = line.substring(0, k);
            }
            line = line.replace("\t", " ").replaceAll(" +", " ").trim();
            if (line.length() == 0)
                continue;

            k = line.indexOf(":");
            if (k != -1)
            {
                String s1 = line.substring(0, k);
                String s2 = line.substring(k + 1);
                line = s1.replace(" ", "_") + " " + s2;
            }

            if (cols == null)
                throw new Exception("Unidentified format of population table file");

            String[] el = line.split(" ");
            el = removeEmpty(el);
            if (el.length != cols.ncolumns())
                throw new Exception("Invalid format of population table");

            String age = el[cols.age];
            if (age.toLowerCase().contains("итого") || age.contains("-"))
                continue;
            if (age.equals("" + MAX_AGE + "+"))
                age = "" + MAX_AGE;

            int m = asInt(el[cols.male]);
            int f = asInt(el[cols.female]);
            int b;

            if (cols.both != -1)
                b = asInt(el[cols.both]);
            else
                b = m + f;

            if (age.equals("unknown"))
            {
                male_unknown = m;
                female_unknown = f;
                both_unknown = b;
            }
            else if (age.equals("total") || age.equals("всего"))
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

        validate();
    }

    private Columns detectColumns(String line)
    {
        Columns cols = new Columns();
        line = line.toLowerCase()
                .trim()
                .replace("both genders", "both")
                .replace("both sexes", "both")
                .replace(", ", ",")
                .replace(",", " ");
        String sa[] = line.split(" ");
        Map<String, Integer> name2ix = new HashMap<>();
        int k = 0;
        for (String s : sa)
        {
            if (name2ix.containsKey(s))
                return null;
            name2ix.put(s, k++);
        }

        cols.age = columnIndex(name2ix, "age");
        cols.male = columnIndex(name2ix, "male");
        cols.female = columnIndex(name2ix, "female");
        cols.both = columnIndex(name2ix, "both");

        if (cols.age >= 0 && cols.male >= 0 && cols.female >= 0)
            return cols;

        return null;
    }

    private int columnIndex(Map<String, Integer> name2ix, String name)
    {
        Integer ix = name2ix.get(name);
        if (ix == null)
            return -1;
        else
            return ix;
    }

    private String[] removeEmpty(String[] sa)
    {
        int n = 0;
        for (String s : sa)
        {
            if (s.trim().length() != 0)
                n++;
        }

        String[] res = new String[n];
        n = 0;
        for (String s : sa)
        {
            if (s.trim().length() != 0)
                res[n++] = s.trim();
        }

        return res;
    }

    /****************************************************************************************************/

    void validate() throws Exception
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

            if (m < 0 || f < 0 || b < 0)
                negative();

            if (differ(m + f, b))
                mismatch();

            sum_m += m;
            sum_f += f;
            sum_b += b;
        }

        if (male_unknown < 0 || female_unknown < 0 || both_unknown < 0)
            negative();

        if (male_total == 0)
            male_total = sum_m + male_unknown;

        if (female_total == 0)
            female_total = sum_f + female_unknown;

        if (both_total == 0)
            both_total = sum_b + both_unknown;

        if (male_total < 0 || female_total < 0 || both_total < 0)
            negative();

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

    private void negative() throws Exception
    {
        throw new Exception("Negative counts in population table");
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

    /****************************************************************************************************/

    public void smooth() throws Exception
    {
        double[] d;

        d = toArray(Gender.MALE);
        fromArray(Gender.MALE, smooth(d));

        d = toArray(Gender.FEMALE);
        fromArray(Gender.FEMALE, smooth(d));

        makeBoth();
        validate();
    }

    public double[] toArray(Gender gender) throws Exception
    {
        int maxage = -1;
        Map<Integer, Double> m = forGender(gender);
        for (int age : m.keySet())
        {
            maxage = Math.max(age, maxage);
        }

        double d[] = new double[maxage + 1];

        for (int age = 0; age <= maxage; age++)
        {
            Double v = m.get(age);
            if (v == null)
                throw new Exception("Missing data");
            d[age] = v;
        }

        return d;
    }

    public void fromArray(Gender gender, double[] d) throws Exception
    {
        Map<Integer, Double> m = new HashMap<>();

        for (int age = 0; age <= d.length - 1; age++)
            m.put(age, d[age]);

        // TODO: collapse anything beyond MAX_AGE into MAX_AGE

        switch (gender)
        {
        case MALE:
            male = m;
            male_total = Util.sum(d) + male_unknown;
            break;

        case FEMALE:
            female = m;
            female_total = Util.sum(d) + female_unknown;
            break;

        case BOTH:
            both = m;
            both_total = Util.sum(d) + both_unknown;
            break;

        default:
            throw new Exception("Incorrect gender");
        }
    }

    private double[] smooth(double[] d) throws Exception
    {
        double[] d2 = SmoothPopulation.smooth(d);
        if (differ(Util.sum(d), Util.sum(d2)))
            throw new Exception("Smoothing error");
        return d2;
    }
}