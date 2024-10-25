package rtss.util.excel;

import java.util.HashSet;
import java.util.Set;

public class ExcelWorkbook
{
    private Set<ExcelSheet> sheets = new HashSet<>();
    
    public void addSheet(ExcelSheet sheet)
    {
        sheets.add(sheet);
    }

    public Set<ExcelSheet> getSheets()
    {
        return sheets;
    }
}
