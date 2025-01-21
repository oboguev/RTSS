package rtss.forward_1926_193x;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.PopulationByLocality;
import rtss.data.population.SmoothPopulation;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

public class Main
{
    public static void main(String[] args)
    {
        try
        {
            Main m = new Main();
            // m.testPopulationSmoother();
            m.do_main();
        }
        catch (Throwable ex)
        {
            Util.err("Exception: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            System.exit(1);
        }

        Util.out("");
        Util.out("*** Completed.");
    }

    private void do_main() throws Exception
    {
        Util.out("Передвижка населения СССР 1926 => 1937");
        Util.out("Вариант 1-ГКС: с таблицей смертности 1926-27 гг.");
        Util.out("Младенческая смертность по Госкомстату");
        Util.out("");
        Util.out("Итоги переписи населения 1937 года скорректированы для устранения фальсификации уровня урбанизации");
        Util.out("");
        new ForwardPopulation_1926_1937().forward(false, false);
        
        Util.out("");
        Util.out("========================================================================================================");
        Util.out("");
        Util.out("Передвижка населения СССР 1926 => 1937");
        Util.out("Вариант 1-АДХ: с таблицей смертности 1926-27 гг.");
        Util.out("Младенческая смертность по АДХ");
        Util.out("");
        Util.out("Итоги переписи населения 1937 года скорректированы для устранения фальсификации уровня урбанизации");
        Util.out("");
        new ForwardPopulation_1926_1937().forward(false, true);
        
        Util.out("");
        Util.out("========================================================================================================");
        Util.out("");
        Util.out("Передвижка населения СССР 1926 => 1937");
        Util.out("Вариант 2-ГКС: погодовая интерполяция таблицы смертности между таблицами 1926-27 и 1938-39 гг.");
        Util.out("Младенческая смертность по Госкомстату");
        Util.out("");
        Util.out("Итоги переписи населения 1937 года скорректированы для устранения фальсификации уровня урбанизации");
        Util.out("");
        new ForwardPopulation_1926_1937().forward(true, false);
        
        Util.out("");
        Util.out("========================================================================================================");
        Util.out("");
        Util.out("Передвижка населения СССР 1926 => 1937");
        Util.out("Вариант 2-АДХ: погодовая интерполяция таблицы смертности между таблицами 1926-27 и 1938-39 гг.");
        Util.out("Младенческая смертность по АДХ");
        Util.out("");
        Util.out("Итоги переписи населения 1937 года скорректированы для устранения фальсификации уровня урбанизации");
        Util.out("");
        new ForwardPopulation_1926_1937().forward(true, true);
        
        Util.out("");
        Util.out("========================================================================================================");
        Util.out("");
        Util.out("Передвижка населения СССР 1926 => 1939");
        Util.out("Погодовая интерполяция таблицы смертности между таблицами 1926-27 и 1938-39 гг.");
        Util.out("Младенческая смертность по Госкомстату");
        Util.out("");
        Util.out("Итоги переписи населения 1939 года скорректированы для устранения фальсифицирующих приписок и фальсификации уровня урбанизации");
        Util.out("");
        new ForwardPopulation_1926_1939().forward(false);

        Util.out("");
        Util.out("========================================================================================================");
        Util.out("");
        Util.out("Передвижка населения СССР 1926 => 1939");
        Util.out("Погодовая интерполяция таблицы смертности между таблицами 1926-27 и 1938-39 гг.");
        Util.out("Младенческая смертность по АДХ");
        Util.out("");
        Util.out("Итоги переписи населения 1939 года скорректированы для устранения фальсифицирующих приписок и фальсификации уровня урбанизации");
        Util.out("");
        new ForwardPopulation_1926_1939().forward(true);

        Util.out("");
        Util.out("========================================================================================================");
        Util.out("");
        Util.out("Передвижка населения СССР 1926 => 1937");
        Util.out("Погодовая интерполяция таблицы смертности между таблицами 1926-27 и 1938-39 гг.");
        Util.out("Рождаемость и смертность в 1926 году скорректированы по АДХ на недоучёт текущей регистрации");
        Util.out("Младенческая смертность по АДХ");
        Util.out("");
        Util.out("Итоги переписи населения 1937 года скорректированы для устранения фальсификации уровня урбанизации");
        Util.out("");
        new ForwardPopulation_1926_1937().useADH1926Rates().forward(true, true);

        Util.out("");
        Util.out("========================================================================================================");
        Util.out("");
        Util.out("Передвижка населения СССР 1926 => 1937");
        Util.out("Рождаемость и смертность в 1926 году скорректированы по АДХ на недоучёт текущей регистрации");
        Util.out("Коэффициенты таблицы смертности 1926-27 гг. увеличены чтобы дать в 1926 году смертность по АДХ,");
        Util.out("Младенческая смертность также по АДХ");
        Util.out("Полученная таблица применяется для всех лет 1926-1937");
        Util.out("");
        Util.out("Итоги переписи населения 1937 года скорректированы для устранения фальсификации уровня урбанизации");
        Util.out("");
        new ForwardPopulation_1926_1937().useADH1926Rates().forward(false, true);

        Util.out("");
        Util.out("========================================================================================================");
        Util.out("");
        Util.out("Передвижка населения СССР 1926 => 1939");
        Util.out("Погодовая интерполяция таблицы смертности между таблицами 1926-27 и 1938-39 гг.");
        Util.out("Рождаемость и смертность в 1926 году скорректированы по АДХ на недоучёт текущей регистрации");
        Util.out("Младенческая смертность по АДХ");
        Util.out("");
        Util.out("Итоги переписи населения 1939 года скорректированы для устранения фальсифицирующих приписок и фальсификации уровня урбанизации");
        Util.out("");
        new ForwardPopulation_1926_1939().useADH1926Rates().forward(true);
    }

    @SuppressWarnings("unused")
    private void load_all_data() throws Exception
    {
        new CombinedMortalityTable("mortality_tables/USSR/1926-1927");
        new CombinedMortalityTable("mortality_tables/USSR/1938-1939");

        PopulationByLocality.census(Area.USSR, 1926);
        PopulationByLocality.census(Area.RSFSR, 1926);
        PopulationByLocality.census(Area.USSR, 1937);
    }
    
    @SuppressWarnings("unused")
    private void testPopulationSmoother() throws Exception
    {
        PopulationByLocality p = PopulationByLocality.census(Area.USSR, 1926);
        double[] d0 = p.toArray(Locality.RURAL, Gender.MALE);
        double[] d1 = SmoothPopulation.smooth(d0, "A");
        double[] d2 = SmoothPopulation.smooth(d0, "AB");
        double[] d3 = SmoothPopulation.smooth(d0, "ABC");
        for (int k = 0; k < d0.length; k++)
        {
            Util.out(String.format("%d,%f,%f,%f,%f", k, d0[k], d1[k], d2[k], d3[k]));
        }
        
        Util.out("****************************");
    }
}
