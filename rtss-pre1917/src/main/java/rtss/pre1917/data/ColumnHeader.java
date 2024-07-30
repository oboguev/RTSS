package rtss.pre1917.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import rtss.util.Util;

public class ColumnHeader
{
    public String text;
    public int row;
    public int col;
    
    public ColumnHeader(String text, int row, int col)
    {
        this.text= text;
        this.row = row;
        this.col = col;
    }
    
    public static List<ColumnHeader> getHeaders(XSSFSheet sheet,  List<List<Object>> rc) throws Exception
    {
        List<ColumnHeader> headers = new ArrayList<>();
     
        int nrow = 0;
        for (Row row : toIterable(sheet.rowIterator()))
        {
            int ncol = 0;
            for (Cell cell : toIterable(row.cellIterator()))
            {
                if (isHeaderCellStyle(sheet,  cell))
                {
                    Object o = RC.get(rc, nrow, ncol);
                    if (o == null)
                        continue;
                    String text = o.toString().trim();
                    text = Util.despace(text);
                    if (text.length() == 0)
                        continue;
                    
                    headers.add(new ColumnHeader(text, nrow, ncol));
                }
                ncol++;
            }
            nrow++;
        }
     
        return headers;
    }
    
    private static boolean isHeaderCellStyle(XSSFSheet sheet,  Cell cell)
    {
        int fx = cell.getCellStyle().getFontIndex();
        XSSFFont font = sheet.getWorkbook().getFontAt(fx);
        return font.getBold();
    }

    private static <T> Iterable<T> toIterable(Iterator<T> it)
    {
        return () -> it;
    }

    public static Map<String, Integer> getTopHeaders(XSSFSheet sheet,  List<List<Object>> rc) throws Exception
    {
        Map<String, Integer> m = new HashMap<>();
        for (ColumnHeader  h: getHeaders(sheet,  rc))
        {
            if (h.row != 0)
                throw new Exception("Column header not in the top row");
            if (m.containsKey(h.text))
                throw new Exception("Duplicate column header");
            m.put(h.text, h.col);
        }
        return m;
    }
}
