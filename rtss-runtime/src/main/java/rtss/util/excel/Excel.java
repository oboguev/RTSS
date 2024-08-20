package rtss.util.excel;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import rtss.util.Util;

public class Excel
{
    /*
     * Get workbook sheet as a list of rows.
     * Each row has values for columns (c1, c2, c3 ...)
     */
    public static List<List<Object>> readSheet(String path, boolean cached, String... matchingSheetNames) throws Exception
    {
        XSSFWorkbook wb = null;

        try
        {
            wb = loadWorkbook(path, cached);
            XSSFSheet sheet = findSheet(wb, matchingSheetNames);
            return readSheet(wb, sheet, path);
        }
        finally
        {
            if (!cached && wb != null)
                wb.close();
        }
    }

    private static <T> Iterable<T> toIterable(Iterator<T> it)
    {
        return () -> it;
    }

    public static ExcelRC readSheet(XSSFWorkbook wb, XSSFSheet sheet, String path) throws Exception
    {
        ExcelRC xrows = new ExcelRC();
        FormulaEvaluator evaluator = new XSSFFormulaEvaluator(wb);
        String sheetName = sheet.getSheetName();
        if (sheetName == null || sheetName.trim().length() == 0)
            sheetName = "<unnamed>";

        int nr1 = sheet.getFirstRowNum();
        int nr2 = sheet.getLastRowNum();

        for (int nr = 0; nr <= nr2; nr++)
        {
            List<Object> xrow = new ArrayList<>();

            if (nr >= nr1 && sheet.getRow(nr) != null)
            {
                XSSFRow row = sheet.getRow(nr);
                int nc1 = row.getFirstCellNum();
                int nc2 = row.getLastCellNum();
                for (int nc = 0; nc <= nc2; nc++)
                {
                    if (nc < nc1)
                    {
                        xrow.add(null);
                    }
                    else
                    {
                        XSSFCell cell = row.getCell(nc,  Row.MissingCellPolicy.RETURN_NULL_AND_BLANK);
                        
                        if (cell == null)
                        {
                            xrow.add(null);
                            continue;
                        }

                        switch (cell.getCellType())
                        {
                        case BLANK:
                            xrow.add(null);
                            continue;

                        default:
                            break;
                        }

                        CellValue cv = evaluator.evaluate(cell);
                        // Util.out(String.format("%s => %s", cell.toString(), cv.toString()));

                        switch (cv.getCellType())
                        {
                        case BLANK:
                            xrow.add(null);
                            break;

                        case STRING:
                            xrow.add(cv.getStringValue());
                            break;

                        case BOOLEAN:
                            xrow.add(cv.getBooleanValue());
                            break;

                        case NUMERIC:
                            xrow.add(cv.getNumberValue());
                            break;

                        case ERROR:
                            xrow.add("#ERROR");
                            break;

                        default:
                            throw new Exception(String.format("Unsupported cell type %s in resource file %s (sheet %s), row=%d, col=%d",
                                                              cell.getCellType().name(),
                                                              path, sheetName,
                                                              nr + 1, nc + 1));
                        }
                    }
                }
            }
            
            xrows.add(xrow);
        }

        return xrows;
    }

    public static ExcelRC readSheet_old(XSSFWorkbook wb, XSSFSheet sheet, String path) throws Exception
    {
        ExcelRC xrows = new ExcelRC();
        FormulaEvaluator evaluator = new XSSFFormulaEvaluator(wb);
        String sheetName = sheet.getSheetName();
        if (sheetName == null || sheetName.trim().length() == 0)
            sheetName = "<unnamed>";

        int nrow = 0;

        for (Row row : toIterable(sheet.rowIterator()))
        {
            List<Object> xrow = new ArrayList<>();
            int ncol = 0;

            for (Cell cell : toIterable(row.cellIterator()))
            {
                switch (cell.getCellType())
                {
                case BLANK:
                    xrow.add(null);
                    continue;

                default:
                    break;
                }

                CellValue cv = evaluator.evaluate(cell);
                // Util.out(String.format("%s => %s", cell.toString(), cv.toString()));

                switch (cv.getCellType())
                {
                case BLANK:
                    xrow.add(null);
                    break;

                case STRING:
                    xrow.add(cv.getStringValue());
                    break;

                case BOOLEAN:
                    xrow.add(cv.getBooleanValue());
                    break;

                case NUMERIC:
                    xrow.add(cv.getNumberValue());
                    break;

                default:
                    throw new Exception(String.format("Unsupported cell type %s in resource file %s (sheet %s), row=%d, col=%d",
                                                      cell.getCellType().name(),
                                                      path, sheetName,
                                                      nrow + 1, ncol + 1));
                }

                ncol++;
            }

            xrows.add(xrow);
            nrow++;
        }

        return xrows;
    }

