package rtss.ww2losses.util;

import java.util.ArrayList;

/*
 * Маассив индексированный по полугодиям с середины 1941
 */
public class HalfYearEntries<T> extends ArrayList<T> 
{
    private static final long serialVersionUID = 1L;
    
    public static final int StartOfFirstHalfYear = 1;
    public static final int StartOfSecondHalfYear = 2;
    
    private int base_year = 1941;
    
    public T get(int year, int half) throws Exception
    {
        return get(index(year, half));
    }

    public void set(int year, int half, T value) throws Exception
    {
        set(index(year, half), value);
    }
    
    private int index(int year, int half) throws Exception
    {
        if (half != StartOfFirstHalfYear && half != StartOfSecondHalfYear)
            throw new Exception("incorrect half-year index");
        
        half -= 1;
        
        return (year - base_year) * 2 + (half - StartOfFirstHalfYear) - 1;
    }
}
