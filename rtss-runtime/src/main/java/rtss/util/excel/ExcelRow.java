package rtss.util.excel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ExcelRow
{
    private Map<String,Object> cells = new HashMap<>();
    private final ExcelSheet sheet;
    private final int nr;
    
    public ExcelRow(ExcelSheet sheet, int nr)
    {
        this.sheet = sheet;
        this.nr = nr;
    }
    
    public int getRowNumber()
    {
        return nr;
    }
    
    public void setgetCellValue(String column, Object value) 
    {
        cells.put(column, value);
    }
    
    public Set<String> getColumns()
    {
        return sheet.getColumns();
    }
    
    public Object getCellValue(String column)
    {
        return cells.get(column);
    }
}
