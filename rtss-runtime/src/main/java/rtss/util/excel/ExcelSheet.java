package rtss.util.excel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExcelSheet
{
    public final String name;
    
    private Set<String> columns = new HashSet<>();
    private List<ExcelRow> rows = new ArrayList<>();
    
    public ExcelSheet(String name)
    {
        this.name = name;
    }
    
    public void addRow(ExcelRow row)
    {
        rows.add(row);
    }

    public Set<String> getColumns()
    {
        return columns;
    }
    
    public List<ExcelRow> getRows()
    {
        return rows;
    }
}
