package rtss.data.population;

import java.util.HashMap;
import java.util.Map;

import rtss.data.DoubleArray;
import rtss.data.ValueConstraint;
import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

/**
 * Holds male, female and both-genders populations by age
 */
public class Population
{
    public static final int MAX_AGE = 100;

    DoubleArray male = newDoubleArray();
    DoubleArray female = newDoubleArray();
    DoubleArray both = newDoubleArray();
    Locality locality;

    double male_unknown = 0;
    double male_total = 0;
    double female_unknown = 0;
    double female_total = 0;
    double both_unknown = 0;
    double both_total = 0;
    
    private DoubleArray newDoubleArray()
    {
        return new DoubleArray(MAX_AGE, ValueConstraint.NON_NEGATIVE);
    }

    static public Population newPopulation(Locality locality)
    {
        Population p = new Population();
        p.locality = locality;
        return p;
    }

    private Population()
    {
    }

    public Population(Locality locality, double[] m, double m_unknown, double[] f, double f_unknown) throws Exception
    {
        this.locality = locality;
        
        if (m.length != MAX_AGE + 1 || f.length != m.length)
            throw new IllegalArgumentException();
        
        for (int age = 0; age <= MAX_AGE; age++)
        {
            set(Gender.MALE, age, m[age]);
            set(Gender.FEMALE, age, f[age]);
        }
        
        male_unknown = m_unknown;
        female_unknown = f_unknown;

        both = null;
        recalcTotal();
        
        makeBoth();
        
        validate();
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
            p.male = new DoubleArray(male);
        if (female != null)
            p.female = new DoubleArray(female);
        if (both != null)
            p.both = new DoubleArray(both);

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
        DoubleArray m = forGender(gender);
        if (!m.containsKey(age))
            throw new Exception("Missing data for age " + age);
        return m.get(age);
    }

    public double sum(Gender gender, int age1, int age2) throws Exception
    {
        DoubleArray m = forGender(gender);

        double sum = 0;

        for (int age = age1; age <= age2; age++)
        {
            if (!m.containsKey(age))
                throw new Exception("Missing data for age " + age);
            sum += m.get(age);
        }

        return Util.validate(sum);
    }

    public void set(Gender gender, int age, double value) throws Exception
    {
        DoubleArray m = forGender(gender);
        Util.validate(value);
        checkNonNegative(value);
        m.put(age, value);
    }

    public void add(Gender gender, int age, double value) throws Exception
    {
        DoubleArray m = forGender(gender);

        if (m.containsKey(age))
        {
            checkNonNegative(m.get(age) + value);
            m.put(age, Util.validate(m.get(age) + value));
        }
        else
        {
            checkNonNegative(value);
            m.put(age, Util.validate(value));
        }
    }

    public void sub(Gender gender, int age, double value) throws Exception
    {
        DoubleArray m = forGender(gender);

        if (m.containsKey(age))
        {
            checkNonNegative(m.get(age) - value);
            m.put(age, Util.validate(m.get(age) - value));
        }
        else
        {
            if (value > 0)
                throw new Exception("Negative population");
            m.put(age, Util.validate(-value));
        }
    }

    private void checkNonNegative(double v) throws Exception
    {
        Util.checkValid(v);
        if (v < 0)
            throw new Exception("Negative population");
    }

    private DoubleArray forGender(Gender gender)
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
    
    public double[] asArray(Gender gender) throws Exception
    {
        return forGender(gender).asUnboxedArray();
    }

    public void resetUnknown() throws Exception
    {
        male_unknown = 0;
        female_unknown = 0;
        both_unknown = 0;
    }

    public void recalcTotal() throws Exception
    {
        male_total = 0;
        female_total = 0;
        both_total = 0;

        if (male != null)
            male_total = Util.validate(this.sum(Gender.MALE, 0, MAX_AGE) + male_unknown);
        if (female != null)
            female_total = Util.validate(this.sum(Gender.FEMALE, 0, MAX_AGE) + female_unknown);
        if (both != null)
            both_total = Util.validate(this.sum(Gender.BOTH, 0, MAX_AGE) + both_unknown);
    }
    
    public double getUnknown(Gender gender) throws Exception
    {
        switch (gender)
        {
        case MALE:
            return male_unknown;
        case FEMALE:
            return female_unknown;
        case BOTH:
            return both_unknown;
        default:
            throw new IllegalArgumentException();
        }
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

            line = line.toLowerCase();
            line = line.replace("возраст не указан", "unknown");

            if (cols == null)
                throw new Exception("Unidentified format of population table file");

            String[] el = line.split(" ");
            el = removeEmpty(el);
            if (el.length != cols.ncolumns())
                throw new Exception("Invalid format of population table");

            String age = el[cols.age];
            if (age.toLowerCase().contains("итого") || age.contains("-") || age.contains("–"))
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
            
            Util.checkValid(m);
            Util.checkValid(f);
            Util.checkValid(b);

            if (m < 0 || f < 0 || b < 0)
                negative();

            if (Util.differ(m + f, b))
                mismatch();

            sum_m += m;
            sum_f += f;
            sum_b += b;
        }
        
