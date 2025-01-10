package rtss.util;

import java.lang.reflect.Field;

/*
 * Access (get/set) object fields and sub-fileds via their names, using dotted notation
 */
public class FieldValue
{
    public static Long getLong(Object o, String selector) throws Exception
    {
        String[] sa = selector.split("\\.");
        
        for (String s : sa)
        {
            Field field = o.getClass().getDeclaredField(s);    
            field.setAccessible(true);
            o = field.get(o);            
        }

        return (Long) o;
    }

    public static void setLong(Object o, String selector, Long value) throws Exception
    {
        String[] sa = selector.split("\\.");
        
        for (int k = 0; k < sa.length; k++)
        {
            String s = sa[k];
            
            Field field = o.getClass().getDeclaredField(s);    
            field.setAccessible(true);

            if (k == sa.length - 1)
            {
                field.set(o, value);
            }
            else
            {
                o = field.get(o);            
            }
        }
    }

    public static Double getDouble(Object o, String selector) throws Exception
    {
        String[] sa = selector.split("\\.");
        
        for (String s : sa)
        {
            Field field = o.getClass().getDeclaredField(s);    
            field.setAccessible(true);
            o = field.get(o);            
        }

        return (Double) o;
    }

    public static void setDouble(Object o, String selector, Double value) throws Exception
    {
        String[] sa = selector.split("\\.");
        
        for (int k = 0; k < sa.length; k++)
        {
            String s = sa[k];
            
            Field field = o.getClass().getDeclaredField(s);    
            field.setAccessible(true);

            if (k == sa.length - 1)
            {
                field.set(o, value);
            }
            else
            {
                o = field.get(o);            
            }
        }
    }
}
