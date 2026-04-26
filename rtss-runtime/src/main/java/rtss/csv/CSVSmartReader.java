package rtss.csv;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;

import com.opencsv.CSVReader;

import rtss.util.Util;

public class CSVSmartReader
{
    public static CSVSmartReader fromFile(String path) throws Exception
    {
        return fromText(Util.readFileAsString(path));
    }
    
    public static CSVSmartReader fromResource(String path) throws Exception
    {
        return fromText(Util.loadResource(path));
    }

    public static CSVSmartReader fromText(String text) throws Exception
    {
        CSVReader reader = new CSVReader(new StringReader(text));
        List<String[]> lines = reader.readAll();
        reader.close();
        if (!(lines instanceof RandomAccess))
            lines = new ArrayList<>(lines);
        String[] header = lines.remove(0);
        return new CSVSmartReader(header, lines);
    }
    
    /* =========================================================== */
    
    private List<String[]> rows;
    private Map<String, Integer> title2col = new HashMap<>();
    
    private CSVSmartReader(String[] header, List<String[]> rows) throws Exception
    {
        this.rows = rows;
        
        for (int k = 0; k < header.length; k++)
        {
            String title = header[k].trim();
            if (title2col.containsKey(title))
                throw new Exception("Duplicate column title: " + title);
            title2col.put(title, k);
        }
    }
    
    public int rowCount()
    {
        return rows.size();
    }
    
    public int column(String title)
    {
        Integer ic = this.title2col.get(title);
        
        if (ic == null)
            return -1;
        else
            return ic;
    }
    
    public String asString(int nr, int nc)
    {
        return rows.get(nr)[nc].trim();
    }

    public int asInt(int nr, int nc)
    {
        String sv = asString(nr, nc);
        return Integer.parseInt(sv);
    }

    public double asDouble(int nr, int nc)
    {
        String sv = asString(nr, nc);
        return Double.parseDouble(sv);
    }

    public String asString(int nr, String col)
    {
        return asString(nr, column(col));
    }

    public int asInt(int nr, String col)
    {
        return asInt(nr, column(col));
    }

    public double asDouble(int nr, String col)
    {
        return asDouble(nr, column(col));
    }
}
