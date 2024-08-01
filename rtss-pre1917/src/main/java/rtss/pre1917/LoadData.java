package rtss.pre1917;

import java.util.List;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import rtss.pre1917.data.ColumnHeader;
import rtss.pre1917.data.RC;
import rtss.pre1917.data.Territory;
import rtss.util.Util;
import rtss.util.excel.Excel;

public class LoadData
{
    public static void main(String[] args)
    {
        try
        {
            new LoadData().loadAllData();
            Territory.printSeen();
            Util.out("** Done");
        }
        catch (Throwable ex) 
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }
    
    public void loadAllData() throws Exception
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
    }
    
    private void loadUGVI(String fn) throws Exception
    {
        String fpath = String.format("ugvi/%s.xlsx", fn);
        XSSFWorkbook wb = Excel.loadWorkbook(fpath);
        for (int k = 0; k < wb.getNumberOfSheets(); k++)
        {
            XSSFSheet sheet = wb.getSheetAt(k);
            List<List<Object>> rc = Excel.readSheet(wb, sheet, fpath);
            Map<String, Integer> headers = ColumnHeader.getTopHeaders(sheet, rc);
            
            if (!headers.containsKey("губ"))
                throw new Exception("Нет колоники для губернии");
            int gc = headers.get("губ");
            for (int nr = 1; nr < rc.size(); nr++)
            {
                Object o = RC.get(rc, nr, gc);
                if (o == null)
                    o = "";
                String gub = o.toString();
                gub = Territory.canonic(gub);
            }
            // ###
        }
        wb.close();
    }

    private void loadUGVI(String fn, int y1, int y2) throws Exception
    {
        String fpath = String.format("ugvi/%s.xlsx", fn);
        XSSFWorkbook wb = Excel.loadWorkbook(fpath);
        for (int k = 0; k < wb.getNumberOfSheets(); k++)
        {
            XSSFSheet sheet = wb.getSheetAt(k);
            List<List<Object>> rc = Excel.readSheet(wb, sheet, fpath);
            Map<String, Integer> headers = ColumnHeader.getTopHeaders(sheet, rc);
            
            if (!headers.containsKey("губ"))
                throw new Exception("Нет колоники для губернии");
            int gc = headers.get("губ");
            for (int nr = 1; nr < rc.size(); nr++)
            {
                Object o = RC.get(rc, nr, gc);
                if (o == null)
                    o = "";
                String gub = o.toString();
                gub = Territory.canonic(gub);
            }
            // ###
        }
        wb.close();
    }
}
