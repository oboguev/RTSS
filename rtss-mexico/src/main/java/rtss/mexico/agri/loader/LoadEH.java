package rtss.mexico.agri.loader;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import rtss.mexico.agri.entities.ArgiConstants;
import rtss.mexico.agri.entities.Culture;
import rtss.mexico.agri.entities.CultureYear;
import rtss.mexico.agri.entities.RiceKind;
import rtss.mexico.agri.entities.CultureSet;
import rtss.util.Util;
import rtss.util.excel.Excel;
import rtss.util.excel.ExcelRC;
import rtss.util.excel.ExcelRow;
import rtss.util.excel.ExcelSheet;
import rtss.util.excel.ExcelWorkbook;
import rtss.util.excel.FindCells;
import rtss.util.excel.RowCol;

public class LoadEH
{
    public static void main(String[] args)
    {
        try
        {
            CultureSet c = load();
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
        return new LoadEH().do_load();
    }

    private CultureSet do_load() throws Exception
    {
        final String fn9 = "agriculture/EH-2014/tables-ch-9.xlsx";
        final String fn17 = "agriculture/EH-2014/extracted-tables-ch-17.xlsx";
        
        try
        {
            try (XSSFWorkbook wb = Excel.loadWorkbook(fn9))
            {
                for (int tab = 9; tab <= 38; tab++)
                    loadTabCh9(wb, "9." + tab);
            }
            
            loadImports(ExcelWorkbook.load(fn17).getTheOnlySheet());

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

    private void loadTabCh9(XSSFWorkbook wb, String tab) throws Exception
    {
        for (int k = 0; k < wb.getNumberOfSheets(); k++)
        {
            XSSFSheet sheet = wb.getSheetAt(k);
            String sname = sheet.getSheetName();
            if (sname == null)
                sname = "";
            sname = Util.despace(sname).trim();
            if (sname.equals(tab))
            {
                ExcelRC rc = Excel.readSheet(wb, sheet, currentFile);
                loadTabCh9(tab, rc);
                return;
            }
        }

        throw new Exception("Cannot find table sheet " + tab);
    }

    private void loadTabCh9(String tab, ExcelRC rc) throws Exception
    {
        String cname = extractCultureName(rc);
        // Util.out(String.format("%s [%s]", cname, tab));
        RiceKind rice_kind = null;

        switch (cname.toLowerCase())
        {
        // non-edible
        case "henequén":
        case "algodón (pluma)":
            return;

        // duplicates SARH data with no additions
        case "cocotero (palmera de coco)":
            return;
        
        case "arroz palay":
            cname = "arroz"; 
            rice_kind = RiceKind.RAW;
            break;
            
        case "plátano":    
            cname = "plátanos otros"; 
            break;
        }

        Culture c = new Culture(cname, null);
        cultures.add(c);

        RowCol rcYear = FindCells.findRequiredVerticalCells(rc, "Año", "");

        RowCol rcSurface = FindCells.findVerticalCells(rc, "Superficie", "cose-", "chada", "ha");
        if (rcSurface == null)
            rcSurface = FindCells.findRequiredVerticalCells(rc, "Superficie", "cosechada", "ha");

        RowCol rcYield = FindCells.findVerticalCells(rc, "Rendi-", "miento", "medio", "kg/ha");
        if (rcYield == null)
            rcYield = FindCells.findVerticalCells(rc, "Rendimien-", "to medio", "kg/ha");
        if (rcYield == null)
            rcYield = FindCells.findVerticalCells(rc, "Rendimiento", "kg/ha");
        if (rcYield == null)
            rcYield = FindCells.findRequiredVerticalCells(rc, "Rendimiento", "medio", "kg/ha");

        RowCol rcProduction = FindCells.findVerticalCells(rc, "Producción", "t");
        if (rcProduction == null)
            rcProduction = FindCells.findVerticalCells(rc, "Producción", "Tone-", "ladas");
        if (rcProduction == null)
            rcProduction = FindCells.findRequiredVerticalCells(rc, "Produc-", "ción", "t");

        RowCol rcImport = FindCells.findVerticalCells(rc, "Importa-", "ción", "t");
        RowCol rcExport = FindCells.findVerticalCells(rc, "Exporta-", "ción", "t");

        RowCol rcConsumption = FindCells.findVerticalCells(rc, "Nacional", "t");
        if (rcConsumption == null)
            rcConsumption = FindCells.findVerticalCells(rc, "Consumo", "nacional", "aparente", "t");
        if (rcConsumption == null)
            rcConsumption = FindCells.findVerticalCells(rc, "Nacio-", "nal", "t");

        RowCol rcPerCapita = FindCells.findVerticalCells(rc, "Per cápita", "kg");
        if (rcPerCapita == null)
            rcPerCapita = FindCells.findVerticalCells(rc, "Per", "cápita", "kg");

        if (Util.False)
        {
            if (rcImport == null)
                Util.out("No import for " + cname);
            if (rcExport == null)
                Util.out("No export for " + cname);
            if (rcConsumption == null)
                Util.out("No consumption for " + cname);
            if (rcPerCapita == null)
                Util.out("No per capita for " + cname);
        }

        for (int nr = rcYear.row + 2; nr < rc.size() && !rc.isEndRow(nr); nr++)
        {
            String s = rc.asString(nr, rcYear.col);
            if (s == null)
                continue;
            s = Util.despace(s).trim();
            if (s.equals(""))
                continue;

            if (s.equals("Características de la producción de " + cname) ||
                s.equals("(Continúa)") ||
                s.equals("Año") ||
                s.startsWith("Características de la produc") ||
                s.startsWith("Nota: ") ||
                s.startsWith("SAGAR. Centro de Estadística Agropecuaria") ||
                s.startsWith("SARH, Dirección General de Economía Agrícola") ||
                s.startsWith("SARH. Dirección General de Economía Agrícola") ||
                s.startsWith("de Agricultura y Recursos Hidráulicos") ||
                s.startsWith("y Recursos Hidráulicos") ||
                s.startsWith("Secretaría de Agricultura y Recursos Hidráulicos") ||
                s.startsWith("de temporal y se cosechan") ||
                s.startsWith("Fuente: SAGAR. Centro de Estadística Agropecuaria") ||
                s.startsWith("www.siap.gob.mx") ||
                s.startsWith("Vol. ") ||
                s.startsWith("Para: ") ||
                s.startsWith("Para ") ||
                s.startsWith("1880 a") ||
                s.startsWith("Años seleccionados de") ||
                s.startsWith("de SAGAR") ||
                s.contains("SARH") ||
                s.startsWith("Serie anual de "))
            {
                // ignore
                continue;
            }
            else if (s.startsWith("18") || s.startsWith("19") || s.startsWith("20"))
            {
                // proceed
            }
            else
            {
                throw new Exception("Unexpected cell content: " + s);
            }

            if (s.contains("-"))
                return;

            if (s.endsWith(".0"))
                s = Util.stripTail(s, ".0");

            if (s.length() != 4)
                throw new Exception("Unexpected year cell content: " + s);

            int year = Integer.parseInt(s);
            if (year < 1881 || year > 2030)
                throw new Exception("Unexpected year cell content: " + s);
            
            // unreliable data (civil war)
            if (year >= 1909 && year <= 1924)
                return;

            CultureYear cy = c.makeCultureYear(year);
            cy.surface = getValue(rc, nr, rcSurface);
            cy.yield = getValue(rc, nr, rcYield);
            cy.production = getValue(rc, nr, rcProduction);
            cy.importAmount = getValue(rc, nr, rcImport);
            cy.exportAmount = getValue(rc, nr, rcExport);
            cy.consumption = getValue(rc, nr, rcConsumption);
            cy.perCapita = getValue(rc, nr, rcPerCapita);
            
            if (rice_kind == RiceKind.RAW)
            {
                rice_kind = RiceKind.WHITE; 
                cy.production_raw = cy.production;
                cy.perCapita = null;
                cy.production *= ArgiConstants.RawRiceToWhiteRice;
            }
            else if (rice_kind == RiceKind.WHITE || rice_kind == null)
            {
                cy.rice_kind = rice_kind;
            }
            
            if (cy.isAllNull())
                c.deleteYear(cy);
        }
    }

    private Double getValue(ExcelRC rc, int nr, RowCol rowcol) throws Exception
    {
        if (rowcol == null)
            return null;
        String s = rc.asString(nr, rowcol.col);
        if (s == null)
            return null;
        s = Util.despace(s).trim();
        if (s.equals("") || s.equals("ND"))
            return null;
        s = s.replace(" ", "");
        return Double.parseDouble(s);
    }

    private String extractCultureName(ExcelRC rc) throws Exception
    {
        final String prefix = "Características de la producción de ";
        String cname = null;

        for (int nr = 0; nr < rc.size() && !rc.isEndRow(nr); nr++)
        {
            int ncols = rc.get(nr).size();
            for (int nc = 0; nc < ncols; nc++)
            {
                String s = rc.asString(nr, nc);
                if (s == null)
                    continue;
                s = Util.despace(s).trim();
                if (!s.toLowerCase().startsWith(prefix.toLowerCase()))
                    continue;
                s = s.substring(prefix.length()).trim();
                if (cname != null && !cname.equalsIgnoreCase(s))
                    throw new Exception("Conflicting culture names");
                cname = s;
            }
        }

        if (cname == null)
            throw new Exception("Missing culture name");

        return cname;
    }
    
    private void loadImports(ExcelSheet sheet) throws Exception
    {
        for (ExcelRow row : sheet.getRows())
        {
            String s = row.asDespacedString("год");
            if (s == null || s.length() == 0)
                continue;
            
            int year = row.asInteger("год");
            Double importMaiz = row.asDouble("Maíz");
            Double importTrigo = row.asDouble("Trigo");
            
            CultureYear cy;
            cy = cultures.get("maiz").cultureYear(year);
            cy.importAmount = importMaiz;
            
            cy = cultures.get("trigo").cultureYear(year);
            cy.importAmount = importTrigo;
        }
    }
}
