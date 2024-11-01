package rtss.mexico.agri.loader;

import rtss.mexico.agri.entities.CultureDefinition;
import rtss.util.Util;
import rtss.util.excel.ExcelRow;
import rtss.util.excel.ExcelSheet;
import rtss.util.excel.ExcelWorkbook;

public class LoadCultureDefinitions
{
    public static void main(String[] args)
    {
        try
        {
            CultureDefinitions cds = load();
            Util.unused(cds);
            Util.out("** Done");
        }
        catch (Exception ex)
        {
            Util.err("** Exception:");
            ex.printStackTrace();
        }
    }
    
    private static CultureDefinitions cachedCultureDefinitions = null;
    
    public static CultureDefinitions load() throws Exception
    {
        if (cachedCultureDefinitions != null)
            return cachedCultureDefinitions;
        
        CultureDefinitions cds = new CultureDefinitions();

        ExcelWorkbook wb = ExcelWorkbook.load("agriculture/cultures.xlsx");
        ExcelSheet sheet = wb.getTheOnlySheet();
        for (ExcelRow row : sheet.getRows())
        {
            CultureDefinition cd = new CultureDefinition();
            cd.category = row.asString("категория");
            cd.name = row.asString("ES");
            cd.name_ru = row.asString("RU");
            cd.name_en = row.asString("EN");
            String aliases = row.asString("синонимы");
            if (aliases == null)
                aliases = "";
            for (String s : aliases.split(","))
            {
                s = Util.despace(s).trim();
                if (s.length() != 0)
                    cd.aliases.add(s);
            }
            
            cds.add(cd);
        }
        
        cachedCultureDefinitions = cds;
        return cds;
    }
}
