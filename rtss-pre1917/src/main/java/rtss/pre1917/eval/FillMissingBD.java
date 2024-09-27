package rtss.pre1917.eval;

import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.util.Util;

/*
 * Заполнить пробелы в сведениях о числе рождений и смертей.
 * Для набора данных УГВИ.
 */
public class FillMissingBD
{
    private final TerritoryDataSet tds;

    public FillMissingBD(TerritoryDataSet tds)
    {
        this.tds = tds;
    }

    public void fillMissingBD() throws Exception
    {
        // Нет сведений за 1913 и 1914 гг. по Карсской области и за 1913 г. для Семипалатинской обл.
        // Изменение их населения за 1913 г. приближено повтором величины за 1912 год.
        repeat("Карсская обл.", 1913, 1912);
        repeat("Семипалатинская обл.", 1913, 1912);

        // Для 1913 гг. сведения есть только по части привислинских губерний и по Варшаве.
        // Для губерний с отсутствующими сведениями повторить значения их роста за 1912 год.
        for (String tname : Taxon.of("привислинские губернии", 1913, tds).territories.keySet())
        {
            if (tds.get(tname) != null && !hasBD(tname, 1913))
                repeat(tname, 1913, 1912);
        }
        
        // Сведения о числе рождений и смертей отсутствуют для Кутаисской губернии (1904-1905).
        // Исчислить приближение усреднением сведений за 1903 и 1906 гг.
        if (Util.True)
        {
            if (hasBD("Кутаисская", 1904) || hasBD("Кутаисская", 1905))
                throw new Exception("Already have BD data");
            Territory t = tds.get("Кутаисская");
            TerritoryYear t1 = t.territoryYear(1903);
            TerritoryYear t2 = t.territoryYear(1904);
            TerritoryYear t3 = t.territoryYear(1905);
            TerritoryYear t4 = t.territoryYear(1906);
            t3.births.total.both = t2.births.total.both = (t1.births.total.both + t4.births.total.both) / 2; 
            t3.deaths.total.both = t2.deaths.total.both = (t1.deaths.total.both + t4.deaths.total.both) / 2; 
        }
        
        // Сведения о числе рождений и смертей отсутствуют для Сыр-Дарьинской области (1902-1905 и 1914). 

        // ###
    }

    private void repeat(String tname, int yto, int yfrom) throws Exception
    {
        Territory t = tds.get(tname);
        TerritoryYear t1 = t.territoryYear(yfrom);
        TerritoryYear t2 = t.territoryYear(yto);
        if (t2.births.total.both != null || t2.deaths.total.both != null)
            throw new Exception("Already have BD data");
        
        t2.births.total.both = t1.births.total.both;
        t2.deaths.total.both = t1.deaths.total.both;

        t2.births.urban.both = t1.births.urban.both;
        t2.deaths.urban.both = t1.deaths.urban.both;

        t2.births.rural.both = t1.births.rural.both;
        t2.deaths.rural.both = t1.deaths.rural.both;
    }

    private boolean hasBD(String tname, int year)
    {
        Territory t = tds.get(tname);
        TerritoryYear ty = t.territoryYear(year);
        return ty.births.total.both != null && ty.deaths.total.both != null;
    }
}
