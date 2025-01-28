package rtss.data.population.forward;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtss.data.ValueConstraint;
import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.curves.InterpolatePopulationAsMeanPreservingCurve;
import rtss.data.curves.InterpolateYearlyToDailyAsValuePreservingMonotoneCurve;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.Population;
import rtss.data.population.PopulationByLocality;
import rtss.data.population.RescalePopulation;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.data.selectors.LocalityGender;
import rtss.data.selectors.holders.LocalityGenderToDoubleArray;
import rtss.util.Util;

/**
 * 
 * Структура PopulationContext применяется для детального учёта населения самых младших возрастов при передвижке,
 * чтобы точнее учесть рождения и ранюю детскую смертность.
 * 
 * Смертность в ранних возрастах изменяется резко с возрастом, различаясь в начале, на протяжении и в конце года.
 * Поэтому для учёта смертности детского населения при последовательных шагах передвижки, особенно если некоторые шаги
 * имеют длительность менее года, требуется более детальная разбивка численности населения этих возрастных групп,
 * нежели по году возраста. Необходмо знать не только возраст в годах, но и в какой именно части года их возраста
 * родились отслеживаемые.
 * 
 * Структуры Population и PopulationByLocality индексируют население только по годам возраста и не дают возможности
 * учёта с более детальным временны́м разрешением.
 * 
 * Структура PopulationContext, в противоположность, хранит численность населения с возрастом индексированным
 * в днях (а не годах) с даты рождения до текущего момента.
 * 
 * Индексация производится по (Locality, Gender, ndays), где ndays – число дней прошедших со дня рождения до текущего момента.
 * 
 * Хранимые числа населения уже подвергнуты влиянию смертности (и соответствующей числовой децимации) и представляют
 * числа доживших до настоящего момента.
 * 
 * Численность населения хранится в PopulationContext только для младших NYEARS лет, т.е. возрастов [0 ... NYEARS-1] лет
 * или [0 ... MAX_DAY] дней со дня рождения.
 * 
 * Хранятся данные только для Gender.MALE и Gender.FEMALE, но не для Gender.BOTH.
 * 
 * Использование:
 * 
 *     PopulationByLocality p = ...
 *     PopulationContext fctx = new PopulationContext();
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
 * затем продолжить передвижку.
 * 
 * ************************************
 * 
 * PopulationContext также может быть использован для отслеживания всех (а не только детских) возрастов
 * при передвижке. Это полезно при последовательных продвижениях на интервалы не кратные году, или когда желательно
 * сохранить детальную возрастную структуру (с грануляцией не по годам, а по дням возраста). Контекст вбирающий
 * все возрасты позволяет отслеживать возраст по дням, а не годам, и избегать проблем "расплытия" структуры населения
 * при не-годовых передвижках. См. более подробное разъяснение в заголовках файлов ForwardPopulationT/ForwardPopulationUR.
 * Чтобы создать контекст охватывающий все возрасты (0 ... MAX_AGE), следует использовать
 * 
 *     PopulationContext fctx = new PopulationContext(PopulationContext.ALL_AGES);
 *     
 * PopulationContext в этом случае используется как альтернативная содержащая всё население, альтернативная
 * Population и PopulationByLocality, имеющая более высокое (дневное, а не годовое) разрешение по возрасту. 
 *     
 */
public class PopulationContext
{
    public static final int DEFAULT_NYEARS = 5; /* years 0-4 */
    public static final int ALL_AGES = Population.MAX_AGE + 1; /* years 0-MAX_AGE */
    private static final int MAX_AGE = Population.MAX_AGE;

    public final int DAYS_PER_YEAR = 365;

    public int NYEARS;
    public int MAX_YEAR;

    public int NDAYS;
    public int MAX_DAY;

    private boolean began = false;
    private boolean hasRuralUrban;
    private LocalityGenderToDoubleArray m;
    private ValueConstraint valueConstraint;

    /*
     * Total number of births during forwarding
     */
    private Map<String, Double> totalBirths = new HashMap<>();

    public PopulationContext()
    {
        this(DEFAULT_NYEARS);
    }

