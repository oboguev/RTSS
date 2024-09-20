package rtss.pre1917;

import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import rtss.pre1917.data.ColumnHeader;
import rtss.pre1917.data.DataSetType;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryNames;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.eval.EvalEvroChastPopulation;
import rtss.pre1917.validate.CrossVerify;
import rtss.util.Util;
import rtss.util.excel.Excel;
import rtss.util.excel.ExcelRC;

public class LoadData
{
    public static enum LoadOptions
    {
        NONE, VERIFY, DONT_VERIFY, MERGE_CITIES, DONT_MERGE_CITIES
    }

    public static void main(String[] args)
    {
        LoadData self = new LoadData();
        try
        {
            // self.loadCensus1897(LoadOptions.VERIFY, LoadOptions.MERGE_CITIES);
            // self.loadEvroChast(LoadOptions.VERIFY, LoadOptions.MERGE_CITIES);
            // self.loadEzhegodnikRossii(LoadOptions.VERIFY, LoadOptions.MERGE_CITIES);
            self.loadUGVI(LoadOptions.VERIFY, LoadOptions.DONT_MERGE_CITIES);
            // TerritoryNames.printSeen();
            Util.out("** Done");
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();

            if (self.currentNR != null)
            {
                Util.out(String.format("File %s, col %c (%d), row %s", self.currentFile, 'A' + self.currentWCOL, self.currentWCOL + 1,
                                       self.currentNR + 1));
            }
        }
    }

    private TerritoryDataSet territories;
    private Integer currentFileYear = null;
    private String currentFile = null;
    private Integer currentWCOL = null;
    private Integer currentNR = null;

    /* ================================================================================================= */

    public TerritoryDataSet loadEzhegodnikRossii(LoadOptions... options) throws Exception
    {
        territories = new TerritoryDataSet(DataSetType.CSK_EZHEGODNIK_ROSSII);

        for (int year = 1904; year <= 1917; year++)
            loadEzhegodnikRossii(year);

        if (hasOption(LoadOptions.MERGE_CITIES, options))
            territories.mergeCities();

        if (hasOption(LoadOptions.VERIFY, options))
            new CrossVerify().verify(territories);

        return territories;
    }

    private void loadEzhegodnikRossii(int year) throws Exception
    {
        currentFileYear = year;
        currentFile = String.format("csk-ezhegodnik-rossii/year-volumes/%d.xlsx", year);

        try (XSSFWorkbook wb = Excel.loadWorkbook(currentFile))
        {
            for (int k = 0; k < wb.getNumberOfSheets(); k++)
            {
                XSSFSheet sheet = wb.getSheetAt(k);
                String sname = sheet.getSheetName();
                if (sname != null && sname.trim().toLowerCase().contains("note"))
                    continue;

                ExcelRC rc = Excel.readSheet(wb, sheet, currentFile);
                Map<String, Integer> headers = ColumnHeader.getTopHeaders(sheet, rc);
                validateHeaders(headers);

                if (!headers.containsKey("губ"))
                    throw new Exception("Нет колонки для губернии");
                int gcol = headers.get("губ");
                scanGubColumn(rc, gcol);

                scanThisYearColumn(rc, gcol, headers, "чж-уез-м");
                scanThisYearColumn(rc, gcol, headers, "чж-уез-ж");
                scanThisYearColumn(rc, gcol, headers, "чж-уез-о");

                scanThisYearColumn(rc, gcol, headers, "чж-гор-м");
                scanThisYearColumn(rc, gcol, headers, "чж-гор-ж");
                scanThisYearColumn(rc, gcol, headers, "чж-гор-о");

                scanThisYearColumn(rc, gcol, headers, "чж-всего-м");
                scanThisYearColumn(rc, gcol, headers, "чж-всего-ж");
                scanThisYearColumn(rc, gcol, headers, "чж-всего-о");
            }
        }
        finally
        {
            currentFileYear = null;
            currentFile = null;
        }

        currentFileYear = null;
        currentFile = null;
    }

    /* ================================================================================================= */

