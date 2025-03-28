package rtss.mexico.agri.loader;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import rtss.mexico.agri.entities.ArgiConstants;
import rtss.mexico.agri.entities.Culture;
import rtss.mexico.agri.entities.CultureYear;
import rtss.mexico.agri.entities.CultureSet;
import rtss.mexico.agri.entities.RiceKind;
import rtss.util.Util;
import rtss.util.excel.Excel;
import rtss.util.excel.ExcelRC;
import rtss.util.excel.ExcelRow;
import rtss.util.excel.ExcelSheet;
import rtss.util.excel.ExcelWorkbook;
import rtss.util.excel.FindCells;
import rtss.util.excel.RowCol;

public class LoadAE
{
    public static void main(String[] args)
    {
        try
        {
            CultureSet c = load();
            c = loadEarly();
            Util.unused(c);
            Util.out("** Done");
        }
        catch (Exception ex)
        {
            Util.err("** Exception");
            ex.printStackTrace();
        }
    }

    private final CultureSet cultures = new CultureSet();
    private String currentFile = null;
    private Integer currentNR = null;

    public static CultureSet load() throws Exception
    {
        return new LoadAE().do_load();
    }

    public static CultureSet loadEarly() throws Exception
    {
        return new LoadAE().do_load_early();
    }

    private CultureSet do_load() throws Exception
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
            productionMultiplier *= ArgiConstants.RawRiceToWhiteRice;
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
            if (cy.isAllNull())
                c.deleteYear(cy);
        }

        // TODO: загрузить импорт экспорт 1983-1986
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

    private CultureSet do_load_early() throws Exception
    {
        final String fnProduction = "agriculture/Anuario-Estadístico/production-early.xlsx";

        try
        {
            currentFile = fnProduction;

            ExcelWorkbook wb = ExcelWorkbook.load(fnProduction);
            for (ExcelSheet sheet : wb.getSheets())
            {
                String sname = sheet.name;
                if (sname == null)
                    sname = "";
                String lcname = sname.toLowerCase();
                if (lcname.contains("note") || lcname.contains("template"))
                    continue;
                loadProductionEarly(sheet, Util.despace(sname).trim());
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

    private void loadProductionEarly(ExcelSheet sheet, String cname) throws Exception
    {
        RiceKind rice_kind = null;

        if (cname.equalsIgnoreCase("arroz palay"))
        {
            cname = "arroz";
            rice_kind = RiceKind.RAW;
        }
        else if (cname.equalsIgnoreCase("arroz"))
        {
            rice_kind = RiceKind.WHITE;
        }

        Culture c = new Culture(cname, null);
        cultures.add(c);

        for (ExcelRow row : sheet.getRows())
        {
            int year = row.asInteger("год");
            CultureYear cy = c.makeCultureYear(year);

            cy.surface = row.asDouble("га");
            cy.yield = row.asDouble("кг/га");
            cy.production = row.asDouble("кг");
            if (cy.production != null)
                cy.production /= 1000.0;

            cy.rice_kind = rice_kind;

            if (cy.rice_kind == RiceKind.RAW)
            {
                cy.production_raw = cy.production;
                cy.production  *= ArgiConstants.RawRiceToWhiteRice;
                cy.rice_kind = RiceKind.WHITE;
            }

            if (cy.isAllNull())
                c.deleteYear(cy);
        }
    }
}
