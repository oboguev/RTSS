package rtss.mexico.agri.loader;

import java.util.HashMap;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import rtss.mexico.agri.entities.Culture;
import rtss.mexico.agri.entities.CultureYear;
import rtss.mexico.agri.entities.Cultures;
import rtss.util.Util;
import rtss.util.excel.Excel;
import rtss.util.excel.ExcelRC;
import rtss.util.excel.ExcelRow;
import rtss.util.excel.ExcelSheet;
import rtss.util.excel.ExcelWorkbook;
import rtss.util.excel.FindCells;
import rtss.util.excel.RowCol;

public class LoadSARH
{
    public static void main(String[] args)
    {
        try
        {
            Cultures c = load();
            Map<Integer, Long> m = loadPopulation(); 
            Util.unused(c, m);
            Util.out("** Done");
        }
        catch (Exception ex)
        {
            Util.err("** Exception");
            ex.printStackTrace();
        }
    }

    /* ================================= */

    private final Cultures cultures = new Cultures();
    private String currentFile = null;
    private Integer currentNR = null;

    public static Cultures load() throws Exception
    {
        return new LoadSARH().do_load();
    }

    private Cultures do_load() throws Exception
    {
        try
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
        catch (Exception ex)
        {
            if (currentNR == null)
            {
                throw ex;
            }
            else
            {
                String msg = String.format("row = %d", currentNR + 1);
                throw new Exception(msg, ex);
            }
        }
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
        cultures.add(c);

        int colYear = 0;
        int colSurface = FindCells.findRequiredVerticalCells(rc, "площадь", "га").col;
        int colYield = FindCells.findRequiredVerticalCells(rc, "урожайность", "кг/га").col;
        int colProduction = FindCells.findRequiredVerticalCells(rc, "урожай", "тонн").col;
        int colImport = FindCells.findRequiredVerticalCells(rc, "импорт", "тонн").col;
        int colExport = FindCells.findRequiredVerticalCells(rc, "экспорт", "тонн").col;
        int colConsumption = FindCells.findRequiredVerticalCells(rc, "национальное", "тонн").col;
        int colPerCapita = FindCells.findRequiredVerticalCells(rc, "душевое", "кг на душу").col;
        RowCol rcAlcohol = FindCells.findVerticalCells(rc, "алкоголь", "литров");
        RowCol rcCleaned = FindCells.findVerticalCells(rc, "очищенный", "тонн");

        for (int nr = 0; nr < rc.size() && !rc.isEndRow(nr); nr++)
        {
            currentNR = nr;

            String s = rc.asString(nr, colYear);
            if (s == null || s.length() == 0)
                continue;

            CultureYear cy;

            if (s.startsWith("Promedio "))
            {
                cy = c.makeAverageCultureYear(s);
            }
            else
            {
                int year = rc.asInt(nr, colYear);
                if (year < 1880 || year >= 2030)
                    throw new Exception("Incorrect year " + s);
                cy = c.makeCultureYear(year);
            }

            cy.surface = asDouble(rc, nr, colSurface);
            cy.yield = asDouble(rc, nr, colYield);
            cy.production = asDouble(rc, nr, colProduction);
            cy.importAmount = asDouble(rc, nr, colImport);
            cy.exportAmount = asDouble(rc, nr, colExport);
            cy.consumption = asDouble(rc, nr, colConsumption);
            cy.perCapita = asDouble(rc, nr, colPerCapita);

            if (rcAlcohol != null)
                cy.alcohol = asDouble(rc, nr, rcAlcohol.col);
            
            if (rcCleaned != null && cname.equalsIgnoreCase("arroz"))
            {
                cy.production_raw = cy.production;
                cy.production = asDouble(rc, nr, rcCleaned.col);
            }

            if (cy.isAllNull())
                c.deleteYear(cy);
        }
    }

    private Double asDouble(ExcelRC rc, int nr, int nc) throws Exception
    {
        String s = rc.asString(nr, nc);
        if (s != null)
        {
            if (Util.despace(s).trim().equals("-"))
                return null;

        }

        return rc.asDouble(nr, nc);
    }

    /* ================================= */

    private static Map<Integer, Long> cachedLoadPopulation;

    public static Map<Integer, Long> loadPopulation() throws Exception
    {
        if (cachedLoadPopulation != null)
            return cachedLoadPopulation;

        Map<Integer, Long> m = new HashMap<>();

        ExcelWorkbook wb = ExcelWorkbook.load("agriculture/SARH-Consumos-aparentes/SARH-population.xlsx");
        ExcelSheet sheet = wb.getTheOnlySheet();
        for (ExcelRow row : sheet.getRows())
        {
            int year = row.asInteger("год");
            long pop = row.asLong("население");
            m.put(year, pop);
        }

        cachedLoadPopulation = m;

        return m;
    }
}
