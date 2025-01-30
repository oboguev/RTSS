package rtss.ww2losses.ageline;

import java.util.HashMap;
import java.util.Map;

import rtss.data.selectors.Gender;

/*
 * Map (Gender, nd_age) -> loss intensity
 */
public class AgeLineLossIntensities 
{
    private Map<String,Double> m = new HashMap<>();
    
    private String key(Gender gender, int nd)
    {
        return gender.name() + "." + nd;
    }
    
    public Double get(Gender gender, int nd)
    {
        return m.get(key(gender, nd));
    }

    public void set(Gender gender, int nd, double v)
    {
        m.put(key(gender, nd), v);
    }
}
