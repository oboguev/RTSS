package rtss.pre1917;

import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import rtss.pre1917.data.ColumnHeader;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryNames;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.validate.CrossVerify;
import rtss.util.Util;
import rtss.util.excel.Excel;
import rtss.util.excel.ExcelRC;

public class LoadData
{
    public static void main(String[] args)
    {
        try
        {
            new LoadData().loadAllData();
            // TerritoryNames.printSeen();
            Util.out("** Done");
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private TerritoryDataSet territories = new TerritoryDataSet();
    private Integer currentFileYear = null;

    public TerritoryDataSet loadAllData() throws Exception
    {
        loadUGVI("1891");
        loadUGVI("1892");
        loadUGVI("1893-1895", 1893, 1895);
        loadUGVI("1896-1901", 1896, 1901);
        loadUGVI("1902");
        loadUGVI("1903");
        loadUGVI("1904");
        loadUGVI("1905");
        loadUGVI("1906");
        loadUGVI("1907");
        loadUGVI("1908");
        loadUGVI("1909");
        loadUGVI("1910");
        loadUGVI("1911");
        loadUGVI("1912");
        loadUGVI("1913");
        loadUGVI("1914");

        new CrossVerify().verify(territories);

        return territories;
    }

    private void loadUGVI(String fn) throws Exception
    {
        currentFileYear = Integer.parseInt(fn);

        String fpath = String.format("ugvi/%s.xlsx", fn);

        try (XSSFWorkbook wb = Excel.loadWorkbook(fpath);)
        {
            for (int k = 0; k < wb.getNumberOfSheets(); k++)
            {
                XSSFSheet sheet = wb.getSheetAt(k);
                String sname = sheet.getSheetName();
                if (sname != null && sname.trim().toLowerCase().contains("note"))
                    continue;

                ExcelRC rc = Excel.readSheet(wb, sheet, fpath);
                Map<String, Integer> headers = ColumnHeader.getTopHeaders(sheet, rc);
                validateHeaders(headers);

                if (!headers.containsKey("губ"))
                    throw new Exception("Нет колонки для губернии");
                int gcol = headers.get("губ");
                scanGubColumn(rc, gcol);

                // scan column "key yyyy" 
                scanYearColumn(rc, gcol, headers, "р");
                scanYearColumn(rc, gcol, headers, "с");
                scanYearColumn(rc, gcol, headers, "п");
                scanYearColumn(rc, gcol, headers, "чж");
                scanYearColumn(rc, gcol, headers, "чр");
                scanYearColumn(rc, gcol, headers, "чу");

                scanYearColumn(rc, gcol, headers, "чж-гор-м ");
                scanYearColumn(rc, gcol, headers, "чж-гор-ж ");
                scanYearColumn(rc, gcol, headers, "чр-гор-м ");
                scanYearColumn(rc, gcol, headers, "чр-гор-ж ");
                scanYearColumn(rc, gcol, headers, "чс-гор-м ");
                scanYearColumn(rc, gcol, headers, "чс-гор-ж ");
                scanYearColumn(rc, gcol, headers, "чж-уез-м ");
                scanYearColumn(rc, gcol, headers, "чж-уез-ж ");
                scanYearColumn(rc, gcol, headers, "чр-уез-м ");
                scanYearColumn(rc, gcol, headers, "чр-уез-ж ");
                scanYearColumn(rc, gcol, headers, "чс-уез-м ");
                scanYearColumn(rc, gcol, headers, "чс-уез-ж ");
            }
        }

        currentFileYear = null;
    }

    private void loadUGVI(String fn, int y1, int y2) throws Exception
    {
        String fpath = String.format("ugvi/%s.xlsx", fn);

        try (XSSFWorkbook wb = Excel.loadWorkbook(fpath);)
        {
            for (int k = 0; k < wb.getNumberOfSheets(); k++)
            {
                XSSFSheet sheet = wb.getSheetAt(k);
                String sname = sheet.getSheetName();
                if (sname != null && sname.trim().toLowerCase().contains("note"))
                    continue;

                ExcelRC rc = Excel.readSheet(wb, sheet, fpath);
                Map<String, Integer> headers = ColumnHeader.getTopHeaders(sheet, rc);

                if (!headers.containsKey("губ"))
                    throw new Exception("Нет колонки для губернии");
                if (!headers.containsKey("год"))
                    throw new Exception("Нет колонки для год");

                int gcol = headers.get("губ");
                int ycol = headers.get("год");
                scanGubColumn(rc, gcol);
                scanMultiYearColumn(rc, gcol, ycol, headers, "р");
                scanMultiYearColumn(rc, gcol, ycol, headers, "с");
                scanMultiYearColumn(rc, gcol, ycol, headers, "чж в сл. году");
                scanMultiYearColumn(rc, gcol, ycol, headers, "чр");
                scanMultiYearColumn(rc, gcol, ycol, headers, "чу");
            }
        }
    }

    private void validateHeaders(Map<String, Integer> headers) throws Exception
    {
        for (String h : headers.keySet())
        {
            switch (h)
            {
            case "губ":
            case "чр":
            case "чу":
            case "р":
            case "с":
            case "чж в сл. году":
                continue;
            default:
                break;
            }

            if (h.startsWith("v") ||
                h.startsWith("р ") ||
                h.startsWith("с ") ||
                h.startsWith("п ") ||
                h.startsWith("чж ") ||
                h.startsWith("чр ") ||
                h.startsWith("чу ") ||
                h.startsWith("чж-гор-м ") ||
                h.startsWith("чж-гор-ж ") ||
                h.startsWith("чр-гор-м ") ||
                h.startsWith("чр-гор-ж ") ||
                h.startsWith("чс-гор-м ") ||
                h.startsWith("чс-гор-ж ") ||
                h.startsWith("чж-уез-м ") ||
                h.startsWith("чж-уез-ж ") ||
                h.startsWith("чр-уез-м ") ||
                h.startsWith("чр-уез-ж ") ||
                h.startsWith("чс-уез-м ") ||
                h.startsWith("чс-уез-ж "))
            {
                continue;
            }

            throw new Exception("Incorrect header in Excel file");
        }
    }

    private void scanGubColumn(ExcelRC rc, int gcol) throws Exception
    {
        for (int nr = 1; nr < rc.size(); nr++)
        {
            Object o = rc.get(nr, gcol);
            if (o == null)
                o = "";
            String gub = o.toString();
            gub = TerritoryNames.canonic(gub);
        }
    }

    private void scanYearColumn(ExcelRC rc, int gcol, Map<String, Integer> headers, String what) throws Exception
    {
        for (String h : headers.keySet())
        {
            if (h.startsWith(what + " "))
            {
                String ys = h.substring(what.length() + 1);

                int year = -1;
                
                if (ys.equals("YY") && currentFileYear != null)
                {
                    year = currentFileYear;
                }
                else if (ys.equals("NYY") && currentFileYear != null)
                {
                    year = currentFileYear + 1;
                }
                else if (ys.length() == 4)
                {
                    year = Integer.parseInt(ys);
                }

                if (year > 0)
                {
                    scanYearColumn(rc, gcol, what, year, headers.get(h));
                }
            }
        }
    }

    private void scanYearColumn(ExcelRC rc, int gcol, String what, int year, int wcol) throws Exception
    {
        for (int nr = 1; nr < rc.size(); nr++)
        {
            Object o = rc.get(nr, gcol);
            if (o == null)
                o = "";
            String gub = o.toString();
            gub = TerritoryNames.canonic(gub);
            if (gub.length() != 0)
            {
                o = rc.get(nr, wcol);
                setValue(gub, year, what, o);
            }
        }
    }

    private void scanMultiYearColumn(ExcelRC rc, int gcol, int ycol, Map<String, Integer> headers, String what) throws Exception
    {
        if (!headers.containsKey(what))
            return;
        int wcol = headers.get(what);

        String gub = null;

        double avg_sum = 0;
        double avg_count = 0;

        for (int nr = 1; nr < rc.size(); nr++)
        {
            // губ
            Object o = rc.get(nr, gcol);
            if (o == null)
                o = "";
            String xgub = o.toString();
            xgub = TerritoryNames.canonic(xgub);
            if (xgub.length() != 0)
                gub = xgub;

            // год
            o = rc.get(nr, ycol);
            if (o == null)
                o = "";
            int year;
            if (o.toString().trim().equals(""))
            {
                continue;
            }
            else if (o.toString().trim().equals("сред"))
            {
                year = -1;
            }
            else
            {
                year = (int) (long) asLong(o);
            }

            // what-value
            o = rc.get(nr, wcol);
            if (o == null)
                o = "";
            switch (o.toString().trim())
            {
            case "":
            case "-":
                continue;
            default:
                break;

            }

            String xwhat = what;
            if (what.equals("чж в сл. году") && year != -1)
            {
                xwhat = "чж";
                year += 1;
            }

            if (typeof(xwhat) == Long.class)
            {
                long v = asLong(o);
                if (year >= 0)
                {
                    territoryYear(gub, year).setValue(xwhat, v);
                    avg_sum += v;
                    avg_count++;
                }
                else
                {
                    double avg = avg_sum / avg_count;
                    if (Util.False && Math.abs(avg - v) > 1)
                        throw new Exception("Averages differ");
                }
            }
            else if (typeof(xwhat) == Double.class)
            {
                double v = asDouble(o);
                if (year >= 0)
                {
                    territoryYear(gub, year).setValue(xwhat, v);
                    avg_sum += v;
                    avg_count++;
                }
                else
                {
                    double avg = avg_sum / avg_count;
                    if (Util.False && Math.abs(avg - v) > 0.1)
                        throw new Exception("Averages differ");
                }
            }
        }
    }

    private Class<?> typeof(String what) throws Exception
    {
        switch (what)
        {
        case "чж в сл. году":
        case "чж":
        case "чр":
        case "чу":
        case "чж-гор-м ":
        case "чж-гор-ж ":
        case "чр-гор-м ":
        case "чр-гор-ж ":
        case "чс-гор-м ":
        case "чс-гор-ж ":
        case "чж-уез-м ":
        case "чж-уез-ж ":
        case "чр-уез-м ":
        case "чр-уез-ж ":
        case "чс-уез-м ":
        case "чс-уез-ж ":
            return Long.class;

        case "р":
        case "с":
        case "п":
            return Double.class;

        default:
            throw new Exception("Invalid selector");
        }
    }

    private void setValue(String gub, int year, String what, Object o) throws Exception
    {
        if (o == null)
            return;

        String so = o.toString();
        so = Util.despace(so).trim();
        if (so.length() == 0)
            return;

        if (so.equals("-"))
            return;

        if (typeof(what) == Double.class)
        {
            territoryYear(gub, year).setValue(what, asDouble(o));
        }
        else if (typeof(what) == Long.class)
        {
            territoryYear(gub, year).setValue(what, asLong(o));
        }
    }

    private double asDouble(Object o) throws Exception
    {
        if (o instanceof Double)
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

    private Long asLong(Object o) throws Exception
    {
        if (o instanceof Long)
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

    private TerritoryYear territoryYear(String gub, int year)
    {
        Territory t = territories.get(gub);

        if (t == null)
        {
            t = new Territory(gub);
            territories.put(gub, t);
        }

        return t.territoryYear(year);
    }
}