    public PopulationContext(int NYEARS)
    {
        this.NYEARS = NYEARS;
        this.MAX_YEAR = NYEARS - 1;
        this.NDAYS = NYEARS * DAYS_PER_YEAR;
        this.MAX_DAY = NDAYS - 1;
        this.m = new LocalityGenderToDoubleArray(MAX_DAY, ValueConstraint.NON_NEGATIVE);
    }

    /* =============================================================================================== */

    public PopulationContext clone()
    {
        PopulationContext cx = new PopulationContext(NYEARS);
        cx.began = began;
        cx.hasRuralUrban = hasRuralUrban;
        cx.m = new LocalityGenderToDoubleArray(m);
        cx.m_lx = new HashMap<String, double[]>(m_lx);
        cx.totalBirths = new HashMap<>(totalBirths);
        return cx;
    }

    public void setValueConstraint(ValueConstraint valueConstraint)
    {
        this.valueConstraint = valueConstraint;
    }

    public ValueConstraint valueConstraint()
    {
        return valueConstraint;
    }

    public double getDay(Locality locality, Gender gender, int day) throws Exception
    {
        checkAccess(locality, gender, day);
        Double d = m.get(locality, gender, day);
        return d != null ? d : 0;
    }

    public void setDay(Locality locality, Gender gender, int day, double v) throws Exception
    {
        checkAccess(locality, gender, day);
        checkNonNegative(v);
        m.put(locality, gender, day, v);
    }

    public double addDay(Locality locality, Gender gender, int day, double v) throws Exception
    {
        checkAccess(locality, gender, day);
        Double d = m.get(locality, gender, day);
        if (d == null)
            d = 0.0;
        v += d;
        checkNonNegative(v);
        m.put(locality, gender, day, v);
        return v;
    }

    public double subDay(Locality locality, Gender gender, int day, double v) throws Exception
    {
        checkAccess(locality, gender, day);
        Double d = m.get(locality, gender, day);
        if (d == null)
            d = 0.0;
        v = d - v;
        checkNonNegative(v);
        m.put(locality, gender, day, v);
        return v;
    }

    public double sumDays(Locality locality, Gender gender, int nd1, int nd2) throws Exception
    {
        if (!began)
            return 0;

        if (locality == Locality.TOTAL && hasRuralUrban)
            return sumDays(Locality.URBAN, gender, nd1, nd2) + sumDays(Locality.RURAL, gender, nd1, nd2);

        if (gender == Gender.BOTH)
            return sumDays(locality, Gender.MALE, nd1, nd2) + sumDays(locality, Gender.FEMALE, nd1, nd2);

        double sum = 0;
        for (int nd = nd1; nd <= nd2; nd++)
            sum += getDay(locality, gender, nd);
        return sum;
    }

    public double sumDays(Gender gender, int nd1, int nd2) throws Exception
    {
        return sumDays(Locality.TOTAL, gender, nd1, nd2);
    }

    public double sumDays(int nd1, int nd2) throws Exception
    {
        return sumDays(Locality.TOTAL, Gender.BOTH, nd1, nd2);
    }

