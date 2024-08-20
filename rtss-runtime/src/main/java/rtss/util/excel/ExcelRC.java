package rtss.util.excel;

import java.util.ArrayList;
import java.util.List;

import rtss.util.Util;

/**
 * Excel sheet values (RC or row-column style).
 * Row-wise first (rows of columns).
 */
public class ExcelRC extends ArrayList<List<Object>>
{
    private static final long serialVersionUID = 1L;

    public Object get(int row, int col)
    {
        if (row >= size())
            return null;
        List<Object> r = get(row);
        if (col >= r.size())
            return null;
        return r.get(col);
    }

    /* =================================================================== */
    
    public boolean isEmpty(int nr, int nc) throws Exception
    {
        String s = asString(nr, nc);
        return s == null || s.length() == 0;
    }

    /* =================================================================== */

    public String asString(int nr, int nc) throws Exception
    {
        return asString(get(nr, nc));
    }

    public static String asString(Object o) throws Exception
    {
        if (o == null)
            return null;
        String s = o.toString();
        s = Util.despace(s).trim();
        return s;
    }

    /* =================================================================== */

    public Double asDouble(int nr, int nc) throws Exception
    {
        return asDouble(get(nr, nc));
    }

    public static Double asDouble(Object o) throws Exception
    {
        if (o == null)
        {
            return null;
        }
        else if (o instanceof Double)
        {
            return (Double) o;
        }
        else if (o instanceof Long)
        {
            return ((Long) o).doubleValue();
        }
        else if (o instanceof Integer)
        {
            return ((Integer) o).doubleValue();
        }
        else if (o instanceof String)
        {
            String so = o.toString();
            so = Util.despace(so).trim();
            if (so.equals(""))
                return null;
            so = so.replace(",", "");
            return Double.parseDouble(so);
        }
        else
        {
            throw new Exception("Invalid cell data type (for expected Double)");
        }
    }

    public double asRequiredDouble(int nr, int nc) throws Exception
    {
        return asRequiredDouble(get(nr, nc));
    }

    public static double asRequiredDouble(Object o) throws Exception
    {
        Double v = asDouble(o);
        if (v == null)
            throw new Exception("Missing double cell value");
        return v;
    }

    /* =================================================================== */

    public long asLong(int nr, int nc) throws Exception
    {
        return asLong(get(nr, nc));
    }

    public static Long asLong(Object o) throws Exception
    {
        if (o == null)
        {
            return null;
        }
        else if (o instanceof Long)
        {
            return ((Long) o).longValue();
        }
        else if (o instanceof Integer)
        {
            return ((Integer) o).longValue();
        }
        else if (o instanceof String)
        {
            String so = o.toString();
            so = Util.despace(so).trim();
            if (so.equals(""))
                return null;
            so = so.replace(",", "");
            return Long.parseLong(so);
        }
        else if (o instanceof Double)
        {
            double dv = (Double) o;
            long v = Math.round(dv);
            if (dv - v != 0)
                throw new Exception("Expected cell value: long/integer, actual: double");
            return v;
        }
        else
        {
            throw new Exception("Invalid cell data type (for expected Long)");
        }
    }

    public long asRequiredLong(int nr, int nc) throws Exception
    {
        return asRequiredLong(get(nr, nc));
    }

    public static long asRequiredLong(Object o) throws Exception
    {
        Long v = asLong (o);
        if (v == null)
            throw new Exception("Missing long cell value");
        return v;
    }

    /* =================================================================== */

    public int asInt(int nr, int nc) throws Exception
    {
        return asInt(get(nr, nc));
    }

    public static Integer asInt(Object o) throws Exception
    {
        if (o == null)
        {
            return null;
        }
        else if (o instanceof Long)
        {
            return ((Long) o).intValue();
        }
        else if (o instanceof Integer)
        {
            return ((Integer) o).intValue();
        }
        else if (o instanceof String)
        {
            String so = o.toString();
            so = Util.despace(so).trim();
            if (so.equals(""))
                return null;
            so = so.replace(",", "");
            return Integer.parseInt(so);
        }
        else if (o instanceof Double)
        {
            double dv = (Double) o;
            long v = Math.round(dv);
            if (dv - v != 0)
                throw new Exception("Expected cell value: long/integer, actual: double");
            return (int) v;
        }
        else
        {
            throw new Exception("Invalid cell data type (for expected Integer)");
        }
    }

    public int asRequiredInt(int nr, int nc) throws Exception
    {
        return asRequiredInt(get(nr, nc));
    }

    public static int asRequiredInt(Object o) throws Exception
    {
        Integer v = asInt(o);
        if (v == null)
            throw new Exception("Missing long cell value");
        return v;
    }

    /* =================================================================== */

    public boolean isEndRow(int nr) throws Exception
    {
        return isEndRow(nr, 0) || isEndRow(nr, 1); 
    }

    public boolean isEndRow(int nr, int nc) throws Exception
    {
        Object o = get(nr, nc);

        if (o != null)
        {
            String s = o.toString();
            s = Util.despace(s).trim();
            if (s.contains("note"))
                return true;
        }
        
        return false; 
    }
}
