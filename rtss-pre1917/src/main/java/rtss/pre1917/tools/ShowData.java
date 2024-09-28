package rtss.pre1917.tools;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.util.FieldValue;
import rtss.util.Util;

public class ShowData
{
    private TerritoryDataSet tds;

    public static void main(String[] args)
    {
        try
        {
            // new ShowData().show_1();
            // new ShowData().show_rates_central_asia();
            new ShowData().show_rates_causases();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    /* ============================================================================================== */

    private void show_1() throws Exception
    {
        // tds = new LoadData().loadEvroChast(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
        // show(tds, "50 губерний Европейской России", "population.total.both", 1897, 1914);
        // show(tds, "Архангельская", "births.total.both", 1897, 1914);
        // show(tds, "Ярославская", "deaths.total.both", 1897, 1914);

        // tds = new LoadData().loadEzhegodnikRossii(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
        // show(tds, "50 губерний Европейской России", "population.total.both", 1904, 1914);

        tds = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
        show(tds, "Астраханская", "population.total.both", 1897, 1915);
        show(tds, "Владимирская", "population.total.both", 1897, 1915);
        show(tds, "Вологодская", "population.total.both", 1897, 1915);
        show(tds, "Вятская", "population.total.both", 1897, 1915);
        show(tds, "Курская", "population.total.both", 1897, 1915);
        show(tds, "Олонецкая", "population.total.both", 1897, 1915);
        show(tds, "Пермская", "population.total.both", 1897, 1915);
        show(tds, "Саратовская", "population.total.both", 1897, 1915);
        show(tds, "Смоленская", "population.total.both", 1897, 1915);
        show(tds, "Ставропольская", "population.total.both", 1897, 1915);
        show(tds, "Тобольская", "population.total.both", 1897, 1915);
        show(tds, "Ярославская", "population.total.both", 1897, 1915);
    }

    private void show(TerritoryDataSet tds, String tname, String selector, int y1, int y2) throws Exception
    {
        Util.out("");
        Util.out(String.format("%s for %s %d-%d", selector, tname, y1, y2));
        Util.out("");

        for (int year = y1; year <= y2; year++)
        {
            Long v = null;

            Territory t = tds.get(tname);
            if (t != null)
            {
                TerritoryYear ty = t.territoryYear(year);
                if (ty != null)
                    v = FieldValue.getLong(ty, selector);
            }

            // Util.out(String.format("%d %,d", year, v));
            Util.out(String.format("%,d", v));
        }
    }

    /* ============================================================================================== */

    @SuppressWarnings("unused")
    private void show_rates_central_asia() throws Exception
    {
        tds = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY);

        show_rates("Акмолинская обл.");
        show_rates("Закаспийская обл.");
        show_rates("Самаркандская обл.");
        show_rates("Семипалатинская обл.");
        show_rates("Семиреченская обл.");
        show_rates("Сыр-Дарьинская обл.");
        show_rates("Тургайская обл.");
        show_rates("Уральская обл.");
        show_rates("Ферганская обл.");
    }

    @SuppressWarnings("unused")
    private void show_rates_causases() throws Exception
    {
        tds = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY);

        show_rates("г. Баку");
        show_rates("Бакинская");
        show_rates("Батумская");
        show_rates("Дагестанская обл.");
        show_rates("Елисаветпольская");
        show_rates("Карсская обл.");
        show_rates("Кубанская обл.");
        show_rates("Кутаисская");
        show_rates("Ставропольская");
        show_rates("Терская обл.");
        show_rates("Тифлисская");
        show_rates("Черноморская");
        show_rates("Эриванская");
        show_rates("Закатальский окр.");
        show_rates("Сухумский окр.");
        ;
    }

    private void show_rates(String tname)
    {
        Util.out("============================================");
        Util.out("");
        Util.out("Рождаемость и смертность для " + tname);
        Util.out("");

        Territory t = tds.get(tname);
        if (t == null)
            return;

        for (int year : t.years())
        {
            if (year >= 1896 && year <= 1914)
            {
                TerritoryYear ty = t.territoryYear(year);
                Double cbr = rate(ty.births.total.both, ty.population.total.both);
                Double cdr = rate(ty.deaths.total.both, ty.population.total.both);
                
                Util.out(String.format("%d %s %s", year, s_rate(cbr), s_rate(cdr)));
            }
        }
    }
    
    private String s_rate(Double d)
    {
        if (d == null)
            return "----";
        else
            return String.format("%2.1f", d);
    }

    private Double rate(Long v, Long pop)
    {
        if (v == null || pop == null || pop == 0)
            return null;
        else
            return (v * 1000.0) / pop;
    }
}
