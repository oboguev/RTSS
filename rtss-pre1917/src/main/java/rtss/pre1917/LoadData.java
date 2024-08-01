package rtss.pre1917;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import rtss.pre1917.data.ColumnHeader;
import rtss.pre1917.data.RC;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryNames;
import rtss.pre1917.data.TerritoryYear;
import rtss.util.Util;
import rtss.util.excel.Excel;

public class LoadData
{
    public static void main(String[] args)
    {
        try
        {
            new LoadData().loadAllData();
            TerritoryNames.printSeen();
            Util.out("** Done");
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }
    
    private Map<String, Territory> territories = new HashMap<>();

    public Map<String, Territory> loadAllData() throws Exception
    {
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
        
        return territories;
    }

    private void loadUGVI(String fn) throws Exception
    {
        String fpath = String.format("ugvi/%s.xlsx", fn);

        try (XSSFWorkbook wb = Excel.loadWorkbook(fpath);)
        {
            for (int k = 0; k < wb.getNumberOfSheets(); k++)
            {
                XSSFSheet sheet = wb.getSheetAt(k);
                List<List<Object>> rc = Excel.readSheet(wb, sheet, fpath);
                Map<String, Integer> headers = ColumnHeader.getTopHeaders(sheet, rc);
                validateHeaders(headers);

                if (!headers.containsKey("губ"))
                    throw new Exception("Нет колоники для губернии");
                int gcol = headers.get("губ");
                scanGubColumn(rc, gcol);

                // scan column "key yyyy" 
                scanYearColumn(rc, gcol, headers, "р");
                scanYearColumn(rc, gcol, headers, "с");
                scanYearColumn(rc, gcol, headers, "п");
                scanYearColumn(rc, gcol, headers, "чж");
                scanYearColumn(rc, gcol, headers, "чр");
                scanYearColumn(rc, gcol, headers, "чу");
            }
        }
    }

    private void loadUGVI(String fn, int y1, int y2) throws Exception
    {
        String fpath = String.format("ugvi/%s.xlsx", fn);

        try (XSSFWorkbook wb = Excel.loadWorkbook(fpath);)
        {
            for (int k = 0; k < wb.getNumberOfSheets(); k++)
            {
                XSSFSheet sheet = wb.getSheetAt(k);
                List<List<Object>> rc = Excel.readSheet(wb, sheet, fpath);
                Map<String, Integer> headers = ColumnHeader.getTopHeaders(sheet, rc);

                if (!headers.containsKey("губ"))
                    throw new Exception("Нет колоники для губернии");
                int gc = headers.get("губ");
                scanGubColumn(rc, gc);
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

            if (h.startsWith("р ") ||
                h.startsWith("с ") ||
                h.startsWith("п ") ||
                h.startsWith("чж ") ||
                h.startsWith("чр ") ||
                h.startsWith("чу "))
            {
                continue;
            }

            throw new Exception("Incorrect header in Excel file");
        }
    }

    private void scanGubColumn(List<List<Object>> rc, int gcol) throws Exception
    {
        for (int nr = 1; nr < rc.size(); nr++)
        {
            Object o = RC.get(rc, nr, gcol);
            if (o == null)
                o = "";
            String gub = o.toString();
            gub = TerritoryNames.canonic(gub);
        }
    }

    private void scanYearColumn(List<List<Object>> rc, int gcol, Map<String, Integer> headers, String what) throws Exception
    {
        for (String h : headers.keySet())
        {
            if (h.startsWith(what + " ") && h.length() == what.length() + 5)
            {
                int year = Integer.parseInt(h.substring(what.length() + 1));
                scanYearColumn(rc, gcol, what, year, headers.get(h));
            }
        }
    }

    private void scanYearColumn(List<List<Object>> rc, int gcol, String what, int year, int wcol) throws Exception
    {
        for (int nr = 1; nr < rc.size(); nr++)
        {
            Object o = RC.get(rc, nr, gcol);
            if (o == null)
                o = "";
            String gub = o.toString();
            gub = TerritoryNames.canonic(gub);
            if (gub.length() != 0)
            {
                o = RC.get(rc, nr, wcol);
                setValue(gub, year, what, o);
            }
        }
    }

    private Class typeof(String what) throws Exception
    {
        switch (what)
        {
        case "чж":
        case "чр":
        case "чу":
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
            Double v = null;
            
            if (o instanceof Double)
            {
                v = (Double) o;
            }
            else if (o instanceof Long)
            {
                v = ((Long) o).doubleValue();
            }
            else if (o instanceof Integer)
            {
                v = ((Integer) o).doubleValue();
            }
            else if (o instanceof String)
            {
                v = Double.parseDouble(so);
            }
            else
            {
                throw new Exception("Invalid cell data type (for expected Double)");
            }
            
            territoryYear(gub, year).setValue(what, v);
        }
        else if (typeof(what) == Long.class)
        {
            Long v = null;
            
            if (o instanceof Long)
            {
                v = ((Long) o).longValue();
            }
            else if (o instanceof Integer)
            {
                v = ((Integer) o).longValue();
            }
            else if (o instanceof String)
            {
                v = Long.parseLong(so);
            }
            else if (o instanceof Double)
            {
                double dv = (Double) o;
                v = Math.round(dv);
                if (dv - v != 0)
                    throw new Exception("Expected cell value: long/integer, actual: double");
            }
            else
            {
                throw new Exception("Invalid cell data type (for expected Long)");
            }

            territoryYear(gub, year).setValue(what, v);
        }
    }

    private TerritoryYear territoryYear(String gub, int year)
    {
        Territory t = territories.get(gub);
        
        if (t == null)
        {
            t  = new Territory(gub);
            territories.put(gub, t);
        }

        return t.territoryYear(year);
    }
}
