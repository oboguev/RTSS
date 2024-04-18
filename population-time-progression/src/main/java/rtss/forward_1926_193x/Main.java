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
        Util.out("Продвижка населения СССР 1926 => 1937");
        Util.out("Вариант 1: с таблицей смертности 1926-27 гг.");
        Util.out("");
        new ForwardPopulation_1926_1937().forward(false);
        Util.out("");
        Util.out("========================================================================================================");
        Util.out("");
        Util.out("Продвижка населения СССР 1926 => 1937");
        Util.out("Вариант 2: погодовая интерполяция таблицы смертности между таблицами 1926-27 и 1938-39 гг.");
        Util.out("");
        new ForwardPopulation_1926_1937().forward(true);
        Util.out("");
        Util.out("========================================================================================================");
        Util.out("");
        Util.out("Продвижка населения СССР 1926 => 1939");
        Util.out("");
        Util.out("Итоги переписи населения 1939 года скорректированы для устранения фальсифицирующих приписок");
        Util.out("");
        new ForwardPopulation_1926_1939().forward();
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
