package rtss.pre1917.eval;

import java.util.Map;

import rtss.pre1917.LoadData;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.data.URValue;
import rtss.pre1917.data.ValueByGender;
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
        if (tds.filledMissingBD)
            return;
            
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
        
        // Черноморская губерния (с центром в Новороссийске) была образована в мае 1896 года выделением из Кубанской области, 
        // поэтому сведения о числе рождений и смертей в ней собираются с 1897 года. Установть для 1896 года в Черноморской губ.
        // число рождений и смертей как среднее за 1897-1900. 
        average("Черноморская", 1896, 1897, 1900);

        // исправить число рождений и смертей для губерний с иудеями
        fixJews();

        tds.filledMissingBD = true;
    }

    /* =============================================================================================== */

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

    private void average(String tname, int yto, int y1, int y2) throws Exception
    {
        Territory t = tds.get(tname);
        TerritoryYear ty2 = t.territoryYear(yto);
        if (ty2.births.total.both != null || ty2.deaths.total.both != null)
            throw new Exception("Already have BD data");
        
        ty2.births.total.both = null;
        ty2.deaths.total.both = null;

        ty2.births.urban.both = null;
        ty2.deaths.urban.both = null;

        ty2.births.rural.both = null;
        ty2.deaths.rural.both = null;

        for (int y = y1; y <= y2; y++)
        {
            TerritoryYear ty1 = t.territoryYear(y);
            
            ty2.births.total.both = sum(ty2.births.total.both, ty1.births.total.both);
            ty2.deaths.total.both = sum(ty2.deaths.total.both, ty1.deaths.total.both);
            ty2.births.urban.both = sum(ty2.births.urban.both, ty1.births.urban.both);
            ty2.deaths.urban.both = sum(ty2.deaths.urban.both, ty1.deaths.urban.both);
            ty2.births.rural.both = sum(ty2.births.rural.both, ty1.births.rural.both);
            ty2.deaths.rural.both = sum(ty2.deaths.rural.both, ty1.deaths.rural.both);
        }
        
        int nyears = y2 - y1 + 1;
        
        ty2.births.total.both = div(ty2.births.total.both, nyears);
        ty2.deaths.total.both = div(ty2.deaths.total.both, nyears);

        ty2.births.urban.both = div(ty2.births.urban.both, nyears);
        ty2.deaths.urban.both = div(ty2.deaths.urban.both, nyears);
        
        ty2.births.rural.both = div(ty2.births.rural.both, nyears);
        ty2.deaths.rural.both = div(ty2.deaths.rural.both, nyears);
    }
    
    private Long sum(Long v1, Long v2)
    {
        if (v1 == null && v2 == null)
            return null;
        else if (v1 == null)
            return v2;
        else if (v2 == null)
            return v1;
        else
            return v1 + v2;
    }
    
    private Long div(Long v, int ny)
    {
        if (v == null)
            return null;
        else
            return Math.round((double) v / ny);
    }

    /* =============================================================================================== */

    private void fixJews() throws Exception
    {
        Map<String, Double> mj = new LoadData().loadJews();

        for (String tname : mj.keySet())
        {
            double pct = mj.get(tname);
            if (pct >= 0.1)
                fixJews(tname, pct);
        }
    }

    private void fixJews(String tname, double pct) throws Exception
    {
        Territory t = tds.get(tname);
        if (t == null)
        {
            // Util.err("Нет территории " + tname);
            return;
        }

        for (int year : t.years())
        {
            TerritoryYear ty = t.territoryYearOrNull(year);
            if (ty != null)
            {
                fixJews(ty.births, pct);
                fixJews(ty.deaths, pct);
            }
        }
    }

    private void fixJews(URValue ur, double pct) throws Exception
    {
        if (ur == null)
            return;
        
        if (ur.urban.both != null || ur.rural.both != null)
        {
            fixJews(ur.urban, pct);
            fixJews(ur.rural, pct);
            ur.total.recalcAsSum(ur.rural, ur.urban);
        }
        else
        {
            fixJews(ur.total, pct);
        }
    }

    private void fixJews(ValueByGender v, double pct) throws Exception
    {
        if (v.male != null && v.female != null)
        {
            v.male = scaleJews(v.male, pct);
            v.female = scaleJews(v.female, pct);
            v.both = v.male + v.female;
        }
        else if (v.both != null)
        {
            v.both = scaleJews(v.both, pct);
        }
    }
    
    private final double JudaicBirthDeathUnderaccouningRate = 0.15;

    private Long scaleJews(Long v, double pct) throws Exception
    {
        if (v != null)
            return v + Math.round(v * JudaicBirthDeathUnderaccouningRate * pct / 100.0);
        else
            return null;
    }
}
