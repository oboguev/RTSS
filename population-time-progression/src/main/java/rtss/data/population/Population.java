package rtss.data.population;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import rtss.data.DoubleArray;
import rtss.data.ValueConstraint;
import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.population.forward.PopulationContext;
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

    private boolean sealed = false;

    private DoubleArray newDoubleArray()
    {
        return newDoubleArray(null);
    }

    private DoubleArray newDoubleArray(ValueConstraint vc)
    {
        if (vc == null)
            vc = ValueConstraint.NON_NEGATIVE;
        return new DoubleArray(MAX_AGE, vc);
    }

    static public Population newTotalPopulation()
    {
        return newPopulation(Locality.TOTAL);
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

    public void setValueConstraint(ValueConstraint vc)
    {
        male.setValueConstraint(vc);
        female.setValueConstraint(vc);
        both.setValueConstraint(vc);
    }

    public ValueConstraint valueConstraint()
    {
        if (both != null)
            return both.valueConstraint();
        else if (male != null)
            return male.valueConstraint();
        else if (female != null)
            return female.valueConstraint();
        else
            return null;
    }

    public Population(Locality locality,
            double[] m, double m_unknown, ValueConstraint mvc,
            double[] f, double f_unknown, ValueConstraint fvc) throws Exception
    {
        this.locality = locality;

        if (m.length != MAX_AGE + 1 || f.length != m.length)
            throw new IllegalArgumentException();

        if (mvc != null)
            male.setValueConstraint(mvc);

        if (fvc != null)
            female.setValueConstraint(fvc);

        if (mvc != null || fvc != null)
        {
            if ((mvc == null) != (fvc == null))
                throw new Exception("Mismatching constraints");
            if (!mvc.equals(fvc))
                throw new Exception("Mismatching constraints");
            both.setValueConstraint(mvc);
        }

        for (int age = 0; age <= MAX_AGE; age++)
        {
            set(Gender.MALE, age, m[age]);
            set(Gender.FEMALE, age, f[age]);
        }

        male_unknown = m_unknown;
        female_unknown = f_unknown;

        ValueConstraint bvc = both.valueConstraint();
        both = null;
        recalcTotal();

        makeBoth(bvc);

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

    public double sum(int age1, int age2) throws Exception
    {
        return sum(Gender.BOTH, age1, age2);
    }

    public double sum(Gender gender) throws Exception
    {
        return sum(gender, 0, MAX_AGE);
    }

    public double sum() throws Exception
    {
        return sum(Gender.BOTH, 0, MAX_AGE);
    }

    public void set(Gender gender, int age, double value) throws Exception
    {
        checkWritable();

        DoubleArray m = forGender(gender);
        Util.validate(value);
        checkValueConstraint(value, m);
        m.put(age, value);
    }

    public void add(Gender gender, int age, double value) throws Exception
    {
        checkWritable();

        DoubleArray m = forGender(gender);

        if (m.containsKey(age))
        {
            checkValueConstraint(m.get(age) + value, m);
            m.put(age, Util.validate(m.get(age) + value));
        }
        else
        {
            checkValueConstraint(value, m);
            m.put(age, Util.validate(value));
        }
    }

    public void sub(Gender gender, int age, double value) throws Exception
    {
        checkWritable();

        DoubleArray m = forGender(gender);

        if (m.containsKey(age))
        {
            checkValueConstraint(m.get(age) - value, m);
            m.put(age, Util.validate(m.get(age) - value));
        }
        else
        {
            if (value > 0)
                throw new Exception("Negative population");
            m.put(age, Util.validate(-value));
        }
    }

    private void checkValueConstraint(double v, DoubleArray m) throws Exception
    {
        checkValueConstraint(v, m.valueConstraint());
    }

    private void checkValueConstraint(double v, ValueConstraint vc) throws Exception
    {
        Util.checkValid(v);

        switch (vc)
        {
        case NONE:
            break;

        case NON_NEGATIVE:
            if (v < 0)
                throw new Exception("Negative population");
            break;

        case POSITIVE:
            if (v <= 0)
                throw new Exception("Negative or zero population");
            break;
        }
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
        checkWritable();

        male_unknown = 0;
        female_unknown = 0;
        both_unknown = 0;
    }

    public void recalcTotal() throws Exception
    {
        checkWritable();

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

    /*
     * Выборка [age1 ... age2] или [ageday1 ... ageday2].
     * 
     * Нецелое значение года означает, что население выбирается только от/до этой возрастной точки.
     * Так age2 = 80.0 означает, что население с возраста 80.0 лет исключено. 
     * Аналогично, age2 = 80.5 означает, что включена половина населения в возрасте 80 лет,
     * а население начиная с возраста 81 года исключено целиком. 
     */
    private final int DAYS_PER_YEAR = 365;

    public Population selectByAge(double age1, double age2) throws Exception
    {
        int ageday1 = (int) Math.round(age1 * DAYS_PER_YEAR);
        int ageday2 = (int) Math.round(age2 * DAYS_PER_YEAR);
        return selectByAge(ageday1, ageday2);
    }
    
    public Population selectByAge(int ageday1, int ageday2) throws Exception
    {
        Population p = new Population();

        p.locality = locality;

        if (Util.True && male != null && female != null)
        {
            /* try to use spline */
            p = this.clone();
            PopulationContext fctx = new PopulationContext(PopulationContext.ALL_AGES);
            PopulationByLocality pl = new PopulationByLocality(p, null, null);
            pl = fctx.begin(pl);

            for (int nd = 0; nd <= fctx.MAX_DAY; nd++)
            {
                if (nd >= ageday1 && nd <= ageday2)
                {
                    // leave alone
                }
                else
                {
                    // outside of selected range: zero
                    fctx.set(Locality.TOTAL, Gender.MALE, nd, 0);
                    fctx.set(Locality.TOTAL, Gender.FEMALE, nd, 0);
                }
            }

            pl = fctx.end(pl);
            p = pl.forLocality(Locality.TOTAL);
        }
        else
        {
            /* basic method */
            double age1 = (double) ageday1 / DAYS_PER_YEAR;
            double age2 = (double) ageday2 / DAYS_PER_YEAR;

            if (male != null || female != null)
            {
                if (male != null)
                    p.male = male.selectByAge(age1, age2);

                if (female != null)
                    p.female = female.selectByAge(age1, age2);

                if (both != null)
                    p.makeBoth();
            }
            else if (both != null)
            {
                p.both = both.selectByAge(age1, age2);
            }
        }

        p.validate();

        return p;
    }

    /*
     * Вернуть результат вычитания @this - @p
     */
    public Population sub(Population p) throws Exception
    {
        return sub(p, null);
    }

    public Population sub(Population p, ValueConstraint rvc) throws Exception
    {
        if (locality != p.locality && rvc == null)
            throw new IllegalArgumentException("населения разнотипны");

        if ((male != null) != (p.male != null))
            throw new IllegalArgumentException("населения разнотипны");

        if ((female != null) != (p.female != null))
            throw new IllegalArgumentException("населения разнотипны");

        if ((both != null) != (p.both != null))
            throw new IllegalArgumentException("населения разнотипны");

        Population res = newPopulation(locality);
        if (rvc != null)
            res.setValueConstraint(rvc);

        if (male != null)
            res.male = male.sub(p.male, rvc);

        if (female != null)
            res.female = female.sub(p.female, rvc);

        if (both != null)
            res.both = both.sub(p.both, rvc);

        return res;
    }

    /*
     * Вернуть результат вычитания @this - @p
     */
    public Population add(Population p) throws Exception
    {
        return add(p, null);
    }

    public Population add(Population p, ValueConstraint rvc) throws Exception
    {
        if (locality != p.locality && rvc == null)
            throw new IllegalArgumentException("населения разнотипны");

        if ((male != null) != (p.male != null))
            throw new IllegalArgumentException("населения разнотипны");

        if ((female != null) != (p.female != null))
            throw new IllegalArgumentException("населения разнотипны");

        if ((both != null) != (p.both != null))
            throw new IllegalArgumentException("населения разнотипны");

        Population res = newPopulation(locality);
        if (rvc != null)
            res.setValueConstraint(rvc);

        if (male != null)
            res.male = male.add(p.male, rvc);

        if (female != null)
            res.female = female.add(p.female, rvc);

        if (both != null)
            res.both = both.add(p.both, rvc);

        return res;
    }

    /*
     * Вернуть результат (@this + @p) / 2
     */
    public Population avg(Population p) throws Exception
    {
        Population p2 = this.add(p);
        p2 = RescalePopulation.scaleBy(p2, 0.5);
        return p2;
    }

    public Population avg(Population p, ValueConstraint rvc) throws Exception
    {
        Population p2 = this.add(p, rvc);
        p2 = RescalePopulation.scaleBy(p2, 0.5);
        return p2;
    }

    /*
     * Сдвинуть возрастное распределение на @years лет вверх
     */
    public Population moveUp(double years) throws Exception
    {
        Population res = newPopulation(locality);

        if (male != null)
            res.male = male.moveUp(years);

        if (female != null)
            res.female = female.moveUp(years);

        if (both != null)
            res.both = both.moveUp(years);

        return res;
    }

    /*
     * Сдвинуть возрастное распределение на @years лет вниз
     */
    public Population moveDown(double years) throws Exception
    {
        Population res = newPopulation(locality);

        if (male != null)
            res.male = male.moveDown(years);

        if (female != null)
            res.female = female.moveDown(years);

        if (both != null)
            res.both = both.moveDown(years);

        return res;
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

    public void saveToFile(String dirPath, String comment) throws Exception
    {
        final String nl = Util.nl;

        StringBuilder sb = new StringBuilder();
        sb.append("# age, both genders, male, female" + nl);
        if (comment != null && comment.length() != 0)
            sb.append(nl + "# " + comment + nl);
        sb.append(nl);

        for (int age = 0; age <= MAX_AGE; age++)
        {
            sb.append(String.format("%-4d %-15s %-15s %-15s" + nl, age, f2s(both.get(age)), f2s(male.get(age)), f2s(female.get(age))));
        }

        File fp = new File(dirPath);
        fp = new File(dirPath, locality.name().toLowerCase() + ".txt");
        String fn = fp.getCanonicalFile().getAbsolutePath();

        Util.writeAsFile(fn, sb.toString());
    }

    private String f2s(double v)
    {
        String s = String.format("%,15.0f", v);
        while (s.startsWith(" "))
            s = s.substring(1);
        return s;
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

            male.valueConstraint().validate(m);
            female.valueConstraint().validate(f);
            both.valueConstraint().validate(b);

            // if (m < 0 || f < 0 || b < 0)
            //     negative();

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
        makeBoth(null);
    }

    public void makeBoth(ValueConstraint vc) throws Exception
    {
        if (vc == null && both != null)
            vc = both.valueConstraint();

        both = newDoubleArray(vc);

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

    public void zero() throws Exception
    {
        zero(Gender.MALE);
        zero(Gender.FEMALE);
        zero(Gender.BOTH);
    }

    public void zero(Gender gender) throws Exception
    {
        for (int age = 0; age <= MAX_AGE; age++)
            set(gender, age, 0);

        switch (gender)
        {
        case MALE:
            male_unknown = 0;
            male_total = 0;
            break;
        case FEMALE:
            female_unknown = 0;
            female_total = 0;
            break;
        case BOTH:
            both_unknown = 0;
            both_total = 0;
            break;
        default:
            throw new IllegalArgumentException();
        }
    }

    /****************************************************************************************************/

    @Override
    public String toString()
    {
        try
        {
            return toString("") + Util.nl + Util.nl + dump();
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

    public String dump() throws Exception
    {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("AGE B M F"));
        sb.append(Util.nl);

        for (int age = 0; age <= MAX_AGE; age++)
        {
            sb.append(String.format("%-3d %f %f %f", age, both(age), male(age), female(age)));
            sb.append(Util.nl);
        }

        return sb.toString();
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

    /*****************************************************************************************************/

    public void seal()
    {
        sealed = true;
    }

    private void checkWritable() throws Exception
    {
        if (sealed)
            throw new Exception("Table is sealed and cannot be modified");
    }

    public void validateBMF() throws Exception
    {
        for (int age = 0; age <= MAX_AGE; age++)
        {
            double m = male(age);
            double f = female(age);
            double b = both(age);
            if (Math.abs(m + f - b) > 0.001)
                throw new Exception("BMF mismatch");
        }
    }
}