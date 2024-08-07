package rtss.mexico.population;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import rtss.mexico.util.ColumnHeader;
import rtss.util.Util;
import rtss.util.excel.Excel;
import rtss.util.excel.ExcelRC;

public class MexPopulationCombineEstimates
{
    public static void main(String[] args)
    {
        try
        {
            new MexPopulationCombineEstimates().do_combine();
        }
        catch (Throwable ex)
        {
            Util.err("*** Exception");
            ex.printStackTrace();
        }
    }

    private void do_combine() throws Exception
    {
        org.apache.poi.util.IOUtils.setByteArrayMaxOverride(300_000_000);
        final String fpath = "mexico-population-estimates.xlsx";

        try (XSSFWorkbook wb = Excel.loadWorkbook(fpath))
        {
            if (wb.getNumberOfSheets() != 1)
                throw new Exception("Unexpected multiple sheets in file");
            XSSFSheet sheet = wb.getSheetAt(0);
            ExcelRC rc = Excel.readSheet(wb, sheet, fpath);
            Map<String, Integer> headers = ColumnHeader.getTopHeaders(sheet, rc);
            do_combine_process(rc, headers);
        }
    }

    private void do_combine_process(ExcelRC rc, Map<String, Integer> headers) throws Exception
    {
        int ixYear = ColumnHeader.getRequiredHeader(headers, "год");
        int ixA = ColumnHeader.getRequiredHeader(headers, "А");
        int ixB = ColumnHeader.getRequiredHeader(headers, "Б");
        int ixV = ColumnHeader.getRequiredHeader(headers, "В");
        int ixG = ColumnHeader.getRequiredHeader(headers, "Г");
        int ixD = ColumnHeader.getRequiredHeader(headers, "Д");
        int ixE = ColumnHeader.getRequiredHeader(headers, "Е");
        
        for (int nr = 1; nr < rc.size(); nr++)
        {
            if (rc.isEmpty(nr, ixYear))
                continue;
            
            Set<Double> xs = new HashSet<>();
            
            int year = rc.asRequiredInt(nr, ixYear);
            addUnique(xs, rc.asDouble(nr, ixA)); 
            addUnique(xs, rc.asDouble(nr, ixB)); 
            addUnique(xs, rc.asDouble(nr, ixV)); 
            addUnique(xs, rc.asDouble(nr, ixG)); 
            addUnique(xs, rc.asDouble(nr, ixD)); 
            // add(xs, rc.asDouble(nr, ixE)); 

            Double e = rc.asDouble(nr, ixE);
            
            Double v = average(xs);
            
            if (v == null)
                v = e;
            else if (e != null)
                v = 1.0/3 * v + 2.0/3 * e;
            
            String sv = "";
            if (v != null) 
                sv = String.format("%,3d", Math.round(v));
            
            Util.out(String.format("%d %s", year, sv));
        }
    }
    
    private void addUnique(Set<Double> xs, Double d)
    {
        if (d == null)
            return;

        for (double v : xs)
        {
            if (Math.abs(v - d) < 1)
                return;
        }
        
        xs.add(d);
    }
    
    private Double average(Set<Double> xs)
    {
        if (xs.size() == 0)
            return null;
        
        double v = 0;
        for (double x : xs)
            v += x;
        
        return v / xs.size();
    }
}
