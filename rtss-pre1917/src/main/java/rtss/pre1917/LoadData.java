package rtss.pre1917;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import rtss.pre1917.data.ColumnHeader;
import rtss.pre1917.data.DataSetType;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryNames;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.data.migration.Emigration;
import rtss.pre1917.data.migration.EmigrationYear;
import rtss.pre1917.data.migration.InnerMigration;
import rtss.pre1917.eval.EvalEvroChastPopulation;
import rtss.pre1917.eval.EvalProgressive;
import rtss.pre1917.eval.FillMissingBD;
import rtss.pre1917.merge.MergeCities;
import rtss.pre1917.merge.MergeDescriptor;
import rtss.pre1917.validate.CrossVerify;
import rtss.util.Util;
import rtss.util.excel.Excel;
import rtss.util.excel.ExcelRC;

public class LoadData
{
    public static enum LoadOptions
    {
        NONE,

        // проверка внутренней согласованности загруженных данных
        VERIFY, DONT_VERIFY,

        // удалить записи для городов, включив их население в состав соотв. губерний
        MERGE_CITIES, DONT_MERGE_CITIES,

        // слить губернии и области образованные после переписи 1897 года с губерниями, из состава
        // которых они были выделены, образовав новые составные записи
        MERGE_POST1897_REGIONS, DONT_MERGE_POST1897_REGIONS,

        // произвести поправку на недорегистрацию рождений девочкек:
        // исправить число рождений для женщин сделав его >= числу мужских рождений / 1.06
        ADJUST_FEMALE_BIRTHS, DONT_ADJUST_FEMALE_BIRTHS,

        // заполнить пробелы в сведениях о числе рождений и смертей (только для УГВИ)
        FILL_MISSING_BD, DONT_FILL_MISSING_BD,

        // вычислить прогрессивную оценку населения отсчётом от переписи 1897 года (только для УГВИ)
        // и сохранить её в поле progressive_population, параллельно собственным данным УГВИ
        EVAL_PROGRESSIVE, DONT_EVAL_PROGRESSIVE
    }

