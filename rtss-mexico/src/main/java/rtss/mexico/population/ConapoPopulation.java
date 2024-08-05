package rtss.mexico.population;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import rtss.mexico.util.ColumnHeader;
import rtss.mexico.util.RC;
import rtss.util.Util;
import rtss.util.excel.Excel;

public class ConapoPopulation
{
    public static void main(String[] args)
    {
        try
        {
            org.apache.poi.util.IOUtils.setByteArrayMaxOverride(300_000_000);
            Util.out("Население Мексики на середину года:");
            Util.out("");
            new ConapoPopulation().do_main("conapo/ConDem50a19_ProyPob20a70/0_Pob_Mitad_1950_2070.xlsx");

            Util.noop();
        }
        catch (Throwable ex)
        {
            Util.err("*** Exception");
            ex.printStackTrace();
        }
    }
    
    private void do_main(String fpath) throws Exception
    {
        try (XSSFWorkbook wb = Excel.loadWorkbook(fpath))
        {
            if (wb.getNumberOfSheets() != 1)
                throw new Exception("Unexpected multiple sheets in file");
            XSSFSheet sheet = wb.getSheetAt(0);
            List<List<Object>> rc = Excel.readSheet(wb, sheet, fpath);
            Map<String, Integer> headers = ColumnHeader.getTopHeaders(sheet, rc);
            do_process(rc, headers);
        }
    }
    
    static class YearData
    {
        /* for the whole country */
        Map<String, Integer> whole = new HashMap<>();
        
        /* split by territories */
        Map<String, Integer> split = new HashMap<>();
        
        /* line for the key */
        Map<String, Integer> line = new HashMap<>();
    }
    
    private Map<Integer, YearData> years = new HashMap<>(); 
    
    private void do_process(List<List<Object>> rc, Map<String, Integer> headers) throws Exception
    {
        int ixYear = ColumnHeader.getRequiredHeader(headers, "AÑO");
        int ixEntityCode = ColumnHeader.getRequiredHeader(headers, "CVE_GEO");
        int ixAge = ColumnHeader.getRequiredHeader(headers, "EDAD");
        int ixGender = ColumnHeader.getRequiredHeader(headers, "SEXO");
        int ixPopulation = ColumnHeader.getRequiredHeader(headers, "POBLACION");
        
        for (int nr = 1; nr < rc.size(); nr++)
        {
            int year = asInt(rc, nr, ixYear);
            int entityCode = asInt(rc, nr, ixEntityCode);
            int age = asInt(rc, nr, ixAge);
            String gender = asString(rc, nr, ixGender);
            int population = asInt(rc, nr, ixPopulation);
            
            YearData yd = years.get(year);
            if (yd == null)
            {
                yd = new YearData();
                years.put(year, yd);
            }
            
            if (entityCode == 0)
            {
                String key = age + "." + gender.toLowerCase();
                
                if (yd.whole.containsKey(key))
                    throw new Exception("Duplicate data");
                yd.whole.put(key, population);
                yd.line.put(key, nr);
            }
            else
            {
                String key = age + "." + entityCode + "." + gender.toLowerCase();
                
                if (yd.split.containsKey(key) && yd.split.get(key) != population)
                {
                    String msg = String.format("Duplicate data, sheet lines %d and %d", yd.line.get(key) + 1, nr + 1);
                    throw new Exception(msg);
                }
                yd.split.put(key, population);
                yd.line.put(key, nr);
            }
        }
        
        List<Integer> ylist = new ArrayList<>(years.keySet());
        Collections.sort(ylist);
        for (int year : ylist)
        {
            YearData yd = years.get(year);
            int count = 0;
            
            if (yd.whole.isEmpty())
            {
                count = sumValues(yd.split);
            }
            else if (yd.split.isEmpty())
            {
                count = sumValues(yd.whole);
            }
            else
            {
                int c1 = sumValues(yd.split);
                int c2 = sumValues(yd.whole);
                if (c1 != c2)
                    throw new Exception("Conflicting whole/split values");
                count = c1;
            }
            
            Util.out(String.format("%s %d", year, count));
        }
        
        Util.noop();
    }
    
    private String asString(List<List<Object>> rc, int nr, int nc) throws Exception
    {
        Object o = RC.get(rc, nr, nc);
        if (o == null)
            return null;
        String s = o.toString();
        s = Util.despace(s).trim();
        return s;
    }

    private int asInt(List<List<Object>> rc, int nr, int nc) throws Exception
    {
        Object o = RC.get(rc, nr, nc);
        
        if (o == null)
            throw new Exception("Missing integer data");

        if (o instanceof Integer)
            return ((Integer)o).intValue(); 
        
        if (o instanceof Long)
            return ((Long)o).intValue(); 

        String s = asString(rc, nr, nc);
        if (s.endsWith(".0"))
            s = Util.stripTail(s, ".0");
        return Integer.parseInt(s);
    }
    
    private int sumValues(Map<String, Integer> m)
    {
        int sum = 0;
        
        for (int v : m.values())
            sum += v;
        
        return sum;
    }
}
