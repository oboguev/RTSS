package rtss.util.excel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExcelSheet
{
    public final String name;
    
    private final Map<String, Integer> headers;
    private final List<ExcelRow> rows = new ArrayList<>();
    
    public ExcelSheet(String name, Map<String, Integer> headers)
    {
        this.name = name;
        this.headers = headers;
    }
    
    public void addRow(ExcelRow row)
    {
        rows.add(row);
    }

    public Set<String> getColumns()
    {
        return headers.keySet();
    }
    
    public List<ExcelRow> getRows()
    {
        return rows;
    }
}