    public TerritoryDataSet loadEvroChast(LoadOptions... options) throws Exception
    {
        territories = new TerritoryDataSet(DataSetType.CSK_DVIZHENIE_EVROPEISKOI_CHASTI_ROSSII);

        for (int year = 1897; year <= 1914; year++)
            loadEvroChast(year);

        new EvalEvroChastPopulation().eval(territories);

        if (hasOption(LoadOptions.MERGE_CITIES, options))
            territories.mergeCities();

        if (hasOption(LoadOptions.VERIFY, options))
            new CrossVerify().verify(territories);

        return territories;
    }

    private void loadEvroChast(int year) throws Exception
    {
        currentFileYear = year;
        currentFile = String.format("csk-dvizhenie-evropriskoi-chasti-rossii/year-volumes/%d.xlsx", year);

        try (XSSFWorkbook wb = Excel.loadWorkbook(currentFile))
        {
            for (int k = 0; k < wb.getNumberOfSheets(); k++)
            {
                XSSFSheet sheet = wb.getSheetAt(k);
                String sname = sheet.getSheetName();
                if (sname != null && sname.trim().toLowerCase().contains("note"))
                    continue;

                ExcelRC rc = Excel.readSheet(wb, sheet, currentFile);
                Map<String, Integer> headers = ColumnHeader.getTopHeaders(sheet, rc);
                validateHeaders(headers);

                if (!headers.containsKey("губ"))
                    throw new Exception("Нет колонки для губернии");
                int gcol = headers.get("губ");
                scanGubColumn(rc, gcol);

                if (year != 1903)
                    scanYearColumn(rc, gcol, headers, "чж");
                scanThisYearColumn(rc, gcol, headers, "р");
                scanThisYearColumn(rc, gcol, headers, "с");
                scanThisYearColumn(rc, gcol, headers, "еп");
                scanThisYearColumn(rc, gcol, headers, "чр-м");
                scanThisYearColumn(rc, gcol, headers, "чр-ж");
                scanThisYearColumn(rc, gcol, headers, "чр-о");
                scanThisYearColumn(rc, gcol, headers, "чс-м");
                scanThisYearColumn(rc, gcol, headers, "чс-ж");
                scanThisYearColumn(rc, gcol, headers, "чс-о");
            }
        }
        finally
        {
            currentFileYear = null;
            currentFile = null;
        }

        currentFileYear = null;
        currentFile = null;
    }

    /* ================================================================================================= */

    public TerritoryDataSet loadCensus1897(LoadOptions... options) throws Exception
    {
        territories = new TerritoryDataSet(DataSetType.CENSUS_1897);

        currentFileYear = 1897;
        currentFile = "census-1897/census-1897.xlsx";

        try (XSSFWorkbook wb = Excel.loadWorkbook(currentFile))
        {
            for (int k = 0; k < wb.getNumberOfSheets(); k++)
            {
                XSSFSheet sheet = wb.getSheetAt(k);
                String sname = sheet.getSheetName();
                if (sname != null && sname.trim().toLowerCase().contains("note"))
                    continue;

                ExcelRC rc = Excel.readSheet(wb, sheet, currentFile);
                Map<String, Integer> headers = ColumnHeader.getTopHeaders(sheet, rc);
                validateHeaders(headers);

                if (!headers.containsKey("губ"))
                    throw new Exception("Нет колонки для губернии");
                int gcol = headers.get("губ");
                scanGubColumn(rc, gcol);

                // scan column "key yyyy" 
                scanThisYearColumn(rc, gcol, headers, "чж-м");
                scanThisYearColumn(rc, gcol, headers, "чж-ж");
                scanThisYearColumn(rc, gcol, headers, "чж-о");
            }
        }
        finally
        {
            currentFileYear = null;
            currentFile = null;
        }

        if (hasOption(LoadOptions.MERGE_CITIES, options))
            territories.mergeCities();

        if (hasOption(LoadOptions.VERIFY, options))
            new CrossVerify().verify(territories);

        return territories;
    }

    /* ================================================================================================= */

    public TerritoryDataSet loadUGVI(LoadOptions... options) throws Exception
    {
        territories = new TerritoryDataSet(DataSetType.UGVI);

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

        if (hasOption(LoadOptions.MERGE_CITIES, options))
            territories.mergeCities();

        if (hasOption(LoadOptions.VERIFY, options))
            new CrossVerify().verify(territories);

        return territories;
    }

