package rtss.util.excel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import rtss.util.Util;

public class ExcelRow
{
    private Map<String, Object> cells = new HashMap<>();
    private final ExcelSheet sheet;
    private final int nr;

    public ExcelRow(ExcelSheet sheet, int nr)
    {
        this.sheet = sheet;
        this.nr = nr;
    }

    public int getRowNumber()
    {
        return nr;
    }

    public void setCellValue(String column, Object value)
    {
        cells.put(column, value);
    }

    public Set<String> getColumns()
    {
        return sheet.getColumns();
    }

    public Object getCellValue(String column)
    {
        return cells.get(column);
    }

    /* ========================================================== */

    public Double asDouble(String column) throws Exception
    {
        Object o = getCellValue(column);

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
            return Double.parseDouble(so);
        }
        else
        {
            throw new Exception("Invalid cell data type (for expected Double)");
        }
    }

    public Integer asInteger(String column) throws Exception
    {
        Long v = asLong(column);

        if (v == null)
            return null;
        else
            return (int) (long) v;
    }

    public Long asLong(String column) throws Exception
    {
        Object o = getCellValue(column);

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

    public Long asLongThousands(String column) throws Exception
    {
        Double v = asDouble(column);

        if (v == null)
            return null;
        else
            return Math.round(1000 * v);
    }

    public Double asPercent(String column) throws Exception
    {
        Double v = asDouble(column);

        if (v == null)
            return null;
        else if (v < 0 || v > 100)
            throw new Exception("percentage out of range");
        return v;
    }
    
    public String asString(String column) throws Exception
    {
        Object o = getCellValue(column);

        if (o == null)
            return null;
        else
            return o.toString();
    }

    public String asDespacedString(String column) throws Exception
    {
        Object o = getCellValue(column);

        if (o == null)
            return null;
        else
            return Util.despace(o.toString()).trim(); 
    }
}
