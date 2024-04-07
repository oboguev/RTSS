package rtss.data.population.forward;

import java.util.HashMap;
import java.util.Map;

import rtss.data.population.PopulationByLocality;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;

/**
 * Применяется для учёта населения самых младших возрастов при продвижке.
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
 * Индексация по: (Locality, Gender, ndays), где ndays – число дней прошедших со дня рождения до текущего момента.
 *   
 * Хранимые числа населения уже были подвергнуты влиянию смертности (и соответствующей числовой децимации) и представляют 
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
 *     pto = fctx.end(pto);
 * 
 */
public class PopulationForwardingContext
{
    public static final int DAYS_PER_YEAR = 365;

    public static final int NYEARS = 4;
    public static final int NDAYS = NYEARS * DAYS_PER_YEAR;
    public static final int MAX_DAY = NDAYS - 1;
    
    private boolean hasRuralUrban;
    
    /* =============================================================================================== */
    
    private Map<String, Double> m = new HashMap<>(); 
    
    public double get(Locality locality, Gender gender, int day) throws Exception
    {
        String key = key(locality, gender, day);
        Double d = m.get(key);
        return d != null ? d : 0;
    }
    
    public void set(Locality locality, Gender gender, int day, double v) throws Exception
    {
        String key = key(locality, gender, day);
        m.put(key, v);
    }

    public double add(Locality locality, Gender gender, int day, double v) throws Exception
    {
        String key = key(locality, gender, day);
        Double d = m.get(key);
        if (d == null)
            d = 0.0;
        v += d;
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

    private String key(Locality locality, Gender gender, int day) throws Exception
    {
        if (day < 0 || day > MAX_DAY || gender == Gender.BOTH)
            throw new IllegalArgumentException();
        return locality.name() + "-" + gender.name() + "-" + day;
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
        
        hasRuralUrban = pto.hasRuralUrban(); 
        if (hasRuralUrban)
        {
            begin(pto, Locality.RURAL);
            begin(pto, Locality.URBAN);
        }
        
        if (pto.hasTotal())
        {
            begin(pto, Locality.TOTAL);
        }
        
        pto.resetTotal();
        pto.validate();
        
        return pto;
    }

    private void begin(PopulationByLocality p, Locality locality) throws Exception
    {
        begin(p, locality, Gender.MALE);
        begin(p, locality, Gender.FEMALE);
    }

    private void begin(PopulationByLocality p, Locality locality, Gender gender) throws Exception
    {
        for (int age = 0; age < NYEARS; age++)
        {
            double v = p.get(locality, gender, age);
            p.set(locality, gender, age, 0);

            int nd1 = age * DAYS_PER_YEAR;
            int nd2 = nd1 + DAYS_PER_YEAR - 1;
            for (int nd = nd1; nd <= nd2; nd++)
                set(locality, gender, nd, v / DAYS_PER_YEAR);
        }
    }
    
    /*
     * Переместить детские ряды из контекста в структуру населения  
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
        
        pto.resetTotal();
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
            int nd1 = age * DAYS_PER_YEAR;
            int nd2 = nd1 + DAYS_PER_YEAR - 1;
            double v = sum(locality, gender, nd1, nd2);
            p.add(locality, gender, age, v);
            p.add(locality, Gender.BOTH, age, v);
        }
    }
}
