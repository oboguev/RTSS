package rtss.ww2losses.util;

import java.util.ArrayList;

/*
 * Маассив индексированный по полугодиям с середины 1941
 */
public class HalfYearEntries<T> extends ArrayList<T> 
{
    private static final long serialVersionUID = 1L;
    
    public static enum HalfYearSelector
    {
        FirstHalfYear,
        SecondHalfYear 
    }
    
    private int base_year = 1941;
    
    public T get(int year, HalfYearSelector half) throws Exception
    {
        return get(index(year, half));
    }

    public void set(int year, HalfYearSelector half, T value) throws Exception
    {
        set(index(year, half), value);
    }
    
    private int index(int year, HalfYearSelector half) throws Exception
    {
        int ix;
        switch (half)
        {
        case FirstHalfYear:
            ix = 0;
            break;

        case SecondHalfYear:
            ix = 1;
            break;
        
        default:
            throw new Exception("incorrect half-year index");
        }
        
        return (year - base_year) * 2 + ix;
    }
    
    public T last()
    {
        return get(size() - 1);
    }
}
