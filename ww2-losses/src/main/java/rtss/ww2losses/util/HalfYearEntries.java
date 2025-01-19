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
        FirstHalfYear, SecondHalfYear;

        public String toString()
        {
            switch (this)
            {
            case FirstHalfYear:
                return "1";
            case SecondHalfYear:
                return "2";
            default:
                return "x";
            }
        }

        public static HalfYearSelector fromString(String s) throws Exception
        {
            if (s.equals("1"))
                return FirstHalfYear;
            else if (s.equals("2"))
                return SecondHalfYear;
            else
                throw new IllegalArgumentException("invalid selector value: " + s);
        }
    }

    private int base_year = 1941;

    public T get(int year, HalfYearSelector half) throws Exception
    {
        return get(index(year, half));
    }

    public T get(String s) throws Exception
    {
        return get(index(s));
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

    private int index(String s) throws Exception
    {
        String[] sa = s.split("\\.");
        if (sa.length != 2)
            throw new IllegalArgumentException("invalid selector value: " + s);
        int year = Integer.parseInt(sa[0]);
        HalfYearSelector half = HalfYearSelector.fromString(sa[1]);
        return index(year, half);
    }

    public T last()
    {
        return get(size() - 1);
    }
}
