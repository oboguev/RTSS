package rtss.mexico.population;

import java.util.List;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import rtss.mexico.util.ColumnHeader;
import rtss.mexico.util.RC;
import rtss.util.Util;
import rtss.util.excel.Excel;

/**
 * Рождаемость и смертность населения Мексики по CONAPO
 */
public class ConapoVitalRates
{
    public static void main(String[] args)
    {
        try
        {
            org.apache.poi.util.IOUtils.setByteArrayMaxOverride(300_000_000);
            Util.out("Рождаемость, смертность и суммарный коэффициент рождаемости населения Мексики:");
            Util.out("");
            new ConapoVitalRates().do_main("conapo/ConDem50a19_ProyPob20a70/5_Indicadores_demográficos_proyecciones.xlsx");

            Util.noop();
        }
        catch (Throwable ex)
        {
            Util.err("*** Exception");
            ex.printStackTrace();
        }
    }

    private void do_main(String fpath) throws Exception
    {
        try (XSSFWorkbook wb = Excel.loadWorkbook(fpath))
        {
            if (wb.getNumberOfSheets() != 1)
                throw new Exception("Unexpected multiple sheets in file");
            XSSFSheet sheet = wb.getSheetAt(0);
            List<List<Object>> rc = Excel.readSheet(wb, sheet, fpath);
            Map<String, Integer> headers = ColumnHeader.getTopHeaders(sheet, rc);
            do_process(rc, headers);
        }
    }

    private void do_process(List<List<Object>> rc, Map<String, Integer> headers) throws Exception
    {
        Util.out("год рождаемость смертность СКР");
        
        int ixYear = ColumnHeader.getRequiredHeader(headers, "AÑO");
        int ixEntityCode = ColumnHeader.getRequiredHeader(headers, "CVE_GEO");
        int ixCBR = ColumnHeader.getRequiredHeader(headers, "T_BRU_NAT");
        int ixCDR = ColumnHeader.getRequiredHeader(headers, "T_BRU_MOR");
        int ixTFR = ColumnHeader.getRequiredHeader(headers, "TGF");

        for (int nr = 1; nr < rc.size(); nr++)
        {
            int entityCode = asInt(rc, nr, ixEntityCode);
            if (entityCode == 0) 
            {
                int year = asInt(rc, nr, ixYear);
                double cbr = asDouble(rc, nr, ixCBR);
                double cdr = asDouble(rc, nr, ixCDR);
                double tfr = asDouble(rc, nr, ixTFR);
                Util.out(String.format("%d %.1f %.1f %.1f", year, cbr, cdr, tfr));
            }
        }
    }

    private String asString(List<List<Object>> rc, int nr, int nc) throws Exception
    {
        Object o = RC.get(rc, nr, nc);
        if (o == null)
            return null;
        String s = o.toString();
        s = Util.despace(s).trim();
        return s;
    }

    private int asInt(List<List<Object>> rc, int nr, int nc) throws Exception
    {
        Object o = RC.get(rc, nr, nc);

        if (o == null)
            throw new Exception("Missing integer data");

        if (o instanceof Integer)
            return ((Integer) o).intValue();

        if (o instanceof Long)
            return ((Long) o).intValue();

        String s = asString(rc, nr, nc);
        if (s.endsWith(".0"))
            s = Util.stripTail(s, ".0");
        return Integer.parseInt(s);
    }

    private double asDouble(List<List<Object>> rc, int nr, int nc) throws Exception
    {
        Object o = RC.get(rc, nr, nc);

        if (o == null)
            throw new Exception("Missing double data");

        if (o instanceof Double)
            return (Double) o;

        if (o instanceof Float)
            return (Float) o;

        String s = asString(rc, nr, nc);
        return Double.parseDouble(s);
    }
}
