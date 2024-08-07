package rtss.mexico.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import rtss.util.Util;
import rtss.util.excel.ExcelRC;

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
    
    public static List<ColumnHeader> getHeaders(XSSFSheet sheet,  ExcelRC rc) throws Exception
    {
        List<ColumnHeader> headers = new ArrayList<>();
        
        int nr1 = sheet.getFirstRowNum();
        int nr2 = sheet.getLastRowNum();
        nr2 = 0;

        for (int nr = 0; nr <= nr2; nr++)
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
                                
                                headers.add(new ColumnHeader(text, nr, nc));
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
        return true;
    }

    public static Map<String, Integer> getTopHeaders(XSSFSheet sheet,  ExcelRC rc) throws Exception
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
    
    public static int getRequiredHeader(Map<String, Integer> headers, String header) throws Exception
    {
        if (!headers.containsKey(header))
            throw new Exception("Missing column " + header);
        return headers.get(header);
    }

}
