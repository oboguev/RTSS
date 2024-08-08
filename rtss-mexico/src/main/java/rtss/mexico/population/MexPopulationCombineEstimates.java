package rtss.mexico.population;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
        Map<Integer,Double> y2p = new HashMap<>();
        
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
            
            Double pa = rc.asDouble(nr, ixA); 
            Double pb = rc.asDouble(nr, ixB); 
            Double pv = rc.asDouble(nr, ixV); 
            Double pg = rc.asDouble(nr, ixG); 
            Double pd = rc.asDouble(nr, ixD);
            
            if (pb != null)
            {
                pb = pa;
                pb = null;
            }

            addUnique(xs, pa); 
            addUnique(xs, pb); 
            addUnique(xs, pv); 
            addUnique(xs, pg); 
            addUnique(xs, pd); 

            Double pe = rc.asDouble(nr, ixE);
            
            Double v = average(xs);
            
            if (v == null)
                v = pe;
            else if (pe != null)
                v = 1.0/3 * v + 2.0/3 * pe;
            
            y2p.put(year, v);
        }
        
        List<Integer> years = new ArrayList<>(y2p.keySet());
        Collections.sort(years);
        
        interpolate(y2p, 1946, 1953);
        
        years = new ArrayList<>(y2p.keySet());
        Collections.sort(years);
        for (int year : years)
        {
            Double v = y2p.get(year);

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
            if (Math.abs(v - d) <= 1)
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

    private void interpolate(Map<Integer,Double> m, int y1, int y2)
    {
        double yv1 = m.get(y1);
        double yv2 = m.get(y2);
        
        double ym = Math.pow(yv2 / yv1, 1.0 / (y2 - y1));

        double v = yv1;
        for (int y = y1 + 1; y < y2; y++)
        {
            v *= ym;
            m.put(y, v);
        }
    }
}
