package rtss.mexico.agri.loader;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import rtss.mexico.agri.entities.Culture;
import rtss.mexico.agri.entities.Cultures;
import rtss.util.Util;
import rtss.util.excel.Excel;
import rtss.util.excel.ExcelRC;
import rtss.util.excel.FindCells;
import rtss.util.excel.RowCol;

public class LoadSARH
{
    public static void main(String[] args)
    {
        try
        {
            Cultures c = load();
            Util.unused(c);
        }
        catch (Exception ex)
        {
            Util.err("** Exception");
            ex.printStackTrace();
        }
    }

    private final Cultures cultures = new Cultures();
    private String currentFile = null;
    private Integer currentNR = null;

    public static Cultures load() throws Exception
    {
        return new LoadSARH().do_load();
    }

    private Cultures do_load() throws Exception
    {
        loadFile("annual", "A");
        loadFile("annual", "B-C");
        loadFile("annual", "E-J");
        loadFile("annual", "L-R");
        loadFile("annual", "S-Z");
        loadFile("perennial", "A-C");
        loadFile("perennial", "D-L");
        loadFile("perennial", "M-P");
        loadFile("perennial", "T-V");
        loadFile("semi-perennial", "A-P");

        return cultures;
    }

    private void loadFile(String category, String part) throws Exception
    {
        loadFilePath(category, String.format("agriculture/SARH-Consumos-aparentes/SARH-Consumos-aparentes-%s-crops-%s.xlsx", category, part));
    }

    private void loadFilePath(String category, String fn) throws Exception
    {
        currentFile = fn;

        try (XSSFWorkbook wb = Excel.loadWorkbook(fn))
        {
            for (int k = 0; k < wb.getNumberOfSheets(); k++)
            {
                XSSFSheet sheet = wb.getSheetAt(k);
                String sname = sheet.getSheetName();
                if (sname == null)
                    sname = "";
                sname = Util.despace(sname).trim();
                if (sname.toLowerCase().contains("note") || sname.toLowerCase().equals("template"))
                    continue;
                ExcelRC rc = Excel.readSheet(wb, sheet, currentFile);
                loadCulture(category, sname, rc);
            }
        }

        currentFile = null;
    }

    private void loadCulture(String category, String cname, ExcelRC rc) throws Exception
    {
        Util.out(String.format("%s/%s", category, cname));
        
        Culture c = new Culture(cname, category);
        if (cultures.contains(c.name))
            throw new Exception("Duplicate culture");
        
        int colYear = 0;
        int colSurface = FindCells.findRequiredVerticalCells(rc, "площадь", "га").col;
        int colYield = FindCells.findRequiredVerticalCells(rc, "урожайность", "кг/га").col;
        int colProduction = FindCells.findRequiredVerticalCells(rc, "урожай", "тонн").col;
        int colImport = FindCells.findRequiredVerticalCells(rc, "импорт", "тонн").col;
        int colExport = FindCells.findRequiredVerticalCells(rc, "экспорт", "тонн").col;
        int colConsumption = FindCells.findRequiredVerticalCells(rc, "национальное", "тонн").col;
        int colPerCapita = FindCells.findRequiredVerticalCells(rc, "душевое", "кг на душу").col;
        RowCol rcAlcohol = FindCells.findVerticalCells(rc, "алкоголь", "литров");
        
        for (int nr = 0; nr < rc.size() && !rc.isEndRow(nr); nr++)
        {
            currentNR = nr;
            
            String s = rc.asString(nr, colYear);
            if (s == null || s.length() == 0)
                continue;
            
            // ###
        }
            
        Util.noop();
        
        // ###
    }
}
