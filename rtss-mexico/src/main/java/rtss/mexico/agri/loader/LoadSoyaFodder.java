package rtss.mexico.agri.loader;

import rtss.mexico.agri.entities.SoyaFodder;
import rtss.util.excel.ExcelRow;
import rtss.util.excel.ExcelSheet;
import rtss.util.excel.ExcelWorkbook;

public class LoadSoyaFodder
{
    private static SoyaFodder cachedSoyaFodder = null;

    public static SoyaFodder load() throws Exception
    {
        if (cachedSoyaFodder != null)
            return cachedSoyaFodder;

        SoyaFodder sf = new SoyaFodder();

        org.apache.poi.util.IOUtils.setByteArrayMaxOverride(300_000_000);
        ExcelWorkbook wb = ExcelWorkbook
                .load("agriculture/FAOSTAT/FoodBalanceSheetsHistoric_E_All_Data/MexOnly - FoodBalanceSheetsHistoric_E_All_Data_NOFLAG.xlsx");
        ExcelSheet sheet = wb.getTheOnlySheet();

        ExcelRow rSupply = findRow(sheet, "Soyabeans", "Domestic supply quantity");
        ExcelRow rFeed = findRow(sheet, "Soyabeans", "Feed");

        for (int year = 1961; year <= 2013; year++)
        {
            String index = "Y" + year;
            double supply = rSupply.asDouble(index);
            double feed = rFeed.asDouble(index);
            sf.put(year, 100.0 * feed / supply);
        }

        cachedSoyaFodder = sf;
        return sf;
    }

    private static ExcelRow findRow(ExcelSheet sheet, String item, String element) throws Exception
    {
        for (ExcelRow row : sheet.getRows())
        {
            if (!row.asDespacedString("Area").equals("Mexico"))
                continue;
            if (row.asDespacedString("Item").equals(item) && row.asDespacedString("Element").equals(element))
                return row;
        }

        throw new Exception("Cannot find requested row");
    }
}