    public double sumAge(Locality locality, Gender gender, int age) throws Exception
    {
        return sumDays(locality, gender, firstDayForAge(age), lastDayForAge(age));
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

    private void checkAccess(Locality locality, Gender gender, int day) throws Exception
    {
        if (!began)
            throw new IllegalArgumentException();

        if (hasRuralUrban && locality == Locality.TOTAL)
            throw new IllegalArgumentException();
        if (!hasRuralUrban && locality != Locality.TOTAL)
            throw new IllegalArgumentException();
        if (day < 0 || day > MAX_DAY || gender == Gender.BOTH)
            throw new IllegalArgumentException();
    }

    private void checkNonNegative(double v) throws Exception
    {
        Util.validate(v);
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
            v[nd] = getDay(locality, gender, nd);
        return v;
    }

    public void fromArray(Locality locality, Gender gender, double[] v) throws Exception
    {
        for (int nd = 0; nd < v.length; nd++)
            setDay(locality, gender, nd, v[nd]);
    }

    public void zero(Locality locality, Gender gender) throws Exception
    {
        for (int nd = 0; nd < NDAYS; nd++)
            setDay(locality, gender, nd, 0);
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
        String key = String.format("%s-%s-%s-%d", mt.tableId(), locality.name(), gender.name(), NDAYS);
        double[] daily_lx = m_lx.get(key);

        if (daily_lx == null)
        {
            double[] yearly_lx = mt.getSingleTable(locality, gender).lx();
            /* значения после MAX_YEAR + 1 не слишком важны */
            yearly_lx = Util.splice(yearly_lx, 0, Math.min(MAX_YEAR + 5, Population.MAX_AGE));

            /*
             * Провести дневную кривую так что
             *       daily_lx[0]         = yearly_lx[0]
             *       daily_lx[365]       = yearly_lx[1]
             *       daily_lx[365 * 2]   = yearly_lx[2]
             *       etc.
             */
            daily_lx = InterpolateYearlyToDailyAsValuePreservingMonotoneCurve.yearly2daily(yearly_lx);

            /*
             * Базовая проверка правильности
             */
            if (Util.differ(daily_lx[0], yearly_lx[0]) ||
                Util.differ(daily_lx[365 * 1], yearly_lx[1]) ||
                Util.differ(daily_lx[365 * 2], yearly_lx[2]) ||
                Util.differ(daily_lx[365 * 3], yearly_lx[3]))
            {
                throw new Exception("Ошибка в построении daily_lx");
            }

            m_lx.put(key, daily_lx);
        }

        return daily_lx;
    }

    /* =============================================================================================== */

    /*
     * Переместить детские ряды из @p в контекст.
     * Вернуть население с обнулёнными детскими рядами.
     */
    public Population begin(final Population p) throws Exception
    {
        PopulationByLocality xp = new PopulationByLocality(p);
        PopulationByLocality xp2 = begin(xp);
        return xp2.forLocality(Locality.TOTAL);
    }

    public PopulationByLocality begin(final PopulationByLocality p) throws Exception
    {
        // TODO: сделать аргументом (таблица смертности в год, для которого указана структура населения)
        CombinedMortalityTable mt = null;

        if (this.began)
            throw new IllegalArgumentException();

        m.clear();

        PopulationByLocality pto = p.clone();

        this.hasRuralUrban = p.hasRuralUrban();
        this.began = true;

        if (hasRuralUrban)
        {
            this.valueConstraint = p.forLocality(Locality.URBAN).valueConstraint();

            try
            {
                begin_spline(pto, Locality.RURAL);
                begin_spline(pto, Locality.URBAN);
            }
            catch (Exception ex)
            {
                Util.err("PopulationContext.begin: using basic method");
                m.clear();
                begin_basic(pto, Locality.RURAL, mt);
                begin_basic(pto, Locality.URBAN, mt);
            }

            begin_complete(pto, Locality.RURAL);
            begin_complete(pto, Locality.URBAN);

            pto.recalcTotalForEveryLocality();
            pto.recalcTotalLocalityFromUrbanRural();
        }
        else
        {
            this.valueConstraint = p.forLocality(Locality.TOTAL).valueConstraint();

            try
            {
                begin_spline(pto, Locality.TOTAL);
            }
            catch (Exception ex)
            {
                Util.err("PopulationContext.begin: using basic method");
                m.clear();
                begin_basic(pto, Locality.TOTAL, mt);
            }

            begin_complete(pto, Locality.TOTAL);

            pto.recalcTotalForEveryLocality();
        }

        pto.validate();

        if (hasRuralUrban)
        {
            validate_begin(p, pto, Locality.RURAL);
            validate_begin(p, pto, Locality.URBAN);
        }
        else
        {
            validate_begin(p, pto, Locality.TOTAL);
        }

        if (NYEARS == ALL_AGES && pto.sum() != 0)
            throw new Exception("внутренняя ошибка");

        return pto;
    }

    /*
     * Перемещение сплайном
     */
    private void begin_spline(PopulationByLocality p, Locality locality) throws Exception
    {
        begin_spline(p, locality, Gender.MALE);
        begin_spline(p, locality, Gender.FEMALE);
    }

    private void begin_spline(PopulationByLocality p, Locality locality, Gender gender) throws Exception
    {
        /*
         * Извлечь распределение по годам
         */
        final int ExtraTrailingYearsForSpline = 3;
        double[] v_years = p.forLocality(locality).asArray(gender);
        v_years = Util.splice(v_years, 0, Math.min(MAX_YEAR + ExtraTrailingYearsForSpline, Population.MAX_AGE));

        Bin[] bins = Bins.fromValues(v_years);
        for (int age = 0; age < bins.length; age++)
        {
            Bin bin = bins[age];
            bin.age_x1 = age * DAYS_PER_YEAR;
            bin.age_x2 = bin.age_x1 + (DAYS_PER_YEAR - 1);
            bin.widths_in_years = DAYS_PER_YEAR;
        }
        bins = Bins.sum2avg(bins);

        /*
         * Построить распределение по дням
         */
        double[] v_days = InterpolatePopulationAsMeanPreservingCurve.curve(bins, "PopulationContext.begin");

        for (int age = 0; age < NYEARS; age++)
        {
            double[] vv = Util.splice(v_days, firstDayForAge(age), lastDayForAge(age));

            if (v_years[age] != 0)
                vv = Util.normalize(vv, v_years[age]);
            else
                vv = Util.multiply(vv, 0);

            for (int nd = firstDayForAge(age); nd <= lastDayForAge(age); nd++)
                setDay(locality, gender, nd, vv[nd - firstDayForAge(age)]);
        }
    }

    /*
     * Простое перемещение
     */
    private void begin_basic(PopulationByLocality p, Locality locality, CombinedMortalityTable mt) throws Exception
    {
        if (mt == null)
            Util.err("PopulationContext.begin: mt == null, вынос в контекст приближённый");

        begin_basic(p, locality, Gender.MALE, mt);
        begin_basic(p, locality, Gender.FEMALE, mt);
    }

    private void begin_basic(PopulationByLocality p, Locality locality, Gender gender, CombinedMortalityTable mt) throws Exception
    {
        for (int age = 0; age < NYEARS; age++)
        {
            double v = p.get(locality, gender, age);

            int nd1 = firstDayForAge(age);
            int nd2 = lastDayForAge(age);

            if (mt == null)
            {
                for (int nd = nd1; nd <= nd2; nd++)
                    setDay(locality, gender, nd, v / DAYS_PER_YEAR);
            }
            else
            {
                double[] dlx = get_daily_lx(mt, locality, gender);
                int offset = 0;

                // если у кривой недостаёт знчений для последнего года
                while (nd2 - offset >= dlx.length)
                    offset++;

                dlx = Util.splice(dlx, nd1 - offset, nd2 - offset);
                dlx = Util.normalize(dlx);

                for (int nd = nd1; nd <= nd2; nd++)
                    setDay(locality, gender, nd, v * dlx[nd - nd1]);
            }
        }
    }

    /*
     * Перемещение завершено
     */
    private void begin_complete(PopulationByLocality p, Locality locality) throws Exception
    {
        begin_complete(p, locality, Gender.MALE);
        begin_complete(p, locality, Gender.FEMALE);
        p.makeBoth(locality);
    }

    private void begin_complete(PopulationByLocality p, Locality locality, Gender gender) throws Exception
    {
        for (int age = 0; age < NYEARS; age++)
            p.set(locality, gender, age, 0);
    }

    private void validate_begin(PopulationByLocality p, PopulationByLocality pto, Locality locality) throws Exception
    {
        validate_begin(p, pto, locality, Gender.MALE);
        validate_begin(p, pto, locality, Gender.FEMALE);
    }

    private void validate_begin(PopulationByLocality p, PopulationByLocality pto, Locality locality, Gender gender) throws Exception
    {
        for (int age = 0; age < NYEARS; age++)
        {
            double p_sum = p.sum(locality, gender, age, age);
            double pto_sum = pto.sum(locality, gender, age, age);
            double moved = p_sum - pto_sum;

            double ctx_sum = sumAge(locality, gender, age);
            if (Math.abs(moved - ctx_sum) > 0.01)
                throw new Exception("ctx.begin validation failed");
        }
    }

    /*
     * Переместить детские ряды из контекста в структуру населения.
     * 
     * Контекст не изменяется и может быть использован для повторных операций. Это позволяет сделать
     * snapshot населения в текущий момент и затем продолжить передвижку.
     * 
     * Возвращает результат добавления. Исходная структура @p не меняется.   
     */
    public Population end(final Population p) throws Exception
    {
        PopulationByLocality xp = new PopulationByLocality(p);
        PopulationByLocality xp2 = end(xp);
        return xp2.forLocality(Locality.TOTAL);
    }

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

        if (pto.hasRuralUrban())
            pto.recalcTotalLocalityFromUrbanRural();

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
            double v = sumDays(locality, gender, firstDayForAge(age), lastDayForAge(age));
            p.add(locality, gender, age, v);
            p.add(locality, Gender.BOTH, age, v);
        }
    }

    /* =============================================================================================== */

    public void clearTotalBirths()
    {
        totalBirths.clear();
    }

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

        String key = key(locality, gender);
        Double v = totalBirths.get(key);
        if (v == null)
            v = 0.0;
        totalBirths.put(key, v + births);
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
        catch (Throwable ex)
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

    /* =============================================================================================== */

    public Population toPopulation() throws Exception
    {
        return toPopulationByLocality().forLocality(Locality.TOTAL);
    }

    public PopulationByLocality toPopulationByLocality() throws Exception
    {
        if (!began)
            throw new Exception("context is idle");

        PopulationByLocality p = null;

        if (this.hasRuralUrban)
        {
            p = PopulationByLocality.newPopulationByLocality();
        }
        else
        {
            p = PopulationByLocality.newPopulationTotalOnly();
        }

        if (this.valueConstraint != null)
            p.setValueConstraint(this.valueConstraint);

        p.zero();
        p = this.end(p);
        return p;
    }

    public void validate() throws Exception
    {
        // currently no-op
    }

    private void checkConforming(PopulationContext cx, ValueConstraint vc) throws Exception
    {
        if (cx.hasRuralUrban != this.hasRuralUrban)
            throw new Exception("разнородные контексты");

        if (vc == null)
        {
            if ((this.valueConstraint != null) != (cx.valueConstraint != null))
                throw new Exception("разнородные контексты");
            if (this.valueConstraint != null && cx.valueConstraint != null && !this.valueConstraint.equals(cx.valueConstraint))
                throw new Exception("разнородные контексты");
        }

        if (this.MAX_DAY != cx.MAX_DAY)
            throw new Exception("разнородные контексты");
    }

    private List<LocalityGender> lgs()
    {
        List<LocalityGender> list = new ArrayList<>();

        if (hasRuralUrban)
        {
            list.add(LocalityGender.URBAN_MALE);
            list.add(LocalityGender.URBAN_FEMALE);

            list.add(LocalityGender.RURAL_MALE);
            list.add(LocalityGender.RURAL_FEMALE);
        }
        else
        {
            list.add(LocalityGender.TOTAL_MALE);
            list.add(LocalityGender.TOTAL_FEMALE);

        }
        return list;
    }

    /* =============================================================================================== */

    /*
     * Simulate population interface
     */
    public void makeBoth()
    {
        // no-op
    }

    public void recalcTotal()
    {
        // no-op
    }

    public void validateBMF()
    {
        // no-op
    }

    /* ---------------------------------------------------------------------------- */

    public double sum(Gender gender, int age1, int age2) throws Exception
    {
        return sum(Locality.TOTAL, gender, age1, age2);
    }

    public double sum(int age1, int age2) throws Exception
    {
        return sum(Locality.TOTAL, Gender.BOTH, age1, age2);
    }

    public double sum(Locality locality) throws Exception
    {
        return sum(locality, Gender.BOTH, 0, MAX_AGE);
    }

    public double sum(Gender gender) throws Exception
    {
        return sum(Locality.TOTAL, gender, 0, MAX_AGE);
    }

    public double sum() throws Exception
    {
        return sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);
    }

    public double sum(Locality locality, Gender gender, int age1, int age2) throws Exception
    {
        return sumDays(locality, gender, firstDayForAge(age1), lastDayForAge(age2));
    }

    /* ---------------------------------------------------------------------------- */

    public PopulationContext sub(PopulationContext p) throws Exception
    {
        return sub(p, (ValueConstraint) null);
    }

    public PopulationContext sub(PopulationContext cx, ValueConstraint vc) throws Exception
    {
        checkConforming(cx, vc);

        PopulationContext c = clone();
        if (vc != null)
            c.setValueConstraint(vc);

        for (LocalityGender lg : lgs())
        {
            double[] v = c.asArray(lg.locality, lg.gender);
            double[] vx = cx.asArray(lg.locality, lg.gender);
            if (v.length != vx.length)
                throw new IllegalArgumentException("разнородные контексты");

            for (int nd = 0; nd < v.length; nd++)
                v[nd] -= vx[nd];

            c.fromArray(lg.locality, lg.gender, v);
        }

        return c;
    }

    /* ---------------------------------------------------------------------------- */

    public PopulationContext add(PopulationContext p) throws Exception
    {
        return add(p, (ValueConstraint) null);
    }

    public PopulationContext add(PopulationContext cx, ValueConstraint vc) throws Exception
    {
        checkConforming(cx, vc);

        PopulationContext c = clone();
        if (vc != null)
            c.setValueConstraint(vc);

        for (LocalityGender lg : lgs())
        {
            double[] v = c.asArray(lg.locality, lg.gender);
            double[] vx = cx.asArray(lg.locality, lg.gender);
            if (v.length != vx.length)
                throw new IllegalArgumentException("разнородные контексты");

            for (int nd = 0; nd < v.length; nd++)
                v[nd] += vx[nd];

            c.fromArray(lg.locality, lg.gender, v);
        }

        return c;
    }

    /* ---------------------------------------------------------------------------- */

    public PopulationContext scaleAllBy(double scale) throws Exception
    {
        PopulationContext c = clone();

        for (LocalityGender lg : lgs())
        {
            double[] v = c.asArray(lg.locality, lg.gender);

            for (int nd = 0; nd < v.length; nd++)
                v[nd] *= scale;

            c.fromArray(lg.locality, lg.gender, v);
        }

        return c;
    }

    /* ---------------------------------------------------------------------------- */

    public PopulationContext avg(PopulationContext p) throws Exception
    {
        return avg(p, null);
    }

    public PopulationContext avg(PopulationContext p, ValueConstraint rvc) throws Exception
    {
        PopulationContext p2 = this.add(p, rvc);
        p2 = RescalePopulation.scaleAllBy(p2, 0.5);
        return p2;
    }

    /* ---------------------------------------------------------------------------- */

    /*
     * Сдвинуть возрастное распределение на @years лет вниз
     */
    public PopulationContext moveDown(double years) throws Exception
    {
        return moveDownByDays((int) Math.round(years * DAYS_PER_YEAR));
    }

    public PopulationContext moveDownByDays(int ndays) throws Exception
    {
        PopulationContext c = clone();

        for (LocalityGender lg : lgs())
        {
            double[] v = c.asArray(lg.locality, lg.gender);
            double[] v2 = new double[v.length];

            for (int nd = ndays; nd < v.length; nd++)
                v2[nd - ndays] = v[nd];

            c.fromArray(lg.locality, lg.gender, v2);
        }

        return c;
    }

    /* ---------------------------------------------------------------------------- */

    /*
     * Выборка [age1 ... age2] или [ageday1 ... ageday2] .
     * 
     * Нецелое значение года означает, что население выбирается только от/до этой возрастной точки.
     * Так age2 = 80.0 означает, что население с возраста 80.0 лет исключено. 
     * Аналогично, age2 = 80.5 означает, что включена половина населения в возрасте 80 лет,
     * а население начиная с возраста 81 года исключено целиком. 
     */
    public PopulationContext selectByAgeYears(double age1, double age2) throws Exception
    {
        int ageday1 = (int) Math.round(age1 * DAYS_PER_YEAR);
        int ageday2 = (int) Math.round(age2 * DAYS_PER_YEAR);
        return selectByAgeDays(ageday1, ageday2);
    }

    public PopulationContext selectByAgeDays(int ageday1, int ageday2) throws Exception
    {
        PopulationContext c = clone();

        for (LocalityGender lg : lgs())
        {
            double[] v = c.asArray(lg.locality, lg.gender);

            for (int nd = 0; nd < v.length; nd++)
            {
                if (nd >= ageday1 && nd <= ageday2)
                {
                    // leave alone
                }
                else
                {
                    v[nd] = 0;
                }
            }

            c.fromArray(lg.locality, lg.gender, v);
        }

        return c;
    }
}
