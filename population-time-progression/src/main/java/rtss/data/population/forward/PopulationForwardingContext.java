package rtss.data.population.forward;

import java.util.HashMap;
import java.util.Map;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.synthetic.InterpolateYearlyToDailyAsValuePreservingMonotoneCurve;
import rtss.data.population.PopulationByLocality;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

/**
 * 
 * Структура PopulationForwardingContext применяется для детального учёта населения самых младших возрастов при продвижке,
 * чтобы точнее учесть рождения и ранюю детскую смертность.
 * 
 * Смертность в ранних возрастах изменяется резко с возрастом, различаясь в начале и в конце года.
 * Поэтому для учёта смертности детского населения при последовательных шагах продвижки, особенно если некоторые шаги
 * имеют длительность менее года, требуется более детальная разбивка численности населения этих возрастных групп,
 * нежели по году возраста. Необходмо знать не только возраст в годах, но и в какой именно части года их возраста
 * родились отслеживаемые.
 * 
 * Структуры Population и PopulationByLocality индексируют население только по годам возраста и не дают возможности
 * учёта с более детальным временны́м разрешением.
 * 
 * Структура PopulationForwardingContext, в противоположность, хранит численность населения с возрастом индексированным
 * в днях (а не годах) с даты рождения до текущего момента.
 * 
 * Индексация производится по (Locality, Gender, ndays), где ndays – число дней прошедших со дня рождения до текущего момента.
 * 
 * Хранимые числа населения уже подвергнуты влиянию смертности (и соответствующей числовой децимации) и представляют
 * числа доживших до настоящего момента.
 * 
 * Численность населения хранится в PopulationForwardingContext только для младших NYEARS лет, т.е. возрастов [0 ... NYEARS-1] лет
 * или [0 ... MAX_DAY] дней со дня рождения.
 * 
 * Хранятся данные только для Gender.MALE и Gender.FEMALE, но не для Gender.BOTH.
 * 
 * Использование:
 * 
 *     PopulationByLocality p = ...
 *     PopulationForwardingContext fctx = new PopulationForwardingContext();
 *     PopulationByLocality pto = fctx.begin(p);
 *     ....
 *     pto = forward(pto, fctx, mt, yfraction) <== повторяемое сколько требуется
 *     ....
 *     ptoEnd = fctx.end(pto);
 * 
 * fctx.begin переносит младшие возрастные группы в контекст, обнуляя их в возвращаеммом @pto.
 * 
 * fctx.end переносит население из контекста обратно в структуру PopulationByLocality. Контекст и @pto передаваемое
 * аргуметном для fctx.end при этом не изменяются, что позволяет сделать снимок населения в настоящий момент и
 * затем продолжить продвижку.
 */
public class PopulationForwardingContext
{
    public final int DAYS_PER_YEAR = 365;

    public final int NYEARS = 4;
    public final int MAX_YEAR = NYEARS - 1;

    public final int NDAYS = NYEARS * DAYS_PER_YEAR;
    public final int MAX_DAY = NDAYS - 1;

    private boolean began = false;
    private boolean hasRuralUrban;
    private Map<String, Double> m = new HashMap<>();

    /*
     * Total number of births during forwarding
     */
    private Map<String, Double> totalBirths;

    /* =============================================================================================== */

    public PopulationForwardingContext clone()
    {
        PopulationForwardingContext cx = new PopulationForwardingContext();
        cx.began = began;
        cx.hasRuralUrban = hasRuralUrban;
        cx.m = new HashMap<String, Double>(m);
        cx.m_lx = new HashMap<String, double[]>(m_lx);
        cx.totalBirths = new HashMap<>(totalBirths);
        return cx;
    }

    public double get(Locality locality, Gender gender, int day) throws Exception
    {
        String key = key(locality, gender, day);
        Double d = m.get(key);
        return d != null ? d : 0;
    }

    public void set(Locality locality, Gender gender, int day, double v) throws Exception
    {
        String key = key(locality, gender, day);
        checkNonNegative(v);
        m.put(key, v);
    }

    public double add(Locality locality, Gender gender, int day, double v) throws Exception
    {
        String key = key(locality, gender, day);
        Double d = m.get(key);
        if (d == null)
            d = 0.0;
        v += d;
        checkNonNegative(v);
        m.put(key, v);
        return v;
    }

    public double sub(Locality locality, Gender gender, int day, double v) throws Exception
    {
        String key = key(locality, gender, day);
        Double d = m.get(key);
        if (d == null)
            d = 0.0;
        v = d - v;
        checkNonNegative(v);
        m.put(key, v);
        return v;
    }

