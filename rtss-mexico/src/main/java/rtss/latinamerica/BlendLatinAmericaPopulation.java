package rtss.latinamerica;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtss.un.wpp.WPP;
import rtss.un.wpp.WPP2024;
import rtss.un.wpp.latinamerica.CountryNames;
import rtss.util.Util;
import rtss.util.excel.Excel;
import rtss.util.excel.ExcelRC;

/*
 * To load the WPP file, use Java heap size 16G:
 *     java -Xmx16g
 */
public class BlendLatinAmericaPopulation
{
    public static void main(String[] args)
    {
        try
        {
            new BlendLatinAmericaPopulation().do_main();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private List<String> Countries = new ArrayList<>();
    private Map<String, Map<Integer, Double>> country2sala = new HashMap<>();
    private Map<String, Map<Integer, Double>> country2wpp = new HashMap<>();
    private Map<String, Map<Integer, Double>> country2blend = new HashMap<>();

    private void defineCountries() throws Exception
    {
        defineCountry("Аргентина");
        defineCountry("Боливия");
        defineCountry("Бразилия");
        defineCountry("Чили");
        defineCountry("Колумбия");
        defineCountry("Коста-Рика");
        defineCountry("Куба");
        defineCountry("Доминиканская республика");
        defineCountry("Эквадор");
        defineCountry("Эль-Сальвадор");
        defineCountry("Гватемала");
        defineCountry("Гаити");
        defineCountry("Гондурас");
        defineCountry("Мексика");
        defineCountry("Никарагуа");
        defineCountry("Панама");
        defineCountry("Парагвай");
        defineCountry("Перу");
        defineCountry("Уругвай");
        defineCountry("Венецуэлла");
    }

    private void defineCountry(String cname) throws Exception
    {
        Countries.add(cname);
        if (CountryNames.r2e(cname) == null)
            throw new Exception("Нет отображения для страны " + cname);
    }

    private void do_main() throws Exception
    {
        defineCountries();

        ExcelRC rc = Excel.readSheet("latinamerica/Latin-America-Population.xlsx", true, "SALA 1974");
        WPP wpp = new WPP2024();

        for (String cname : Countries)
        {
            country2sala.put(cname, loadSALA(rc, cname));
            country2wpp.put(cname, loadWPP(wpp, cname));
        }

        for (String cname : Countries)
        {
            country2blend.put(cname, blend(country2sala.get(cname), country2wpp.get(cname)));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("год");
        for (String cname : Countries)
            sb.append(String.format(",\"%s\"", cname));
        Util.out(sb.toString());

        for (int year = 1900; year <= 2023; year++)
        {
            sb = new StringBuilder();
            sb.append("" + year);

            for (String cname : Countries)
                sb.append(String.format(",%.1f", country2blend.get(cname).get(year)));
            
            Util.out(sb.toString());
        }
    }

    private Map<Integer, Double> blend(Map<Integer, Double> sala, Map<Integer, Double> wpp)
    {
        Map<Integer, Double> blend = new HashMap<>();

        for (int year = 1900; year <= 1949; year++)
            blend.put(year, sala.get(year));

        for (int year = 1961; year <= 2023; year++)
            blend.put(year, wpp.get(year));

        double sala1950 = sala.get(1950);
        double wpp1950 = wpp.get(1950);

        /*
         * Начальный поправочный коэффициент:
         *
         *     R = SALA(1950) / WPP(1950)
         *
         * В 1950 году он применяется полностью.
         * К 1960 году он плавно исчезает.
         */
        double r = sala1950 / wpp1950;

        for (int year = 1950; year <= 1960; year++)
        {
            double u = (year - 1950) / 10.0;

            /*
             * Эрмитов smoothstep:
             *
             *     h(0) = 0
             *     h(1) = 1
             *     h'(0) = h'(1) = 0
             */
            double h = 3 * u * u - 2 * u * u * u;

            /*
             * Мультипликативная склейка:
             *
             *     1950: wpp(1950) * r^(1 - 0) = sala(1950)
             *     1960: wpp(1960) * r^(1 - 1) = wpp(1960)
             */
            double v = wpp.get(year) * Math.pow(r, 1 - h);

            blend.put(year, v);
        }

        return blend;
    }

    /* ================================================================================ */

    private Map<Integer, Double> loadSALA(ExcelRC rc, String cname) throws Exception
    {
        Map<Integer, Double> m = new HashMap<>();

        int col = findCountryColumn(rc, cname);

        for (int nr = 1; nr < rc.size() && !rc.isEndRow(nr); nr++)
        {
            String ys = rc.asString(nr, 0);
            if (ys != null)
            {
                int year = rc.asRequiredInt(nr, 0);
                double pop = rc.asRequiredDouble(nr, col);
                m.put(year, pop);
            }
        }

        for (int year = 1900; year <= 1972; year++)
        {
            if (m.get(year) == null)
                throw new Exception("Missing value for " + cname + " " + year);
        }

        return m;
    }

    private int findCountryColumn(ExcelRC rc, String cname) throws Exception
    {
        int ncols = rc.get(0).size();
        verify_eq("год", rc.asString(0, 0));

        for (int col = 1; col < ncols; col++)
        {
            if (cname.equals(rc.asString(0, col)))
                return col;
        }

        throw new Exception("Cannot locate country " + cname);
    }

    private void verify_eq(String s1, String s2) throws Exception
    {
        if (!s1.equals(s2))
            throw new Exception("unexpected cell value");
    }

    /* ================================================================================ */

    private Map<Integer, Double> loadWPP(WPP wpp, String cname) throws Exception
    {
        Map<Integer, Double> m = new HashMap<>();
        String ename = CountryNames.r2e(cname);
        Map<Integer, Map<String, Object>> ydatas = wpp.forCountry(ename);
        List<Integer> years = new ArrayList<>(ydatas.keySet());
        Collections.sort(years);

        for (int year : years)
        {
            Double pm = null;

            Map<String, Object> ydata = ydatas.get(year);
            for (String key : ydata.keySet())
            {
                if (key.toLowerCase().contains("Total Population, as of 1 July".toLowerCase()))
                {
                    pm = ExcelRC.asDouble(ydata.get(key));
                    m.put(year, pm);
                }
            }
        }

        for (int year = 1950; year <= 2023; year++)
        {
            if (m.get(year) == null)
                throw new Exception("Missing value for " + cname + " " + year);
        }

        return m;
    }
}