    public static void main(String[] args)
    {
        LoadData self = new LoadData();

        try
        {
            // self.loadCensus1897(LoadOptions.VERIFY, LoadOptions.MERGE_CITIES);
            // self.loadEvroChast(LoadOptions.VERIFY, LoadOptions.MERGE_CITIES);
            // self.loadEzhegodnikRossii(LoadOptions.VERIFY, LoadOptions.MERGE_CITIES);
            // self.loadUGVI(LoadOptions.VERIFY, LoadOptions.DONT_MERGE_CITIES);
            // TerritoryNames.printSeen();
            self.loadEmigration();
            // self.loadJews();
            // self.loadInnerMigration();
            // self.loadFinland();
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

    public TerritoryDataSet loadEzhegodnikRossii(Set<LoadOptions> options) throws Exception
    {
        return loadEzhegodnikRossii(options.toArray(new LoadOptions[0]));
    }

    public TerritoryDataSet loadEzhegodnikRossii(LoadOptions... options) throws Exception
    {
        territories = new TerritoryDataSet(DataSetType.CSK_EZHEGODNIK_ROSSII, Set.of(options));

        for (int year = 1904; year <= 1917; year++)
            loadEzhegodnikRossii(year);

        if (hasOption(LoadOptions.ADJUST_FEMALE_BIRTHS, options))
            territories.adjustFemaleBirths();

        if (hasOption(LoadOptions.MERGE_CITIES, options))
            territories.mergeCities();

        if (hasOption(LoadOptions.MERGE_POST1897_REGIONS, options))
            territories.mergePost1897Regions();

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

    public TerritoryDataSet loadEvroChast(Set<LoadOptions> options) throws Exception
    {
        return loadEvroChast(options.toArray(new LoadOptions[0]));
    }

    public TerritoryDataSet loadEvroChast(LoadOptions... options) throws Exception
    {
        territories = new TerritoryDataSet(DataSetType.CSK_DVIZHENIE_EVROPEISKOI_CHASTI_ROSSII, Set.of(options));

        for (int year = 1897; year <= 1914; year++)
            loadEvroChast(year);

        if (hasOption(LoadOptions.ADJUST_FEMALE_BIRTHS, options))
            territories.adjustFemaleBirths();

        new EvalEvroChastPopulation().eval(territories);

        if (hasOption(LoadOptions.MERGE_CITIES, options))
            territories.mergeCities();

        if (hasOption(LoadOptions.MERGE_POST1897_REGIONS, options))
            territories.mergePost1897Regions();

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

    public TerritoryDataSet loadCensus1897(Set<LoadOptions> options) throws Exception
    {
        return loadCensus1897(options.toArray(new LoadOptions[0]));
    }

    public TerritoryDataSet loadCensus1897(LoadOptions... options) throws Exception
    {
        territories = new TerritoryDataSet(DataSetType.CENSUS_1897, Set.of(options));

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

        if (hasOption(LoadOptions.ADJUST_FEMALE_BIRTHS, options))
            territories.adjustFemaleBirths();

        if (hasOption(LoadOptions.MERGE_CITIES, options))
            territories.mergeCities();

        if (hasOption(LoadOptions.MERGE_POST1897_REGIONS, options))
            territories.mergePost1897Regions();

        if (hasOption(LoadOptions.VERIFY, options))
            new CrossVerify().verify(territories);

        return territories;
    }

    /* ================================================================================================= */

    public TerritoryDataSet loadUGVI(Set<LoadOptions> options) throws Exception
    {
        return loadUGVI(options.toArray(new LoadOptions[0]));
    }

    public TerritoryDataSet loadUGVI(LoadOptions... options) throws Exception
    {
        territories = new TerritoryDataSet(DataSetType.UGVI, Set.of(options));

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

        addFinland(territories);

        if (hasOption(LoadOptions.ADJUST_FEMALE_BIRTHS, options))
            territories.adjustFemaleBirths();

        if (hasOption(LoadOptions.FILL_MISSING_BD, options))
            new FillMissingBD(territories).fillMissingBD();

        if (hasOption(LoadOptions.MERGE_CITIES, options))
            territories.mergeCities();

        if (hasOption(LoadOptions.MERGE_POST1897_REGIONS, options))
            territories.mergePost1897Regions();

        if (hasOption(LoadOptions.EVAL_PROGRESSIVE, options))
            new EvalProgressive(territories).evalProgressive();

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
            throw new Exception("Invalid selector: " + what);
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

    /* ================================================================================================= */

    public Emigration loadEmigration() throws Exception
    {
        Emigration em = new Emigration();

        currentFile = "emigration.xlsx";

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
                loadEmigration(em, rc, headers.get("год"), headers);
            }
        }
        finally
        {
            currentFile = null;
        }
        
        em.build();

        return em;
    }

    private void loadEmigration(Emigration em, ExcelRC rc, int colYear, Map<String, Integer> headers) throws Exception
    {
        for (int nr = 1; nr < rc.size() && !rc.isEndRow(nr); nr++)
        {
            currentNR = nr;

            Object o = rc.get(nr, colYear);
            if (o == null || o.toString().trim().length() == 0 || o.toString().contains("-"))
                continue;

            EmigrationYear yd = new EmigrationYear();
            yd.year = (int) (long) asLong(o);
            
            yd.total = getEmigration(rc, nr, headers, "всего");
            yd.armenians = getEmigration(rc, nr, headers, "армяне");
            yd.finns = getEmigration(rc, nr, headers, "финны");
            yd.germans = getEmigration(rc, nr, headers, "немцы");
            yd.greeks = getEmigration(rc, nr, headers, "греки");
            yd.hebrews = getEmigration(rc, nr, headers, "евреи");
            yd.lithuanians = getEmigration(rc, nr, headers, "литовцы");
            yd.poles = getEmigration(rc, nr, headers, "поляки");
            yd.russians = getEmigration(rc, nr, headers, "русские");
            yd.ruthenians = getEmigration(rc, nr, headers, "русины");
            yd.scandinavians = getEmigration(rc, nr, headers, "скандинавы");
            yd.others = getEmigration(rc, nr, headers, "другие");
            yd.vyborg = getEmigration(rc, nr, headers, "% для Выборгской губернии");

            em.setYearData(yd);
        }

        currentNR = null;
    }

    private long getEmigration(ExcelRC rc, int nr, Map<String, Integer> headers, String what) throws Exception
    {
        int col = headers.get(what);
        Object o = rc.get(nr, col);
        return Math.round(asDouble(o));
    }

    private double getEmigrationDouble(ExcelRC rc, int nr, Map<String, Integer> headers, String what) throws Exception
    {
        int col = headers.get(what);
        Object o = rc.get(nr, col);
        return asDouble(o);
    }

    /* ================================================================================================= */

    public Map<String, Double> loadJews() throws Exception
    {
        Map<String, Double> m = new HashMap<>();

        currentFile = "census-1897/juifs.xlsx";

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
                loadJews(m, rc, headers.get("губ"), headers.get("% иудеев"));

                for (MergeDescriptor md : MergeCities.MergeCitiesDescriptors)
                    replicateJews(m, md);
            }
        }
        finally
        {
            currentFile = null;
        }

        return m;
    }

    private void loadJews(Map<String, Double> m, ExcelRC rc, int colGub, int colAmount) throws Exception
    {
        for (int nr = 1; nr < rc.size() && !rc.isEndRow(nr); nr++)
        {
            currentNR = nr;

            Object o = rc.get(nr, colGub);
            if (o == null || o.toString().trim().length() == 0)
                continue;
            String gub = o.toString();
            gub = TerritoryNames.canonic(gub);
            TerritoryNames.checkValidTerritoryName(gub);

            double amount = asDouble(rc.get(nr, colAmount));

            if (m.containsKey(gub))
                throw new Exception("Duplicate territory");

            m.put(gub, amount);
        }

        currentNR = null;
    }

    private void replicateJews(Map<String, Double> m, MergeDescriptor md)
    {
        if (md.parent != null)
            replicateJews(m, md.combined, md.parent);

        for (String child : md.children)
            replicateJews(m, md.combined, child);
    }

    private void replicateJews(Map<String, Double> m, String g1, String g2)
    {
        if (m.containsKey(g1))
            m.put(g2, m.get(g1));
        else if (m.containsKey(g2))
            m.put(g1, m.get(g2));
    }

    /* ================================================================================================= */

    private static InnerMigration cachedInnerMigration;

    public InnerMigration loadInnerMigration() throws Exception
    {
        if (cachedInnerMigration == null)
        {
            InnerMigration im = new InnerMigration();
            loadInnerMigrationYearly(im);
            loadInnerMigrationCorarse(im);
            im.build();
            cachedInnerMigration = im;
        }
        
        return cachedInnerMigration;
    }

    private void loadInnerMigrationYearly(InnerMigration im) throws Exception
    {
        currentFile = "inner-migration/inner-migration-yearly.xlsx";

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
                loadInnerMigrationYearly(im, sname, rc, headers);
            }
        }
        finally
        {
            currentFile = null;
        }
    }

    private void loadInnerMigrationYearly(InnerMigration im, String sheetName, ExcelRC rc, Map<String, Integer> headers) throws Exception
    {
        int colGub = headers.get("губ");
        int colYear = headers.get("год");
        loadInnerMigrationYearly(im, sheetName, rc, colGub, colYear, headers.get("прибыло"), "прибыло");
        loadInnerMigrationYearly(im, sheetName, rc, colGub, colYear, headers.get("убыло"), "убыло");
    }

    private static class AreaData extends HashMap<Integer, Long>
    {
        private static final long serialVersionUID = 1L;
        private final String gub;

        public AreaData(String gub)
        {
            this.gub = gub;
        }
    }

    private static class AreaToData extends HashMap<String, AreaData>
    {
        private static final long serialVersionUID = 1L;
    }

    private void loadInnerMigrationYearly(InnerMigration im, String sheetName, ExcelRC rc, int colGub, int colYear, int colWhat, String what)
            throws Exception
    {
        AreaToData a2d = new AreaToData();
        String gub = null;

        for (int nr = 1; nr < rc.size() && !rc.isEndRow(nr); nr++)
        {
            currentNR = nr;

            // губерния, область или группировка
            Object o = rc.get(nr, colGub);
            if (o != null && o.toString().trim().length() != 0)
                gub = o.toString();
            gub = canonicAreaName(gub);

            // год
            o = rc.get(nr, colYear);
            if (o == null || o.toString().trim().length() == 0)
                continue;
            String syear = o.toString();
            if (sheetName.startsWith("1910-1914 "))
            {
                switch (syear)
                {
                case "1896-1909":
                case "1896-1914":
                    continue;

                case "1910-1914":
                    syear = "всего";
                    break;
                }
            }

            int year = -1;
            if (!syear.equals("всего"))
            {
                year = (int) (long) asLong(o);
                if (sheetName.startsWith("1896-1909 "))
                {
                    if (!(year >= 1896 && year <= 1909))
                        throw new Exception("Invalid year");
                }
                else if (sheetName.startsWith("1910-1914 "))
                {
                    if (!(year >= 1910 && year <= 1914))
                        throw new Exception("Invalid year");
                }
                else
                {
                    throw new Exception("Unexpected sheet name: " + sheetName);
                }
            }

            // величина
            Long v = null;
            o = rc.get(nr, colWhat);
            if (o != null && o.toString().trim().length() != 0)
                v = asLong(o);

            AreaData ad = a2d.get(gub);
            if (ad == null)
            {
                ad = new AreaData(gub);
                a2d.put(gub, ad);
            }

            if (ad.containsKey(year))
                throw new Exception("Duplicate year data");
            ad.put(year, v);
        }

        currentNR = null;

        validate_vsego(a2d);

        String extra1 = null;
        String extra2 = null;

        if (sheetName.startsWith("1910-1914 "))
        {
            extra1 = "Батумская обл.";
            extra2 = "Холмская";
        }

        if (sheetName.endsWith(" выход"))
        {
            validate(a2d,
                     "Итого по Черноземной и Степной полосам",
                     "Курская",
                     "Тамбовская",
                     "Пензенская",
                     "Орловская",
                     "Черниговская",
                     "Тульская",
                     "Рязанская",
                     "Полтавская",
                     "Харьковская",
                     "Воронежская",
                     "Киевская",
                     "Подольская",
                     "Волынская",
                     "Бессарабская",
                     "Екатеринославская",
                     "Херсонская",
                     "Таврическая",
                     "Область Войска Донского",
                     "Астраханская",
                     "Казанская",
                     "Нижегородская",
                     "Симбирская",
                     "Саратовская",
                     "Самарская",
                     "Уфимская",
                     "Оренбургская",
                     "Уральская обл.",
                     "Ставропольская",
                     "Кубанская обл.",
                     "Терская обл.",
                     "Кутаисская",
                     "Черноморская",
                     "Тифлисская",
                     "Дагестанская обл.",
                     "Бакинская",
                     "Эриванская",
                     extra1,
                     "Елизаветпольская",
                     "Карсская");

            validate(a2d,
                     "Итого по Нечерноземной полосе",
                     "Московская",
                     "Калужская",
                     "Владимирская",
                     "Тверская",
                     "Смоленская",
                     "Гродненская",
                     "Виленская",
                     "Ковенская",
                     "Могилевская",
                     "Витебская",
                     "Минская",
                     "Костромская",
                     "Вятская",
                     "Пермская",
                     "Псковская",
                     "Новгородская",
                     "Санкт-Петербургская",
                     "Лифляндская",
                     "Курляндская",
                     "Эстляндская",
                     "Вологодская",
                     extra2,
                     "Люблинская",
                     "Прочие губернии Нечерноземной полосы");

            validate(a2d,
                     "Общий итог",
                     "Итого по Черноземной и Степной полосам",
                     "Итого по Нечерноземной полосе",
                     "Прочие губернии и области",
                     "Иностранные подданные");
        }

        if (sheetName.endsWith(" назначение"))
        {
            validate(a2d,
                     "Итого по Степному краю и Туркестану",
                     "Тургайская",
                     "Уральская",
                     "Акмолинская",
                     "Семипалатинская",
                     "Семиреченская",
                     "Сыр-Дарьинская",
                     "Ферганская",
                     "Прочие области Туркестана");

            validate(a2d,
                     "Томская (всего)",
                     "Томская (на казённые земли)",
                     "Томская (на кабинетские земли)",
                     "Томская (на невыяснено какие земли)");

            validate(a2d,
                     "Итого по Сибирским губерниям",
                     "Тобольская",
                     "Томская (всего)",
                     "Енисейская",
                     "Иркутская");

            validate(a2d,
                     "Итого по степному краю, Туркестану и Сибирским губерниям",
                     "Итого по Степному краю и Туркестану",
                     "Итого по Сибирским губерниям");

            validate(a2d,
                     "Итого по Дальнему востоку ",
                     "Забайкальская",
                     "Амурская",
                     "Приморская",
                     "Якутская");

            validate(a2d,
                     "Итого по Азиатской России",
                     "Итого по степному краю, Туркестану и Сибирским губерниям",
                     "Итого по Дальнему востоку");

            validate(a2d,
                     "Итого по приуральским губерниям Европейской России",
                     "Оренбургская",
                     "Уфимская",
                     "Самарская",
                     "Пермская");

            validate(a2d,
                     "Общий итог",
                     "Итого по Азиатской России",
                     "Итого по приуральским губерниям Европейской России",
                     "Вернулись с пути",
                     "Невыяснено");
        }

        // load to im
        for (String aname : a2d.keySet())
        {
            String tname = aname;
            if (aname.equals("Томская (всего)"))
                tname = TerritoryNames.canonic("Томская");
            if (isAggregateAreaName(tname))
                continue;

            AreaData ad = a2d.get(aname);

            for (int year : ad.keySet())
            {
                if (year != -1 && ad.get(year) != null)
                {
                    switch (what)
                    {
                    case "прибыло":
                        im.setInFlow(tname, year, ad.get(year));
                        break;

                    case "убыло":
                        im.setOutFlow(tname, year, ad.get(year));
                        break;
                    }
                }
            }
        }
    }

    private void validate_vsego(AreaToData a2d) throws Exception
    {
        for (String gub : a2d.keySet())
            validate_vsego(a2d.get(gub));
    }

    private void validate_vsego(AreaData ad) throws Exception
    {
        long sum = 0;
        for (int year : ad.keySet())
        {
            if (year != -1 && ad.get(year) != null)
                sum += ad.get(year);
        }

        Long xsum = ad.get(-1);
        if (xsum == null)
            xsum = 0L;

        if (sum != xsum)
            throw new Exception("Mismatching всего для " + ad.gub);
    }

    private void validate(AreaToData a2d, String aggregate, String... parts) throws Exception
    {
        aggregate = canonicAreaName(aggregate);
        AreaData adAggregate = a2d.get(aggregate);

        for (int year : orderYears(adAggregate.keySet()))
        {
            long sum = 0;

            for (String part : parts)
            {
                if (part == null)
                    continue;
                part = canonicAreaName(part);
                AreaData ad = a2d.get(part);
                Long v = ad.get(year);
                if (v != null)
                    sum += v;
            }

            Long agg = adAggregate.get(year);
            if (agg == null)
                agg = 0L;

            if (sum != agg)
            {
                String msg = String.format("Migration aggregate value mismatch for [%s, год %d]: %,d vs %,d (listed vs. computed), diff: %,d",
                                           aggregate, year, agg, sum, Math.abs(agg - sum));
                // Util.err(msg);
                if (Math.abs(sum - agg) >= 100)
                    throw new Exception(msg);
            }
        }
    }

    private List<Integer> orderYears(Collection<Integer> years)
    {
        Set<Integer> xs = new HashSet<Integer>(years);
        boolean hasTotal = xs.contains(-1);
        xs.remove(-1);
        List<Integer> list = new ArrayList<Integer>(xs);
        Collections.sort(list);
        if (hasTotal)
            list.add(-1);
        return list;
    }

    private String canonicAreaName(String aname) throws Exception
    {
        aname = Util.despace(aname).trim();

        if (!isAggregateAreaName(aname))
        {
            aname = TerritoryNames.canonic(aname);
            TerritoryNames.checkValidTerritoryName(aname);
        }

        return aname;
    }

    private boolean isAggregateAreaName(String aname) throws Exception
    {
        aname = Util.despace(aname).trim();

        switch (aname)
        {
        case "Общий итог":
        case "Итого по Черноземной и Степной полосам":
        case "Итого по Нечерноземной полосе":
        case "Прочие губернии и области":
        case "Иностранные подданные":
        case "Прочие губернии Нечерноземной полосы":
        case "Прочие области Туркестана":
        case "Итого по Степному краю и Туркестану":
        case "Томская (на казённые земли)":
        case "Томская (на кабинетские земли)":
        case "Томская (на невыяснено какие земли)":
        case "Томская (всего)":
        case "Итого по Сибирским губерниям":
        case "Итого по степному краю, Туркестану и Сибирским губерниям":
        case "Итого по Дальнему востоку":
        case "Итого по Азиатской России":
        case "Итого по приуральским губерниям Европейской России":
        case "Вернулись с пути":
        case "Невыяснено":
            return true;

        default:
            return false;
        }
    }

    /* ------------------------------------------------------------------------------------------- */

    private void loadInnerMigrationCorarse(InnerMigration im) throws Exception
    {
        currentFile = "inner-migration/inner-migration-coarse-loadable.xlsx";

        try (XSSFWorkbook wb = Excel.loadWorkbook(currentFile))
        {
            for (int k = 0; k < wb.getNumberOfSheets(); k++)
            {
                XSSFSheet sheet = wb.getSheetAt(k);
                String sname = sheet.getSheetName();
                if (sname != null && sname.trim().toLowerCase().startsWith("баланс-"))
                {
                    ExcelRC rc = Excel.readSheet(wb, sheet, currentFile);
                    Map<String, Integer> headers = ColumnHeader.getTopHeaders(sheet, rc);
                    loadInnerMigrationCoarse(im, rc, headers);
                }
            }
        }
        finally
        {
            currentFile = null;
        }
    }

    private void loadInnerMigrationCoarse(InnerMigration im, ExcelRC rc, Map<String, Integer> headers) throws Exception
    {
        int colGub = headers.get("губ");

        for (String header : headers.keySet())
        {
            String htext = Util.despace(header);
            if (htext.startsWith("прибытие "))
            {
                loadInnerMigrationCoarse(im, rc, colGub, headers.get(header), "прибытие", htext.substring("прибытие ".length()));
            }
            else if (htext.startsWith("убытие "))
            {
                loadInnerMigrationCoarse(im, rc, colGub, headers.get(header), "убытие", htext.substring("убытие ".length()));
            }
        }
    }

    private void loadInnerMigrationCoarse(InnerMigration im, ExcelRC rc, int colGub, int col, String what, String years) throws Exception
    {
        int y1;
        int y2;

        if (years.contains("-"))
        {
            String[] sa = years.split("-");
            y1 = Integer.parseInt(sa[0]);
            y2 = Integer.parseInt(sa[1]);
        }
        else
        {
            y1 = y2 = Integer.parseInt(years);
        }

        for (int nr = 1; nr < rc.size() && !rc.isEndRow(nr); nr++)
        {
            currentNR = nr;

            Object o = rc.get(nr, colGub);
            if (o == null || o.toString().trim().length() == 0)
                continue;

            String gub = o.toString().trim();
            switch (gub)
            {
            case "прочие губ. Европейской России":
            case "Прочие":
            case "Иностранные подданные":
            case "Всего":
            case "Итого":
            case "Урянхайский край":
            case "Туркестан":
            case "из неуказанных областей":
            case "вернувшихся с пути":
                continue;
            }

            gub = TerritoryNames.canonic(gub);
            TerritoryNames.checkValidTerritoryName(gub);

            o = rc.get(nr, col);
            if (o == null)
                continue;
            String so = o.toString().trim();
            if (so.length() == 0 || so.equals("-"))
                continue;

            long amount = asLong(o);

            if (Util.True)
            {
                switch (what)
                {
                case "прибытие":
                    im.setInFlowCoarse(gub, amount, y1, y2);
                    break;

                case "убытие":
                    im.setOutFlowCoarse(gub, amount, y1, y2);
                    break;

                default:
                    throw new Exception("Invalid selector");
                }
            }
            else
            {
                for (int year = y1; year <= y2; year++)
                {
                    switch (what)
                    {
                    case "прибытие":
                        im.setInFlow(gub, year, year_amount(amount, year, y1, y2));
                        break;

                    case "убытие":
                        im.setOutFlow(gub, year, year_amount(amount, year, y1, y2));
                        break;

                    default:
                        throw new Exception("Invalid selector");
                    }
                }
            }
        }

        currentNR = null;
    }

    @SuppressWarnings("unused")
    private long year_amount(long amount, int year, int y1, int y2)
    {
        if (y1 == 1911 && y2 == 1915)
        {
            final double weights[] = { 1, 1, 1, 0.6, 0 };
            double sum_weights = 0;
            for (double w : weights)
                sum_weights += w;
            return Math.round(amount * weights[year - 1911] / sum_weights);
        }
        else
        {
            int nyears = y2 - y1 + 1;
            return Math.round((1.0 * amount) / nyears);
        }
    }

    /* ================================================================================================= */

    public TerritoryDataSet loadFinland() throws Exception
    {
        territories = new TerritoryDataSet(DataSetType.FINLAND, new HashSet<LoadOptions>());

        String fpath = String.format("Finland.xlsx");
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
                if (!headers.containsKey("чж-о"))
                    throw new Exception("Нет колонки для чж-о");
                if (!headers.containsKey("чр"))
                    throw new Exception("Нет колонки для чр");
                if (!headers.containsKey("чу"))
                    throw new Exception("Нет колонки для чу");

                int gcol = headers.get("губ");
                int ycol = headers.get("год");

                scanGubColumn(rc, gcol);
                scanMultiYearColumn(rc, gcol, ycol, headers, "чж-о");
                scanMultiYearColumn(rc, gcol, ycol, headers, "чр");
                scanMultiYearColumn(rc, gcol, ycol, headers, "чу");
            }
        }
        finally
        {
            currentFile = null;
        }

        return territories;
    }

    public void addFinland(TerritoryDataSet tds) throws Exception
    {
        TerritoryDataSet tdsFinland = new LoadData().loadFinland();
        for (String name : tdsFinland.keySet())
        {
            addFinlandProgressive(tdsFinland.get(name));
            tds.put(name, tdsFinland.get(name));

        }
    }

    private void addFinlandProgressive(Territory t)
    {
        /*
         * Население по финской статистике уже учитывает миграцию
         */
        for (int year : t.years())
        {
            TerritoryYear ty = t.territoryYearOrNull(year);
            ty.progressive_population.total.both = ty.population.total.both;
        }
    }
}
