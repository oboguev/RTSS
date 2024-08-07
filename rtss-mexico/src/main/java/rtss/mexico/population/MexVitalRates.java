package rtss.mexico.population;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import rtss.mexico.util.ColumnHeader;
import rtss.util.Util;
import rtss.util.excel.Excel;
import rtss.util.excel.ExcelRC;

import rtss.un.wpp.WPP;
import rtss.un.wpp.WPP2024;


/**
 * Рождаемость и смертность населения Мексики по CONAPO и UN WPP
 */
public class MexVitalRates
{
    public static void main(String[] args)
    {
        try
        {
            org.apache.poi.util.IOUtils.setByteArrayMaxOverride(300_000_000);
            
            Util.out("Рождаемость, смертность и суммарный коэффициент рождаемости населения Мексики (по CONAPO):");
            Util.out("");
            new MexVitalRates().do_conapo("conapo/ConDem50a19_ProyPob20a70/5_Indicadores_demográficos_proyecciones.xlsx");

            Util.out("");
            Util.out("====================================");
            Util.out("");
            Util.out("Рождаемость, смертность, суммарный коэффициент рождаемости и нетто коэффициент воспроизводства населения Мексики (по UN WPP 2024):");
            Util.out("  - Crude Birth Rate (births per 1,000 population)");
            Util.out("  - Crude Death Rate (deaths per 1,000 population)");
            Util.out("  - Total Fertility Rate (live births per woman)");
            Util.out("  - Net Reproduction Rate (surviving daughters per woman) = чистый (нетто) коэффициент воспроизводства");
            Util.out("");
            new MexVitalRates().do_wpp();
            
            Util.noop();
        }
        catch (Throwable ex)
        {
            Util.err("*** Exception");
            ex.printStackTrace();
        }
    }

    private void do_conapo(String fpath) throws Exception
    {
        try (XSSFWorkbook wb = Excel.loadWorkbook(fpath))
        {
            if (wb.getNumberOfSheets() != 1)
                throw new Exception("Unexpected multiple sheets in file");
            XSSFSheet sheet = wb.getSheetAt(0);
            ExcelRC rc = Excel.readSheet(wb, sheet, fpath);
            Map<String, Integer> headers = ColumnHeader.getTopHeaders(sheet, rc);
            do_conapo_process(rc, headers);
        }
    }

    private void do_conapo_process(ExcelRC rc, Map<String, Integer> headers) throws Exception
    {
        Util.out("год рождаемость смертность СКР");
        
        int ixYear = ColumnHeader.getRequiredHeader(headers, "AÑO");
        int ixEntityCode = ColumnHeader.getRequiredHeader(headers, "CVE_GEO");
        int ixCBR = ColumnHeader.getRequiredHeader(headers, "T_BRU_NAT");
        int ixCDR = ColumnHeader.getRequiredHeader(headers, "T_BRU_MOR");
        int ixTFR = ColumnHeader.getRequiredHeader(headers, "TGF");

        for (int nr = 1; nr < rc.size(); nr++)
        {
            int entityCode = rc.asRequiredInt(nr, ixEntityCode);
            if (entityCode == 0) 
            {
                int year = rc.asRequiredInt(nr, ixYear);
                double cbr = rc.asRequiredDouble(nr, ixCBR);
                double cdr = rc.asRequiredDouble(nr, ixCDR);
                double tfr = rc.asRequiredDouble(nr, ixTFR);
                Util.out(String.format("%d %.1f %.1f %.1f", year, cbr, cdr, tfr));
            }
        }
    }

    /* ============================================================================================ */

    private void do_wpp() throws Exception
    {
        Util.out("год рождаемость смертность СКР ЧКВ");

        try (WPP wpp = new WPP2024())
        {
            Map<Integer, Map<String, Object>> mx = wpp.forCountry("Mexico");
            List<Integer> years = new ArrayList<>(mx.keySet());
            Collections.sort(years);
            for (int year : years)
            {
                Double cbr = null;
                Double cdr = null;
                Double tfr = null;
                Double nrr = null;
                
                Map<String, Object> m = mx.get(year);
                for (String key : m.keySet())
                {
                    if (key.toLowerCase().contains("Crude Birth Rate".toLowerCase()))
                    {
                        cbr = ExcelRC.asRequiredDouble(m.get(key));
                    }
                    else if (key.toLowerCase().contains("Crude Death Rate".toLowerCase()))
                    {
                        cdr = ExcelRC.asRequiredDouble(m.get(key));
                    }
                    else if (key.toLowerCase().contains("Total Fertility Rate".toLowerCase()))
                    {
                        tfr = ExcelRC.asRequiredDouble(m.get(key));
                    }
                    else if (key.toLowerCase().contains("Net Reproduction Rate".toLowerCase()))
                    {
                        nrr = ExcelRC.asRequiredDouble(m.get(key));
                    }
                }
                
                Util.out(String.format("%d %s %s %s %s", year, d2s(cbr), d2s(cdr), d2s(tfr), d2s(nrr)));
            }
        }
    }
    
    private String d2s(Double v)
    {
        if (v == null)
            return "-";
        else
            return String.format("%.1f", v);
    }
}
