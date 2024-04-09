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
 * Применяется для детального учёта населения самых младших возрастов при продвижке, чтобы точнее учесть рождения и 
 * ранюю детскую смертность. 
 * 
 * Смертность в этих возрастах изменяется резко, поэтому для учёта населения при последовательных шагах продвижки,
 * особенно если некоторые шаги имеют продолжительность менее года, требуется более детальная разбивка численности
 * населения этих возрастных групп, нежели по году возраста.
 * 
 * Структуры Population и PopulationByLocality индексируют население тольrо по годам возраста, и не дают возможности
 * учёта с более детальным временны́м разрешением.  
 * 
 * Структура PopulationForwardingContext, в противоположность, хранит численность населения с возрастом индексированным 
 * в днях (а не годах) с даты рождения до текущего момента.
 * 
 * Индексация производится по (Locality, Gender, ndays), где ndays – число дней прошедших со дня рождения до текущего момента.
 *   
 * Хранимые числа населения уже подвергнуты влиянию смертности (и соответствующей числовой децимации) и представляют 
 * числа доживших до данного момента.
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
 *     pto = forward(pto, fctx, mt, yfraction) <== повторяемое сколько нужно
 *     ....
 *     pto = fctx.end(pto);
 * 
 */
public class PopulationForwardingContext
{
    public final int DAYS_PER_YEAR = 365;

    public final int NYEARS = 4;
    public final int MAX_YEAR = NYEARS - 1;

    public final int NDAYS = NYEARS * DAYS_PER_YEAR;
    public final int MAX_DAY = NDAYS - 1;
    
    private boolean hasRuralUrban;
    private Map<String, Double> m = new HashMap<>();
    
    /* =============================================================================================== */
    
    public PopulationForwardingContext clone()
    {
        PopulationForwardingContext cx = new PopulationForwardingContext();
        cx.hasRuralUrban = hasRuralUrban;
        cx.m = new HashMap<String, Double>(m);
        cx.m_lx = new HashMap<String, double[]>(m_lx); 
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

    private String key(Locality locality, Gender gender, int day) throws Exception
    {
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
        m.clear();
        
        PopulationByLocality pto = p.clone();
        
        hasRuralUrban = p.hasRuralUrban(); 
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
     */
    public PopulationByLocality end(final PopulationByLocality p) throws Exception
    {
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
}
