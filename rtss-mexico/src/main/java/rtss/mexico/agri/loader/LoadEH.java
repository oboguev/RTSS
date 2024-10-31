package rtss.mexico.agri.loader;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import rtss.mexico.agri.entities.Cultures;
import rtss.util.Util;
import rtss.util.excel.Excel;
import rtss.util.excel.ExcelRC;
import rtss.util.excel.FindCells;
import rtss.util.excel.RowCol;

public class LoadEH
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
        return new LoadEH().do_load();
    }

    private Cultures do_load() throws Exception
    {
        final String fn9 = "agriculture/EH-2014/tables-ch-9.xlsx";

        try (XSSFWorkbook wb = Excel.loadWorkbook(fn9))
        {
            for (int tab = 9; tab <= 38; tab++)
                loadTab(wb, "9." + tab);

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

    private void loadTab(XSSFWorkbook wb, String tab) throws Exception
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
                // Util.out(tab);
                loadTab(rc);
                return;
            }
        }

        throw new Exception("Cannot find table sheet " + tab);
    }

    private void loadTab(ExcelRC rc) throws Exception
    {
        String cname = extractCultureName(rc);
        Util.out(cname);

        
        if (cname.equalsIgnoreCase("henequén") || cname.equalsIgnoreCase("cocotero (palmera de coco)"))
            return;

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
}
