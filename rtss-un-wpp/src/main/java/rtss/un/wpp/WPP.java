package rtss.un.wpp;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import rtss.util.Util;
import rtss.util.excel.Excel;
import rtss.util.excel.ExcelRC;

public class WPP implements AutoCloseable
{
    private XSSFWorkbook wb;
    private XSSFSheet sheet;
    private ExcelRC rc;
    private int iyTitleRow;
    private int ixCountry;
    private int ixYear;
    private Map<String,Integer> columnTitles;

    public WPP(String fpath, String sheetTitle) throws Exception
    {
        try
        {
            wb = Excel.loadWorkbook(fpath);

            for (int k = 0; k < wb.getNumberOfSheets(); k++)
            {
                XSSFSheet sheet = wb.getSheetAt(k);
                String sname = sheet.getSheetName();
                if (sname != null && sname.trim().equalsIgnoreCase(sheetTitle))
                {
                    this.sheet = sheet;
                    break;
                }
            }
            
            if (sheet == null)
                throw new Exception("Unable to locate requested sheet in Excel workbook");
            
            rc = Excel.readSheet(wb, sheet, fpath);
            locateTitleRow(); 
        }
        catch (Exception ex)
        {
            close();
            throw ex;
        }
    }

    @Override
    public void close()
    {
        if (wb != null)
        {
            try
            {
                wb.close();
            }
            catch (IOException ex)
            {
                Util.err("Unable tp close Excel workbook");
                ex.printStackTrace();
            }

            wb = null;
        }
    }
    
    public static Object rc(List<List<Object>> rc, int row, int col)
    {
        if (row >= rc.size())
            return null;
        List<Object> r = rc.get(row);
        if (col >= r.size())
            return null;
        return r.get(col);
    }
    
    public Object rc(int row, int col)
    {
        return rc(this.rc, row, col);
    }
    
    private void locateTitleRow() throws Exception
    {
        for (int nr = 0; nr < 100 && nr < rc.size(); nr++)
        {
            Object c0 = rc(nr, 0);
            Object c1 = rc(nr, 1);
            Object c2 = rc(nr, 2);
            
            if (c0 != null && c1 != null && c2 != null)
            {
                String s0 = c0.toString().trim();
                String s1 = c1.toString().trim();
                String s2 = c2.toString().trim();
                s2 = Util.despace(s2);
                
                if (s0.equals("Index") && s1.equals("Variant") && s2.contains("Region"))
                {
                    this.iyTitleRow = nr;
                    columnTitles = new HashMap<>();
                    
                    for (int nc = 0; nc < rc.get(nr).size(); nc++)
                    {
                        Object o = rc(nr, nc);
                        if (o != null)
                            columnTitles.put(Util.despace(o.toString()).trim(), nc);
                    }
                    
                    ixYear = columnTitles.get("Year");
                    ixCountry = 2;
                    
                    return;
                }
            }
        }
        
        throw new Exception("Unable to locate title row");
    }

    /**
     * Extract country values year -> (key -> value)
     */
    public Map<Integer, Map<String, Object>> forCountry(String country) throws Exception
    {
        Map<Integer, Map<String, Object>> m = new HashMap<>();
        
        for (int nr = iyTitleRow + 1; nr < rc.size(); nr++)
        {
            List<Object> row = rc.get(nr);
            Object oc = row.get(ixCountry);
            Object oy = row.get(ixYear); 
            if (oc == null || oy == null)
                continue;
            
            String xcountry = Util.despace(oc.toString()).trim();
            if (xcountry.equals(country))
            {
                int year = ExcelRC.asInt(oy);
                if (m.containsKey(year))
                    throw new Exception("Duplicate year data");
                
                Map<String, Object> mm = new HashMap<>();
                m.put(year, mm);
                
                for (String title : columnTitles.keySet())
                    mm.put(title, row.get(columnTitles.get(title)));
            }
        }

        return m;
    }

    /* ============================================================================================ */
    
    public String asString(int nr, int nc) throws Exception
    {
        return rc.asString(nr, nc);
    }
    
    public double asDouble(int nr, int nc) throws Exception
    {
        return rc.asRequiredDouble(nr, nc);
    }

    public long asLong(int nr, int nc) throws Exception
    {
        return rc.asRequiredLong(nr, nc);
    }

    public int asInt(int nr, int nc) throws Exception
    {
        return rc.asRequiredInt(nr, nc);
    }
}