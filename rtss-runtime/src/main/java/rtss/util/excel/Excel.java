package rtss.util.excel;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import rtss.util.Util;

public class Excel
{
    public static List<List<Object>> readSheet(String path, String sheetName) throws Exception
    {
        List<List<Object>> xrows = new ArrayList<>();

        try (
             InputStream is = new ByteArrayInputStream(Util.loadResourceAsBytes(path));
             XSSFWorkbook wb = new XSSFWorkbook(is))
        {
            XSSFSheet sheet = wb.getSheet(sheetName);
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
                        break;

                    case STRING:
                        xrow.add(cell.getStringCellValue());
                        break;

                    case NUMERIC:
                        xrow.add(cell.getNumericCellValue());
                        break;

                    default:
                        throw new Exception(String.format("Unsupported cell type in resource file %s, row=%d, col=%d", path, nrow + 1, ncol + 1));
                    }

                    ncol++;
                }

                xrows.add(xrow);
                nrow++;
            }
        }

        return xrows;
    }

    private static <T> Iterable<T> toIterable(Iterator<T> it)
    {
        return () -> it;
    }
}
