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
            cd.category = row.asDespacedString("категория");
            cd.name = row.asDespacedString("ES");
            cd.name_ru = row.asDespacedString("RU");
            cd.name_en = row.asDespacedString("EN");
            String aliases = row.asDespacedString("синонимы");
            if (aliases == null)
                aliases = "";
            for (String s : aliases.split(","))
            {
                s = Util.despace(s).trim();
                if (s.length() != 0)
                    cd.aliases.add(s);
            }
            
            String kcal = row.asString("ккал/кг");
            if (kcal == null)
                kcal = "";
            kcal = Util.despace(kcal).trim();
            switch (kcal)
            {
            case "":
            case "-":
            case "непищевой":
                break;

            default:
                cd.kcal_kg = Double.parseDouble(kcal);
                break;
            }
            
            cds.add(cd);
        }
        
        cachedCultureDefinitions = cds;
        return cds;
    }
}
