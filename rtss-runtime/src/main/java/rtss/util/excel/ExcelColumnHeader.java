package rtss.util.excel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import rtss.util.Util;

public class ExcelColumnHeader
{
    public String text;
    public int row;
    public int col;
    
    public ExcelColumnHeader(String text, int row, int col)
    {
        this.text= text;
        this.row = row;
        this.col = col;
    }
    
    public static List<ExcelColumnHeader> getHeaders(XSSFSheet sheet, ExcelRC rc) throws Exception
    {
        List<ExcelColumnHeader> headers = new ArrayList<>();
        
        int nr1 = sheet.getFirstRowNum();
        int nr2 = sheet.getLastRowNum();

        for (int nr = 0; nr <= nr2 && !rc.isEndRow(nr); nr++)
        {
            if (nr >= nr1 && sheet.getRow(nr) != null)
            {
                XSSFRow row = sheet.getRow(nr);
                int nc1 = row.getFirstCellNum();
                int nc2 = row.getLastCellNum();
                
                for (int nc = 0; nc <= nc2; nc++)
                {
                    if (nc >= nc1)
                    {
                        XSSFCell cell = row.getCell(nc,  Row.MissingCellPolicy.RETURN_NULL_AND_BLANK);
                        if (cell != null)
                        {
                            if (isHeaderCellStyle(sheet,  cell))
                            {
                                Object o = rc.get(nr, nc);
                                if (o == null)
                                    continue;
                                String text = o.toString().trim();
                                text = Util.despace(text);
                                if (text.length() == 0)
                                    continue;
                                
                                headers.add(new ExcelColumnHeader(text, nr, nc));
                            }
                        }
                    }

                }
            }
        }
     
        return headers;
    }
    
    private static boolean isHeaderCellStyle(XSSFSheet sheet,  Cell cell)
    {
        int fx = cell.getCellStyle().getFontIndex();
        XSSFFont font = sheet.getWorkbook().getFontAt(fx);
        return font.getBold();
    }

    public static Map<String, Integer> getTopHeaders(XSSFSheet sheet,  ExcelRC rc) throws Exception
    {
        Map<String, Integer> m = new HashMap<>();
        
        for (ExcelColumnHeader h : getHeaders(sheet,  rc))
        {
            if (h.row != 0)
                throw new Exception("Column header not in the top row");
            if (h.text.equals("v"))
                continue;
            if (m.containsKey(h.text))
                throw new Exception("Duplicate column header");
            m.put(h.text, h.col);
        }

        return m;
    }
}
