package rtss.losses199x.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import rtss.util.excel.Excel;
import rtss.util.excel.ExcelRC;

public class TableValues
{
    public static Map<Integer,Double> actualBirths(int y1, int y2) throws Exception
    {
        return actualTableValues(y1, y2, "рождения total");
    }
    
    public static Map<Integer,Double> actualDeaths(int y1, int y2) throws Exception
    {
        return actualTableValues(y1, y2, "смерти total");
    }

    public static Map<Integer,Double> actualTableValues(int y1, int y2, String columnTitle) throws Exception
    {
        Map<Integer,Double> m = new LinkedHashMap<>();
        ExcelRC rc = Excel.readSheet("199x-population-births-deaths.xlsx", true, "Data с Крымом");
        
        List<Object> yearColumnValues = rc.columnValues("год");
        for (int nr = 1; nr < yearColumnValues.size(); nr++)
        {
            Object o = yearColumnValues.get(nr);
            if (ExcelRC .isBlank(o))
                continue;
            int year = ExcelRC.asInt(o);
            if (year >= y1 && year <= y2)
            {
                o = rc.rowColumnValue(nr, columnTitle);
                Double value = ExcelRC.asDouble(o);
                m.put(year, value);
            }
        }

        return m;
    }
}
