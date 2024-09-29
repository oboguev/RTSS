package rtss.pre1917.tools;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.util.FieldValue;
import rtss.util.Util;

public class ShowAreaValues
{
    public static void main(String[] args)
    {
        try
        {
            Util.out("Прогрессивные величины исчисляются отсчётом от переписи 1897 года с прибавлением ежегодного");
            Util.out("числа рождений минус смертей (по УГВИ) и миграционного баланса области (по ЦСК).");
            Util.out("");
            Util.out("Недорегистрация рождений и смертей ведёт к исакажению прогрессивной численности населения,");
            Util.out("вероятнее всего в сторону его недоучёта.");
            Util.out("");
            Util.out("Миграционный баланс -- среднегодовой за 1896-1910 и 1911-1915.");
            Util.out("");
            new ShowAreaValues().show_values_central_asia();
            new ShowAreaValues().show_values_causases();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }
    
    private final TerritoryDataSet tdsUGVI = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES, LoadOptions.EVAL_PROGRESSIVE);
    private final TerritoryDataSet tdsCSK = new LoadData().loadEzhegodnikRossii(LoadOptions.DONT_VERIFY);

    private ShowAreaValues() throws Exception
    {
    }

    /* ============================================================================================== */

    @SuppressWarnings("unused")
    private void show_values_central_asia() throws Exception
    {
        show_values("Акмолинская обл.");
        show_values("Закаспийская обл.");
        show_values("Самаркандская обл.");
        show_values("Семипалатинская обл.");
        show_values("Семиреченская обл.");
        show_values("Сыр-Дарьинская обл.");
        show_values("Тургайская обл.");
        show_values("Уральская обл.");
        show_values("Ферганская обл.");
    }

    @SuppressWarnings("unused")
    private void show_values_causases() throws Exception
    {
        show_values("г. Баку");
        show_values("Бакинская");
        show_values("Батумская");
        show_values("Дагестанская обл.");
        show_values("Елисаветпольская");
        show_values("Карсская обл.");
        show_values("Кубанская обл.");
        show_values("Кутаисская");
        show_values("Ставропольская");
        show_values("Терская обл.");
        show_values("Тифлисская");
        show_values("Черноморская");
        show_values("Эриванская");
        show_values("Закатальский окр.");
        show_values("Сухумский окр.");
    }

    private void show_values(String tname)
    {
        Util.out("");
        Util.out("******************************************************************************");
        Util.out("");
        Util.out("Рождаемость и смертность для " + tname);
        Util.out("");
        Util.out("год       ЦСК             УГВИ          прогрессивные от 1897");
        Util.out("==== =========== =====================  =====================");
        
        Territory t = tdsUGVI.get(tname);
        if (t == null)
            return;

        Territory tCSK = tdsCSK.get(tname);

        for (int year : t.years())
        {
            if (year >= 1896 && year <= 1914)
            {
                TerritoryYear ty = t.territoryYear(year);
                Double cbrUGVI = rate(ty.births.total.both, ty.population.total.both);
                Double cdrUGVI = rate(ty.deaths.total.both, ty.population.total.both);

                Double cbrProgressive = rate(ty.births.total.both, ty.progressive_population.total.both);
                Double cdrProgressive = rate(ty.deaths.total.both, ty.progressive_population.total.both);
                
                TerritoryYear tyCSK = null;
                Long popCSK = null;
                if (tCSK != null)
                    tyCSK = tCSK.territoryYearOrNull(year);
                if (tyCSK != null)
                    popCSK = tyCSK.population.total.both;
                
                Util.out(String.format("%d %s %s %s %s %s %s %s", year, 
                                       s_pop(popCSK), 
                                       s_pop(ty.population.total.both), 
                                       s_rate(cbrUGVI), s_rate(cdrUGVI),
                                       s_pop(ty.progressive_population.total.both), 
                                       s_rate(cbrProgressive), s_rate(cdrProgressive)));
            }
        }
    }
    
    private String s_pop(Long v)
    {
        String s = "";
        if (v != null)
            s = String.format("%,d", v);
        while (s.length() < 11)
            s = " " + s;
        return s;
    }
    
    private String s_rate(Double d)
    {
        String s = "";
        if (d != null)
            s = String.format("%2.1f", d);
        while (s.length() < 4)
            s = " " + s;
        return s;
    }

    private Double rate(Long v, Long pop)
    {
        if (v == null || pop == null || pop == 0)
            return null;
        else
            return (v * 1000.0) / pop;
    }
}