    private void loadUGVI(String fn) throws Exception
    {
        currentFileYear = Integer.parseInt(fn);

        String fpath = String.format("ugvi/year-volumes/%s.xlsx", fn);
        currentFile = fpath;

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

                scanYearColumn(rc, gcol, headers, "чж-гор-м");
                scanYearColumn(rc, gcol, headers, "чж-гор-ж");
                scanYearColumn(rc, gcol, headers, "чж-гор-о");

                scanYearColumn(rc, gcol, headers, "чр-гор-м");
                scanYearColumn(rc, gcol, headers, "чр-гор-ж");
                scanYearColumn(rc, gcol, headers, "чр-гор-о");

                scanYearColumn(rc, gcol, headers, "чс-гор-м");
                scanYearColumn(rc, gcol, headers, "чс-гор-ж");
                scanYearColumn(rc, gcol, headers, "чс-гор-о");

                scanYearColumn(rc, gcol, headers, "чж-уез-м");
                scanYearColumn(rc, gcol, headers, "чж-уез-ж");
                scanYearColumn(rc, gcol, headers, "чж-уез-о");

                scanYearColumn(rc, gcol, headers, "чр-уез-м");
                scanYearColumn(rc, gcol, headers, "чр-уез-ж");
                scanYearColumn(rc, gcol, headers, "чр-уез-о");

                scanYearColumn(rc, gcol, headers, "чс-уез-м");
                scanYearColumn(rc, gcol, headers, "чс-уез-ж");
                scanYearColumn(rc, gcol, headers, "чс-уез-о");
            }
        }

        currentFileYear = null;
        currentFile = null;
    }

    private void loadUGVI(String fn, int y1, int y2) throws Exception
    {
        String fpath = String.format("ugvi/year-volumes/%s.xlsx", fn);
        currentFile = fpath;

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

        currentFile = null;
    }

    /* ================================================================================================= */

    private void validateHeaders(Map<String, Integer> headers) throws Exception
    {
        for (String h : headers.keySet())
        {
            switch (h)
            {
            case "губ":
            case "чж-о":
            case "чж-м":
            case "чж-ж":
            case "чж-уез-м":
            case "чж-уез-ж":
            case "чж-уез-о":
            case "чж-гор-м":
            case "чж-гор-ж":
            case "чж-гор-о":
            case "чж-всего-м":
            case "чж-всего-ж":
            case "чж-всего-о":
            case "чр":
            case "чр-м":
            case "чр-ж":
            case "чр-о":
            case "чс-м":
            case "чс-ж":
            case "чс-о":
            case "чу":
            case "р":
            case "с":
            case "еп":
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
                // --------------------------
                h.startsWith("чж-гор-м ") ||
                h.startsWith("чж-гор-ж ") ||
                h.startsWith("чж-гор-о ") ||
                // --------------------------
                h.startsWith("чр-гор-м ") ||
                h.startsWith("чр-гор-ж ") ||
                h.startsWith("чр-гор-о ") ||
                // --------------------------
                h.startsWith("чс-гор-м ") ||
                h.startsWith("чс-гор-ж ") ||
                h.startsWith("чс-гор-о ") ||
                // --------------------------
                h.startsWith("чж-уез-м ") ||
                h.startsWith("чж-уез-ж ") ||
                h.startsWith("чж-уез-о ") ||
                // --------------------------
                h.startsWith("чр-уез-м ") ||
                h.startsWith("чр-уез-ж ") ||
                h.startsWith("чр-уез-о ") ||
                // --------------------------
                h.startsWith("чс-уез-м ") ||
                h.startsWith("чс-уез-ж ") ||
                h.startsWith("чс-уез-о "))
            {
                continue;
            }

            throw new Exception("Incorrect header in Excel file");
        }
    }

    private void scanGubColumn(ExcelRC rc, int gcol) throws Exception
    {
        for (int nr = 1; nr < rc.size() && !rc.isEndRow(nr); nr++)
        {
            Object o = rc.get(nr, gcol);
            if (o == null)
                o = "";
            String gub = o.toString();

            if (territories.dataSetType == DataSetType.CSK_DVIZHENIE_EVROPEISKOI_CHASTI_ROSSII && gub.equals("всего"))
                gub = "50 губерний Европейской России";

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
                else if (ys.equals("MYY") && currentFileYear != null)
                {
                    year = currentFileYear;
                    what = "MYY-" + what;
                }
                else if (ys.equals("NYY") && currentFileYear != null)
                {
                    year = currentFileYear + 1;
                }
                else if (ys.equals("PYY") && currentFileYear != null)
                {
                    year = currentFileYear - 1;
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

    private void scanThisYearColumn(ExcelRC rc, int gcol, Map<String, Integer> headers, String what) throws Exception
    {
        for (String h : headers.keySet())
        {
            if (h.equals(what))
            {
                scanYearColumn(rc, gcol, what, currentFileYear.intValue(), headers.get(h));
            }
        }
    }

    private void scanYearColumn(ExcelRC rc, int gcol, String what, int year, int wcol) throws Exception
    {
        currentWCOL = wcol;

        for (int nr = 1; nr < rc.size() && !rc.isEndRow(nr); nr++)
        {
            currentNR = nr;

            Object o = rc.get(nr, gcol);
            if (o == null)
                o = "";
            String gub = o.toString();

            if (territories.dataSetType == DataSetType.CSK_DVIZHENIE_EVROPEISKOI_CHASTI_ROSSII && gub.equals("всего"))
                gub = "50 губерний Европейской России";

            if (territories.dataSetType == DataSetType.CSK_EZHEGODNIK_ROSSII)
            {
                switch (gub)
                {
                case "Бакинская":
                    gub = "Бакинская с Баку";
                    break;

                case "Таврическая":
                    gub = "Таврическая с Севастополем";
                    break;
                }
            }

            gub = TerritoryNames.canonic(gub);
            if (gub.length() != 0)
            {
                o = rc.get(nr, wcol);
                setValue(gub, year, what, o);
            }
        }

        currentWCOL = null;
        currentNR = null;
    }

    private void scanMultiYearColumn(ExcelRC rc, int gcol, int ycol, Map<String, Integer> headers, String what) throws Exception
    {
        if (!headers.containsKey(what))
            return;
        int wcol = headers.get(what);

        String gub = null;

        double avg_sum = 0;
        double avg_count = 0;

        for (int nr = 1; nr < rc.size() && !rc.isEndRow(nr); nr++)
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
        case "MYY-чж":
        case "чж":
        case "чж-м":
        case "чж-ж":
        case "чж-о":
        case "чр":
        case "чр-м":
        case "чр-ж":
        case "чр-о":
        case "чс-м":
        case "чс-ж":
        case "чс-о":
        case "чу":
            // ---------------
        case "чж-гор-м":
        case "чж-гор-ж":
        case "чж-гор-о":
            // ---------------
        case "чр-гор-м":
        case "чр-гор-ж":
        case "чр-гор-о":
            // ---------------
        case "чс-гор-м":
        case "чс-гор-ж":
        case "чс-гор-о":
            // ---------------
        case "чж-уез-м":
        case "чж-уез-ж":
        case "чж-уез-о":
            // ---------------
        case "чр-уез-м":
        case "чр-уез-ж":
        case "чр-уез-о":
            // ---------------
        case "чс-уез-м":
        case "чс-уез-ж":
        case "чс-уез-о":
            return Long.class;

        case "р":
        case "с":
        case "п":
        case "еп":
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
        if (so.length() == 0 || so.equals("-") || so.equals("—"))
            return;

        if (territories.dataSetType == DataSetType.CSK_EZHEGODNIK_ROSSII && what.startsWith("чж") && year != 1917)
        {
            Long v = asLongThousands(o);
            if (v != null && (v % 100) != 0)
            {
                String msg = String.format("Значение не округлено до 0.1 тысячи: %d %s %s", year, gub, what);
                Util.err(msg);
            }
            territoryYear(gub, year).setValue(what, v);
        }
        else if (territories.dataSetType == DataSetType.CSK_EZHEGODNIK_ROSSII && what.startsWith("чж") && year == 1917)
        {
            territoryYear(gub, year).setValue(what, asLong(o));
        }
        else if (typeof(what) == Double.class)
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

    private Long asLongThousands(Object o) throws Exception
    {
        double v = asDouble(o);
        return Math.round(1000 * v);
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

    private boolean hasOption(LoadOptions option, LoadOptions[] options)
    {
        for (int k = 0; k < options.length; k++)
        {
            if (options[k] == option)
                return true;
        }

        return false;
    }
}
