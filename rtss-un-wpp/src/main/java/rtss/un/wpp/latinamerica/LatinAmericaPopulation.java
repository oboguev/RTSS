package rtss.un.wpp.latinamerica;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtss.un.wpp.WPP;
import rtss.un.wpp.WPP2024;
import rtss.util.Util;
import rtss.util.excel.ExcelRC;

/*
 * To load the WPP file, use Java heap size 16G:
 *     java -Xmx16g
 */
public class LatinAmericaPopulation
{
    public static void main(String[] args)
    {
        try
        {
            org.apache.poi.util.IOUtils.setByteArrayMaxOverride(300_000_000);
            new LatinAmericaPopulation().do_wpp();

        }
        catch (Throwable ex)
        {
            Util.err("*** Exception");
            ex.printStackTrace();
        }
    }
    
    private final List<String> Countries = new ArrayList<>();
    
    private void defineCountries() throws Exception
    {
        // defineCountries_1();
        defineCountries_2();
    }
    
    @SuppressWarnings("unused")
    private void defineCountries_1() throws Exception
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

    @SuppressWarnings("unused")
    private void defineCountries_2() throws Exception
    {
        defineCountry("Мексика");
        defineCountry("Гватемала");
        defineCountry("Гондурас");
        defineCountry("Сальвадор");
        defineCountry("Никарагуа");
        defineCountry("Коста-Рика");
        defineCountry("Панама");
        defineCountry("Куба");
        defineCountry("Гаити");
        defineCountry("Доминиканская республика");
        defineCountry("Пуэрто-Рико");
        defineCountry("Венесуэла");
        defineCountry("Колумбия");
        defineCountry("Эквадор");
        defineCountry("Перу");
        defineCountry("Боливия");
        defineCountry("Чили");
        defineCountry("Бразилия");
        defineCountry("Парагвай");
        defineCountry("Уругвай");
        defineCountry("Аргентина");
    }
    
    private void defineCountry(String rname) throws Exception
    {
        Countries.add(rname);
        if (CountryNames.r2e(rname) == null)
            throw new Exception("Unmapped country name: " + rname);
    }

    private Map<String, Map<Integer, Map<String, Object>>> cdatas = new HashMap<>();
    private WPP wpp;

    private void do_wpp() throws Exception
    {
        defineCountries();

        wpp = new WPP2024();
        
        for (String rname : Countries)
        {
            String ename = CountryNames.r2e(rname);
            Map<Integer, Map<String, Object>> cdata = wpp.forCountry(ename);
            if (cdata == null)
                throw new Exception("No data for " + ename);
            cdatas.put(rname, cdata);
        }
        
        if (Util.True)
        {
            Util.out("");
            Util.out("=======================================================");
            Util.out("Население стран Латинской Америки на начало года (UN WPP):");
            Util.out("");
            do_wpp("Total Population, as of 1 January");
        }

        if (Util.True)
        {
            Util.out("");
            Util.out("=======================================================");
            Util.out("Население стран Латинской Америки на середину года (UN WPP):");
            Util.out("");
            do_wpp("Total Population, as of 1 July");
        }
    }
    
    private void do_wpp(String datekey) throws Exception
    {
        StringBuilder sb = new StringBuilder();
        sb.append("год");
        for (String rname : Countries)
        {
            if (sb.length() != 0)
                sb.append(",");
            sb.append('"' + rname + '"');
        }
        Util.out(sb.toString());

        for (int year = 1950; year <= 2023; year++)
        {
            sb = new StringBuilder();
            sb.append("" + year);
            for (String rname : Countries)
            {
                Map<Integer, Map<String, Object>> cdata = cdatas.get(rname);
                Map<String, Object> ydata = cdata.get(year);
                
                Double p = null;
                
                for (String key : ydata.keySet())
                {
                    if (key.toLowerCase().contains(datekey.toLowerCase()))
                    {
                        p = ExcelRC.asDouble(ydata.get(key));
                    }
                }

                sb.append(",");
                sb.append(n2s(p));
            }

            Util.out(sb.toString());
        }
    }

    private String n2s(Double v)
    {
        if (v == null)
            return "-";
        else
            return String.format("%.1f", v);
    }
}
