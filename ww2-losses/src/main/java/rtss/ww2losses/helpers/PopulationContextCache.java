package rtss.ww2losses.helpers;

import java.util.HashMap;
import java.util.Map;

import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Area;

/*
 * Кешировать результаты расчёта PopulationContext,
 * чтобы избегать ненужных повторяющихся пересчётов 
 */
public class PopulationContextCache
{
    private static Map<String, PopulationContext> m = new HashMap<>();

    private static String key(Area area, String name)
    {
        return area.name() + "." + name;
    }

    public interface ComputePopulationContext
    {
        public PopulationContext compute() throws Exception;
    }

    public static void clear()
    {
        synchronized (m)
        {
            m.clear();
        }
    }

    public static void remove(Area area, String name)
    {
        synchronized (m)
        {
            m.remove(key(area, name));
        }
    }

    public static PopulationContext get(Area area, String name)
    {
        PopulationContext p;
        
        synchronized (m)
        {
            p = m.get(key(area, name));
        }
        
        if (p != null)
            p = p.clone();
        
        return p;
    }

    public static synchronized PopulationContext get(Area area, String name, ComputePopulationContext compute) throws Exception
    {
        PopulationContext p ;
        
        synchronized (m)
        {
            p = m.get(key(area, name));

            if (p == null)
            {
                p = compute.compute();
                m.put(key(area, name), p);
            }
        }

        return p.clone();
    }
}
