package rtss.mexico.agri.loader;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import rtss.mexico.agri.entities.Culture;
import rtss.mexico.agri.entities.CultureYear;
import rtss.mexico.agri.entities.Cultures;
import rtss.mexico.agri.entities.RiceKind;
import rtss.util.Util;
import rtss.util.excel.Excel;
import rtss.util.excel.ExcelRC;
import rtss.util.excel.FindCells;
import rtss.util.excel.RowCol;

public class LoadAE
{
    public static void main(String[] args)
    {
        try
        {
            Cultures c = load();
            Util.unused(c);
            Util.out("** Done");
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
        return new LoadAE().do_load();
    }

    private Cultures do_load() throws Exception
    {
        final String fnProduction = "agriculture/Anuario-Estadístico/production.xlsx";

        try
        {
            currentFile = fnProduction;

            try (XSSFWorkbook wb = Excel.loadWorkbook(fnProduction))
            {
                for (int k = 0; k < wb.getNumberOfSheets(); k++)
                {
                    XSSFSheet sheet = wb.getSheetAt(k);
                    String sname = sheet.getSheetName();
                    if (sname == null)
                        sname = "";
                    if (sname.contains("note"))
                        continue;
                    ExcelRC rc = Excel.readSheet(wb, sheet, currentFile);
                    loadProduction(rc);
                }
            }

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

    private void loadProduction(ExcelRC rc) throws Exception
    {
        loadProduction(rc, "маис");
        loadProduction(rc, "пшеница");
        loadProduction(rc, "фасоль");
        loadProduction(rc, "рис сырец");
        loadProduction(rc, "ячмень");
        loadProduction(rc, "плантаны роатан");
        loadProduction(rc, "плантаны другие");
        loadProduction(rc, "сорго (зерно)");
        loadProduction(rc, "соя");

        joinPlantains();
    }

    private void loadProduction(ExcelRC rc, String cname) throws Exception
    {
        RowCol rcYear = FindCells.findRequiredVerticalCells(rc, "", "год");
        RowCol rcProduction = FindCells.findRequiredVerticalCells(rc, cname, "тыс. тонн");
        RowCol rcSurface = rcProduction.offset(0, 1);
        String s = rc.asString(rcSurface.row + 1, rcSurface.col);
        if (s == null || !s.equals("тыс. га"))
            throw new Exception("Unexpected worksheet layout");
        
        double productionMultiplier = 1000.0;
        double surfaceMultiplier = 1000.0;
        RiceKind rice_kind = null;

        if (cname.equals("рис сырец"))
        {
            cname = "рис";
            productionMultiplier *= 0.66;
            rice_kind = RiceKind.WHITE;
        }

        Culture c = new Culture(cname, null);
        cultures.add(c);

        for (int nr = rcYear.row + 2; nr < rc.size() && !rc.isEndRow(nr); nr++)
        {
            s = rc.asString(nr, rcYear.col);
            if (s == null)
                continue;
            s = Util.despace(s).trim();
            if (s.equals(""))
                continue;
            if (s.endsWith(".0"))
                s = Util.stripTail(s, ".0");
            int year = Integer.parseInt(s);
            if (year < 1881 || year > 2030)
                throw new Exception("Incorrect year " + year);

            CultureYear cy = c.makeCultureYear(year);
            cy.production = value(rc, nr, rcProduction.col, productionMultiplier);
            cy.surface = value(rc, nr, rcSurface.col, surfaceMultiplier);
            cy.rice_kind = rice_kind;
        }
        
        // ### импорт экспорт 1983-1986
    }

    private Double value(ExcelRC rc, int nr, int nc, double multiplier) throws Exception
    {
        String s = rc.asString(nr, nc);
        if (s == null)
            s = "";
        if (s.length() == 0 || s.equals("(a)"))
            return null;
        double v = Double.parseDouble(s);
        return v * multiplier;
    }

    private void joinPlantains() throws Exception
    {
        Culture c1 = cultures.get("плантаны роатан");
        Culture c2 = cultures.get("плантаны другие");

        Culture c = new Culture("плантаны разные", null);
        
        for (int year = 1880; year <= 2050; year++)
        {
            CultureYear cy1 = c1.cultureYear(year);
            CultureYear cy2 = c1.cultureYear(year);
            
            if (cy1 == null && cy2 == null)
                continue;
            
            CultureYear cy = c.makeCultureYear(year);
            
            if (cy1 != null && cy2 != null)
            {
                cy.copyValues(cy1);   
                cy.addValues(cy2);   
            }
            else if (cy1 != null)
            {
                cy.copyValues(cy1);   
                
            }
            else if (cy2 != null)
            {
                cy.copyValues(cy2);   
            }
        }

        cultures.remove(c1);
        cultures.remove(c2);
        cultures.add(c);
    }
}
