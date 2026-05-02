package rtss.pre1917.tools.show;

import rtss.pre1917.ExportData;
import rtss.util.Util;

public class ShowAllAreasValuesRaw extends ShowAllAreasValues 
{
    public static void main(String[] args)
    {
        Util.out("Сырые данные изданий ЦСК и УГВИ без коррекции (кроме опечаток выявленных по перекрёстным суммам), ");
        Util.out("без слияния крупных городов с губерниями, в которых они находились, ");
        Util.out("и без слияния губерний и областей образованных после 1897 года с губерниями, из которых они были выделены. ");
        Util.out("");
        Util.out("Структура строки:");
        Util.out("");
        Util.out("- год");
        Util.out("");
        Util.out("- ЦСК: численность населения на начало года по ЦСК");
        Util.out("");
        Util.out("- УГВИ: ");
        Util.out("-- численность населения на начало года по УГВИ");
        Util.out("-- рождаемость (вычислена из числа рождений и численности населения по УГВИ)");
        Util.out("-- смертность (вычислена  из числа смертей и численности населения по УГВИ)");
        Util.out("-- естественный прирост (вычислен из рождаемости и смертности)");
        Util.out("-- число рождений (по УГВИ)");
        Util.out("-- число смертей (по УГВИ)");
        Util.out("");
        Util.out("- сальдо миграции (из данных о миграции, не по ЦСК или УГВИ)");
        Util.out("");
        Util.out("- метка (звёздочка), если год входит в стабилизированный участок");
        Util.out("");
        Util.out("Рождаемость и смертность нормированы на население начала года.");
        Util.out("");
        Util.out("Выборгская губерния не содержится в изданиях ЦСК и УГВИ.");
        Util.out("Для Выборгской губернии данные получены по финским сведениям о населении, числе рождений, смертей и миграции.");

        try
        {
            ExportData exportData = ExportData.forRaw();
            // new ShowAllAreasValues().show_values_all();
            // new ShowAllAreasValues(LoadOptions.MERGE_POST1897_REGIONS).show_values_all();
            rawShowAllAreasValues(exportData).show_values_all();
            exportData.export("p:\\@\\CSK-UGVI-raw.csv");
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }
    
    private ShowAllAreasValuesRaw() throws Exception
    {
    }
}