    private static Map<String, XSSFWorkbook> path2workbook = new HashMap<>();

    private static XSSFWorkbook loadWorkbook(String path, boolean cached) throws Exception
    {
        if (cached)
        {
            synchronized (path2workbook)
            {
                XSSFWorkbook wb = path2workbook.get(path);
                if (wb == null)
                {
                    wb = loadWorkbook(path);
                    path2workbook.put(path, wb);
                }
                return wb;
            }
        }
        else
        {
            return loadWorkbook(path);
        }
    }

    public static XSSFWorkbook loadWorkbook(String path) throws Exception
    {
        try (InputStream is = new ByteArrayInputStream(Util.loadResourceAsBytes(path)))
        {
            XSSFWorkbook wb = new XSSFWorkbook(is);
            wb.getCreationHelper().createFormulaEvaluator().evaluateAll();
            return wb;
        }
    }

    public static XSSFSheet findSheet(XSSFWorkbook wb, String... names) throws Exception
    {
        XSSFSheet selectedSheet = null;
        for (int k = 0; k < wb.getNumberOfSheets(); k++)
        {
            XSSFSheet sheet = wb.getSheetAt(k);
            String name = sheet.getSheetName();
            if (matches(name, names))
            {
                if (selectedSheet != null)
                    throw new Exception("Workbook contains mutiple sheets with matching names");
                selectedSheet = sheet;
            }
        }

        if (selectedSheet == null)
            throw new NoSuchElementException("Workbook does not contain the requested sheet");

        return selectedSheet;
    }

    private static boolean matches(String name, String... names)
    {
        for (String s : names)
        {
            if (s.trim().equalsIgnoreCase(name))
                return true;
        }

        return false;
    }

    /*
     * Get values for the indicated column in the worksheet
     */
    public static List<Object> getColumn(List<List<Object>> sheet, String... matchingColumnNames) throws Exception
    {
        return getColumn(sheet, 1, matchingColumnNames);
    }

    public static List<Object> getColumn(List<List<Object>> sheet, int headerRow, String... matchingColumnNames) throws Exception
    {
        int selectedColumn = -1;
        List<Object> list = new ArrayList<>();

        for (int nrow = 1; nrow <= sheet.size(); nrow++)
        {
            List<Object> row = sheet.get(nrow - 1);

            if (nrow < headerRow)
            {
                // ignore the row
            }
            else if (nrow == headerRow)
            {
                selectedColumn = findColumn(row, matchingColumnNames);
                if (selectedColumn == -1)
                    throw new NoSuchElementException("No such column");
            }
            else
            {
                int ix = selectedColumn - 1;
                if (ix < row.size())
                {
                    list.add(row.get(ix));
                }
                else
                {
                    list.add(null);
                }
            }
        }

        return list;
    }

    private static int findColumn(List<Object> row, String... matchingColumnNames) throws Exception
    {
        int selected = -1;

        for (int ncol = 1; ncol <= row.size(); ncol++)
        {
            if (headerMatches(row.get(ncol - 1), matchingColumnNames))
            {
                if (selected != -1)
                    throw new Exception("Multiple matches for column");
                selected = ncol;
            }
        }

        return selected;
    }

    private static boolean headerMatches(Object o, String... matchingColumnNames)
    {
        if (o == null)
        {
            for (String text : matchingColumnNames)
            {
                if (text == null || text.length() == 0)
                    return true;
            }
            return false;
        }
        else if (o instanceof String)
        {
            String s = (String) o;
            return matches(s, matchingColumnNames);
        }
        else if (o instanceof Double)
        {
            for (String text : matchingColumnNames)
            {
                try
                {
                    Double d = Double.parseDouble(text.replace(",", ""));
                    if (d.equals(o))
                        return true;
                }
                catch (Exception ex)
                {
                    // ignore
                }
            }

            return false;
        }
        else
        {
            Util.err("Unsupported column type in the worksheet");
            return false;
        }
    }

    public static List<Object> loadColumn(String path, String[] matchingSheetNames, String[] matchingColumnNames) throws Exception
    {
        return loadColumn(path, matchingSheetNames, 1, matchingColumnNames);
    }

    public static List<Object> loadColumn(String path, String[] matchingSheetNames, int headerRow, String[] matchingColumnNames) throws Exception
    {
        List<List<Object>> sheet = readSheet(path, true, matchingSheetNames);
        return Excel.getColumn(sheet, headerRow, matchingColumnNames);
    }
}