        Util.checkValid(male_unknown);
        Util.checkValid(female_unknown);
        Util.checkValid(both_unknown);

        Util.checkValid(male_total);
        Util.checkValid(female_total);
        Util.checkValid(both_total);

        Util.checkValid(sum_m);
        Util.checkValid(sum_f);
        Util.checkValid(sum_b);
        
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

        if (Util.differ(male_total + female_total, both_total))
            mismatch();

        if (Util.differ(male_unknown + female_unknown, both_unknown))
            mismatch();

        if (Util.differ(sum_m + male_unknown, male_total))
            mismatch();

        if (Util.differ(sum_f + female_unknown, female_total))
            mismatch();

        if (Util.differ(sum_b + both_unknown, both_total))
            mismatch();
    }

    public void makeBoth() throws Exception
    {
        both = newDoubleArray();

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
        DoubleArray m = forGender(gender);
        Double[] x = m.get();
        
        for (int age = 0; age < x.length; age++)
        {
            if (x[age] != null)
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
        DoubleArray m = newDoubleArray();

        for (int age = 0; age <= d.length - 1; age++)
            m.put(age, Util.validate(d[age]));

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
        if (Util.differ(Util.sum(d), Util.sum(d2)))
            throw new Exception("Smoothing error");
        return d2;
    }
    
    /*
     * Value returned in bin "avg" field is SUM rather than AVG
     */
    public Bin[] binSumByAge(Gender gender, final Bin[] ages) throws Exception
    {
        Bin[] bins = new Bin[ages.length];
        
        for (int k = 0; k < ages.length; k++)
        {
            double sum = this.sum(gender, ages[k].age_x1, ages[k].age_x2);
            bins[k] = new Bin(ages[k].age_x1, ages[k].age_x2, sum);
        }

        return Bins.bins(bins);
    }

    /****************************************************************************************************/

    @Override
    public String toString()
    {
        try
        {
            return toString("");
        }
        catch (Throwable ex)
        {
            return "<exception while formating>";
        }
    }

    public String toString(String prefix) throws Exception
    {
        double m = sum(Gender.MALE, 0, MAX_AGE);
        double f = sum(Gender.FEMALE, 0, MAX_AGE);
        return String.format("%smf:%s %sm:%s %sf:%s", prefix, f2k(m + f), prefix, f2k(m), prefix, f2k(f));
    }

    private String f2k(double v)
    {
        String s = String.format("%,15.0f", v);
        while (s.startsWith(" "))
            s = s.substring(1);
        return s;
    }
    
    public static final String STRUCT_014 = "0 1-4 5-9 10-14 15-19 20-24 25-29 30-34 35-39 40-44 45-49 50-54 55-59 60-64 65-69 70-74 75-79 80-84 85+"; 
    public static final String STRUCT_0459 = "0-4 5-9 10-14 15-19 20-24 25-29 30-34 35-39 40-44 45-49 50-54 55-59 60-64 65-69 70-74 75-79 80-84 85+"; 
    
    public String ageStructure014() throws Exception
    {
        return ageStructure(STRUCT_014);
    }

    public String ageStructure0459() throws Exception
    {
        return ageStructure(STRUCT_0459);
    }

    public String ageStructure(String struct) throws Exception
    {
        return ageStructure(struct, Gender.BOTH);
    }

    public String ageStructure(String struct, String which) throws Exception
    {
        switch (which.trim().toLowerCase())
        {
        case "mf":
        case "fm":
        case "both":
            return ageStructure(struct, Gender.BOTH);

        case "m":
        case "male":
            return ageStructure(struct, Gender.MALE);

        case "f":
        case "female":
            return ageStructure(struct, Gender.FEMALE);
            
        default:
            throw new Exception("Invalid gender selector: " + which);
        }
    }
    
    public String ageStructure(String struct, Gender gender) throws Exception
    {
        StringBuilder sb = new StringBuilder();
        
        double tot = sum(gender, 0, MAX_AGE);

        for (String s : struct.split(" "))
        {
            if (s == null)
                continue;
            s = s.trim();
            if (s.length() == 0)
                continue;
            
            s = s.replace("+", "-" + MAX_AGE);
            
            int age1;
            int age2;
            
            if (s.contains("-"))
            {
                String[] sa = s.split("-");
                if (sa.length != 2)
                    throw new IllegalArgumentException();
                age1 = Integer.parseUnsignedInt(sa[0]);
                age2 = Integer.parseUnsignedInt(sa[1]);
            }
            else
            {
                age1 = age2 = Integer.parseUnsignedInt(s);
            }
            
            double v = sum(gender, age1, age2);
            
            String s_age = "";
            if (age1 == age2)
            {
                s_age += age1; 
            }
            else if (age2 == MAX_AGE)
            {
                s_age += age1 + "+";
            }
            else
            {
                s_age += age1 + "-" + age2;
            }
            
            if (sb.length() != 0)
                sb.append("\n");
            
            sb.append(String.format("%-5s %s (%.2f%%)", s_age, f2k(v), 100 * v / tot));
        }
        
        return sb.toString();
    }
}