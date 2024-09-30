package rtss.pre1917.tools;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.InnerMigration;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.eval.EvalGrowthRate;
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
            Util.out("Недорегистрация рождений и смертей ведёт к искажению прогрессивной численности населения,");
            Util.out("вероятнее всего (но не обязательно) в сторону его недоучёта и занижения.");
            Util.out("");
            Util.out("Миграционный баланс -- среднегодовой за 1896-1910 и 1911-1915.");
            Util.out("В миграции учитыватся только крестьянское переселение, но не движение рабочих,");
            Util.out("поэтому миграционная оценка для Бакинского района или Донбасса занижена,");
            Util.out("а в местах выхода рабочих соответственно не учтена.");
            Util.out("");
            Util.out("Прогрессивная оценка не вычисляется для");
            Util.out("    Камчатской области (создана в 1909), Батумской области (создана в 1903), Холмской губернии (создана в 1912)");
            Util.out("т.к. для промежутка между 1897 годом и моментом их создания сведения о естественом движении");
            Util.out("не включены в базу (хотя могут быть добавлены позднее, из уездных сведений УГВИ).");
            Util.out("");
            Util.out("Оценка по стабилизированному участку вычисляется на основе лет, в которые была достигнута");
            Util.out("удовлетворительная полнота регистрации рождений и смертей, в предположении, что в остальные годы");
            Util.out("рождаемость и смертность была такой же, как в среднем по стаб. участку.");
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
    private final TerritoryDataSet tdsCensus1897 = new LoadData().loadCensus1897(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
    private final InnerMigration innerMigration = new LoadData().loadInnerMigration();
    private final EvalGrowthRate evalGrowthRate = new  EvalGrowthRate(tdsCensus1897, innerMigration);
    
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
        show_values("Бакинская с Баку");
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

    private void show_values(String tname) throws Exception
    {
        Territory t = tdsUGVI.get(tname);
        if (t == null)
        {
            Util.out("************************************************************");
            Util.out("");
            Util.out("Нет сведений для " + tname);
            Util.out("");
            return;
        }

        Territory tCSK = tdsCSK.get(tname);
        Territory tEval = evalGrowthRate.evalTerritory(t);

        Util.out("");
        Util.out("************************************************************");
        Util.out("");
        Util.out("Рождаемость и смертность для " + tname);
        Util.out("");
        if (tEval == null)
        {
            Util.out("год       ЦСК             УГВИ          прогрессивные от 1897");
            Util.out("==== =========== =====================  =====================");
        }
        else
        {
            Util.out("год       ЦСК             УГВИ          прогрессивные от 1897  по стабилиз. участку");
            Util.out("==== =========== =====================  =====================  =====================");
        }
        
        for (int year : t.years())
        {
            if (year >= 1896 && year <= 1914)
            {
                TerritoryYear ty = t.territoryYear(year);
                Double cbrUGVI = rate(ty.births.total.both, ty.population.total.both);
                Double cdrUGVI = rate(ty.deaths.total.both, ty.population.total.both);

                Double cbrProgressive = rate(ty.births.total.both, ty.progressive_population.total.both);
                Double cdrProgressive = rate(ty.deaths.total.both, ty.progressive_population.total.both);
                
                TerritoryYear tyEval = null;
                Double cbrEval = null;
                Double cdrEval = null;
                Long popEval = null;

                if (tEval != null)
                {
                    tyEval = tEval.territoryYear(year);
                    popEval = tyEval.population.total.both;
                    cbrEval = rate(tyEval.births.total.both, popEval);
                    cdrEval = rate(tyEval.deaths.total.both, popEval);
                }
                
                TerritoryYear tyCSK = null;
                Long popCSK = null;
                if (tCSK != null)
                    tyCSK = tCSK.territoryYearOrNull(year);
                if (tyCSK != null)
                    popCSK = tyCSK.population.total.both;
                
                Util.out(String.format("%d %s %s %s %s %s %s %s %s %s %s", year, 
                                       s_pop(popCSK), 
                                       s_pop(ty.population.total.both), 
                                       s_rate(cbrUGVI), s_rate(cdrUGVI),
                                       s_pop(ty.progressive_population.total.both), 
                                       s_rate(cbrProgressive), s_rate(cdrProgressive),
                                       s_pop(popEval),
                                       s_rate(cbrEval), s_rate(cdrEval)));
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
