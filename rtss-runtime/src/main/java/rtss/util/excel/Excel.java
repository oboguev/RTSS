package rtss.util.excel;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.poi.ss.formula.functions.Rows;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jxls.common.CellData.CellType;

import rtss.util.Util;

public class Excel
{
    /*
     * Get workbook sheet as a list of rows.
     * Each row has values for columns (c1, c2, c3 ...)
     */
    public static List<List<Object>> readSheet(String path, boolean cached, String... matchingSheetNames) throws Exception
    {
        List<List<Object>> xrows = new ArrayList<>();
        XSSFWorkbook wb = null;

        try
        {
            wb = loadWorkbook(path, cached);
            FormulaEvaluator evaluator = new XSSFFormulaEvaluator(wb);
            XSSFSheet sheet = findSheet(wb, matchingSheetNames);
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
                        throw new Exception(String.format("Unsupported cell type %s in resource file %s, row=%d, col=%d",
                                                          cell.getCellType().name(),
                                                          path, nrow + 1, ncol + 1));
                    }

                    ncol++;
                }

                xrows.add(xrow);
                nrow++;
            }
        }
        finally
        {
            if (!cached && wb != null)
                wb.close();
        }

        return xrows;
    }

    private static <T> Iterable<T> toIterable(Iterator<T> it)
    {
        return () -> it;
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
                    wb = loadWorkbook(path, false);
                    path2workbook.put(path, wb);
                }
                return wb;
            }
        }
        else
        {
            try (InputStream is = new ByteArrayInputStream(Util.loadResourceAsBytes(path)))
            {
                XSSFWorkbook wb = new XSSFWorkbook(is);
                wb.getCreationHelper().createFormulaEvaluator().evaluateAll();
                return wb;
            }
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
                    Double d = Double.parseDouble(text);
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
