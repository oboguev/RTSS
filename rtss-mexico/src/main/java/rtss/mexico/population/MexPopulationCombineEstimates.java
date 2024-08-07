package rtss.mexico.population;

import java.util.List;
import java.util.Map;

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

    private void do_combine_process(List<List<Object>> rc, Map<String, Integer> headers) throws Exception
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
            
        }
    }
}
