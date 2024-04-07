package rtss.data.population.forward;

import java.util.HashMap;
import java.util.Map;

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
 */
public class PopulationForwardingContext
{
    public static final int DAYS_PER_YEAR = 365;

    public static final int NYEARS = 4;
    public static final int NDAYS = NYEARS * DAYS_PER_YEAR;
    public static final int MAX_DAY = NDAYS - 1;
    
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
        double sum = 0;
        for (int nd = nd1; nd <= nd2; nd++)
            sum += get(locality, gender, nd);
        return sum;
    }

    private String key(Locality locality, Gender gender, int day) throws Exception
    {
        if (day < 0 || day > MAX_DAY)
            throw new IllegalArgumentException();
        return locality.name() + "-" + gender.name() + "-" + day;
    }

    /* =============================================================================================== */
}