    public double sum(Locality locality, Gender gender, int nd1, int nd2) throws Exception
    {
        if (!began)
            return 0;
        
        if (locality == Locality.TOTAL && hasRuralUrban)
            return sum(Locality.URBAN, gender, nd1, nd2) + sum(Locality.RURAL, gender, nd1, nd2);

        if (gender == Gender.BOTH)
            return sum(locality, Gender.MALE, nd1, nd2) + sum(locality, Gender.FEMALE, nd1, nd2);

        double sum = 0;
        for (int nd = nd1; nd <= nd2; nd++)
            sum += get(locality, gender, nd);
        return sum;
    }

    public double sumAge(Locality locality, Gender gender, int age) throws Exception
    {
        return sum(locality, gender, firstDayForAge(age), lastDayForAge(age));
    }

    public double sumAges(Locality locality, Gender gender, int age1, int age2) throws Exception
    {
        double sum = 0;
        for (int age = age1; age <= age2; age++)
            sum += sumAge(locality, gender, age);
        return sum;
    }

    public double sumAllAges(Locality locality, Gender gender) throws Exception
    {
        return sumAges(locality, gender, 0, MAX_YEAR);
    }

    private String key(Locality locality, Gender gender, int day) throws Exception
    {
        if (!began)
            throw new IllegalArgumentException();
        
        if (hasRuralUrban && locality == Locality.TOTAL)
            throw new IllegalArgumentException();
        if (!hasRuralUrban && locality != Locality.TOTAL)
            throw new IllegalArgumentException();
        if (day < 0 || day > MAX_DAY || gender == Gender.BOTH)
            throw new IllegalArgumentException();
        return locality.name() + "-" + gender.name() + "-" + day;
    }

    private void checkNonNegative(double v) throws Exception
    {
        if (v < 0)
            throw new Exception("Negative population");
    }

    public int firstDayForAge(int age)
    {
        return age * DAYS_PER_YEAR;
    }

    public int lastDayForAge(int age)
    {
        return (age + 1) * DAYS_PER_YEAR - 1;
    }

    public double[] asArray(Locality locality, Gender gender) throws Exception
    {
        double[] v = new double[NDAYS];
        for (int nd = 0; nd < NDAYS; nd++)
            v[nd] = get(locality, gender, nd);
        return v;
    }

    public void fromArray(Locality locality, Gender gender, double[] v) throws Exception
    {
        for (int nd = 0; nd < v.length; nd++)
            set(locality, gender, nd, v[nd]);
    }

    public void zero(Locality locality, Gender gender) throws Exception
    {
        for (int nd = 0; nd < NDAYS; nd++)
            set(locality, gender, nd, 0);
    }

    public int day2age(int nd)
    {
        return nd / DAYS_PER_YEAR;
    }

    /* =============================================================================================== */

    private Map<String, double[]> m_lx = new HashMap<>();

    /*
     * Вычислить подневное значение "lx" их годовых значений в таблице смертности
     */
    public double[] get_daily_lx(final CombinedMortalityTable mt, final Locality locality, final Gender gender) throws Exception
    {
        String key = String.format("%s-%s-%s", mt.tableId(), locality.name(), gender.name());
        double[] daily_lx = m_lx.get(key);

        if (daily_lx == null)
        {
            double[] yearly_lx = mt.getSingleTable(locality, gender).lx();
            /* значения после MAX_YEAR + 1 не слишком важны */
            yearly_lx = Util.splice(yearly_lx, 0, MAX_YEAR + 5);
            daily_lx = InterpolateYearlyToDailyAsValuePreservingMonotoneCurve.yearly2daily(yearly_lx);
            m_lx.put(key, daily_lx);
        }

        return daily_lx;
    }

    /* =============================================================================================== */

    /*
     * Переместить детские ряды из @p в контекст.
     * Вернуть население с обнулёнными детскими рядами.
     */
    public PopulationByLocality begin(final PopulationByLocality p) throws Exception
    {
        if (began)
            throw new IllegalArgumentException();
        
        m.clear();

        PopulationByLocality pto = p.clone();

        hasRuralUrban = p.hasRuralUrban();
        began = true;

        if (hasRuralUrban)
        {
            begin(pto, Locality.RURAL);
            begin(pto, Locality.URBAN);
            pto.recalcTotalForEveryLocality();
            pto.recalcTotalLocalityFromUrbanRural();
        }
        else
        {
            begin(pto, Locality.TOTAL);
            pto.recalcTotalForEveryLocality();
        }

        pto.validate();

        return pto;
    }

    private void begin(PopulationByLocality p, Locality locality) throws Exception
    {
        begin(p, locality, Gender.MALE);
        begin(p, locality, Gender.FEMALE);
        p.makeBoth(locality);
    }

