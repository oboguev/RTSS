package rtss.util.excel;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelWorkbook
{
    private Set<ExcelSheet> sheets = new HashSet<>();
    private String source = "<unknown source>";
    
    public ExcelWorkbook()
    {
    }

    public ExcelWorkbook(String source)
    {
        this.source = source;
    }


    public void addSheet(ExcelSheet sheet)
    {
        sheets.add(sheet);
    }

    public Set<ExcelSheet> getSheets()
    {
        return sheets;
    }
    
    public ExcelSheet getTheOnlySheet() throws Exception
    {
        if (sheets.size() != 1)
            throw new Exception(String.format("Excel file %s has %d worksheets other than notes, expected to have exactly one", source, sheets.size()));
        
        for (ExcelSheet sheet : sheets)
            return sheet;
        
        throw new Exception(String.format("Excel file %s has no worksheets other than notes, expected to have exactly one", source));
    }

    public static ExcelWorkbook load(String filepath) throws Exception
    {
        ExcelWorkbook wb = new ExcelWorkbook(filepath);
        wb.do_load(filepath);
        return wb;
    }

    private void do_load(String filepath) throws Exception
    {
        try (XSSFWorkbook wb = Excel.loadWorkbook(filepath))
        {
            for (int k = 0; k < wb.getNumberOfSheets(); k++)
            {
                XSSFSheet sheet = wb.getSheetAt(k);
                String sname = sheet.getSheetName();

                if (sname != null && sname.trim().toLowerCase().contains("note"))
                    continue;

                ExcelRC rc = Excel.readSheet(wb, sheet, filepath);
                Map<String, Integer> headers = ExcelColumnHeader.getTopHeaders(sheet, rc);
                
                ExcelSheet esheet = new ExcelSheet(sname, headers);
                addSheet(esheet);
                loadSheet(rc, headers, esheet);
            }
        }
    }
    
    private void loadSheet(ExcelRC rc, Map<String, Integer> headers, ExcelSheet esheet) throws Exception
    {
        for (int nr = 1; nr < rc.size() && !rc.isEndRow(nr); nr++)
        {
            ExcelRow row = new ExcelRow(esheet, nr);
            esheet.addRow(row);

            for (String colname : headers.keySet())
            {
                int nc = headers.get(colname);
                Object o = rc.get(nr, nc);
                row.setCellValue(colname, o);
            }
        }
    }
}