    private void begin(PopulationByLocality p, Locality locality, Gender gender) throws Exception
    {
        for (int age = 0; age < NYEARS; age++)
        {
            double v = p.get(locality, gender, age);
            p.set(locality, gender, age, 0);

            for (int nd = firstDayForAge(age); nd <= lastDayForAge(age); nd++)
                set(locality, gender, nd, v / DAYS_PER_YEAR);
        }
    }

    /*
     * Переместить детские ряды из контекста в структуру населения.
     * 
     * Контекст не изменяется и может быть использован для повторных операций. Это позволяет сделать
     * snapshot населения в текущий момент и затем продолжить продвижку.
     * 
     * Возвращает результат добавления. Исходная структура @p не меняется.   
     */
    public PopulationByLocality end(final PopulationByLocality p) throws Exception
    {
        if (!began)
            throw new IllegalArgumentException();
        
        PopulationByLocality pto = p.clone();

        if (pto.hasRuralUrban())
        {
            end(pto, Locality.RURAL);
            end(pto, Locality.URBAN);
        }

        if (pto.hasTotal())
        {
            end(pto, Locality.TOTAL);
        }

        pto.recalcTotalForEveryLocality();
        pto.validate();

        return pto;
    }

    private void end(PopulationByLocality p, Locality locality) throws Exception
    {
        end(p, locality, Gender.MALE);
        end(p, locality, Gender.FEMALE);
    }

    private void end(PopulationByLocality p, Locality locality, Gender gender) throws Exception
    {
        for (int age = 0; age < NYEARS; age++)
        {
            double v = sum(locality, gender, firstDayForAge(age), lastDayForAge(age));
            p.add(locality, gender, age, v);
            p.add(locality, Gender.BOTH, age, v);
        }
    }

    /* =============================================================================================== */

    public double getTotalBirths(Locality locality, Gender gender) throws Exception
    {
        if (!began)
            return 0;

        if (hasRuralUrban && locality == Locality.TOTAL)
        {
            return getTotalBirths(Locality.RURAL, gender) + getTotalBirths(Locality.URBAN, gender); 
        }

        switch (gender)
        {
        case MALE:
        case FEMALE:
            Double v = totalBirths.get(key(locality, gender));
            if (v == null)
                v = 0.0;
            return v;

        case BOTH:
            return getTotalBirths(locality, Gender.MALE) + getTotalBirths(locality, Gender.FEMALE);

        default:
            throw new IllegalArgumentException();
        }
    }

    public void setTotalBirths(Locality locality, Gender gender, double births) throws Exception
    {
        switch (gender)
        {
        case MALE:
        case FEMALE:
            break;
        default:
            throw new IllegalArgumentException();
        }
        
        totalBirths.put(key(locality, gender), births);
    }

    public void addTotalBirths(Locality locality, Gender gender, double births) throws Exception
    {
        switch (gender)
        {
        case MALE:
        case FEMALE:
            break;
        default:
            throw new IllegalArgumentException();
        }
        
        Double v = totalBirths.get(key(locality, gender));
        if (v == null)
            v = 0.0;
        totalBirths.put(key(locality, gender), v + births);
    }

    private String key(Locality locality, Gender gender) throws Exception
    {
        if (!began)
            throw new IllegalArgumentException();
        
        if (hasRuralUrban && locality == Locality.TOTAL)
            throw new IllegalArgumentException();
        if (!hasRuralUrban && locality != Locality.TOTAL)
            throw new IllegalArgumentException();
        return locality.name() + "-" + gender.name();
    }

    /* =============================================================================================== */

    @Override
    public String toString()
    {
        try
        {
            StringBuilder sb = new StringBuilder();

            double m = sumAllAges(Locality.TOTAL, Gender.MALE);
            double f = sumAllAges(Locality.TOTAL, Gender.FEMALE);
            sb_out(sb, "total.", m, f);

            if (hasRuralUrban)
            {
                m = sumAllAges(Locality.URBAN, Gender.MALE);
                f = sumAllAges(Locality.URBAN, Gender.FEMALE);
                sb_out(sb, "urban.", m, f);

                m = sumAllAges(Locality.RURAL, Gender.MALE);
                f = sumAllAges(Locality.RURAL, Gender.FEMALE);
                sb_out(sb, "rural.", m, f);
            }

            return sb.toString();
        }
        catch (Exception ex)
        {
            return "<exception while formating>";
        }
    }

    private void sb_out(StringBuilder sb, String prefix, double m, double f)
    {
        if (sb.length() != 0)
            sb.append(" ");
        sb.append(String.format("%smf:%s %sm:%s %sf:%s", prefix, f2k(m + f), prefix, f2k(m), prefix, f2k(f)));
    }

    private String f2k(double v)
    {
        String s = String.format("%,15.0f", v);
        while (s.startsWith(" "))
            s = s.substring(1);
        return s;
    }
}
