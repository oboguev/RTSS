package rtss.un.wpp.latinamerica;

import rtss.util.Util;
import rtss.util.excel.ExcelRC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtss.un.wpp.WPP;
import rtss.un.wpp.WPP2024;

/*
 * To load the WPP file, use Java heap size 16G:
 *     java -Xmx16g
 */
public class LatinAmericaVitalRates
{
    public static void main(String[] args)
    {
        try (WPP wpp = new WPP2024())
        {
            new LatinAmericaVitalRates().print_quinquennial(wpp);
            // new LatinAmericaVitalRates().print_yearly(wpp);
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    private void print_quinquennial(WPP wpp) throws Exception
    {
        this.wpp = wpp;

        if (Util.False)
        {
            for (String s : wpp.listCountries())
                Util.out(s);
        }

        defineCountries();

        show_rate("Crude birth rates", "CBR");
        show_rate("Crude death rates", "CDR");
        show_rate("Total fertility rates", "TFR");
        show_rate("Net reproduction rates", "NRR");

        show_population(1950, 2000, 5);
    }

    private void show_rate(String ratePrintableName, String rateCode) throws Exception
    {
        Util.out("");
        Util.out("==================== " + ratePrintableName + " ====================");
        show_rates_header();

        for (String rname : Countries)
        {
            String ename = CountryNames.r2e(rname);
            show_rates(rname, ename, rateCode);
        }
    }

    private void show_population(int y1, int y2, int ystep) throws Exception
    {
        Util.out("");
        Util.out("==================== Население на начало года ====================");
        show_population_header(y1, y2, ystep);

        for (String rname : Countries)
        {
            String ename = CountryNames.r2e(rname);
            show_population(rname, ename, y1, y2, ystep);
        }
    }

    /* ======================================================================================== */

    private WPP wpp;
    private final List<String> Countries = new ArrayList<>();

    private void defineCountries() throws Exception
    {
        defineCountry("Коста-Рика");
        defineCountry("Гватемала");
        defineCountry("Колумбия");
        defineCountry("Эквадор");
        defineCountry("Бразилия");
        defineCountry("Парагвай");
        defineCountry("Перу");
        defineCountry("Боливия");
        defineCountry("Сальвадор");
        defineCountry("Чили");
        defineCountry("Аргентина");
        defineCountry("Мексика");
        defineCountry("Куба");
        defineCountry("Панама");
        defineCountry("Доминиканская республика");
        defineCountry("Гондурас");
        defineCountry("Уругвай");
        // defineCountry("Никарагуа");
    }

    private void defineCountry(String rname) throws Exception
    {
        Countries.add(rname);
        if (CountryNames.r2e(rname) == null)
            throw new Exception("Unmapped country name: " + rname);
    }

    /* ======================================================================================== */

    private void show_rates_header() throws Exception
    {
        StringBuilder sb = new StringBuilder("страна");

        for (int year = 1950; year < 2020; year += 5)
            sb.append(String.format(",%d-%d", year, year + 4));

        Util.out(sb.toString());
    }

    private void show_rates(String rname, String ename, String which) throws Exception
    {
        StringBuilder sb = new StringBuilder(rname);

        for (int year = 1950; year < 2020; year += 5)
            sb.append("," + rate(ename, year, year + 4, which));

        Util.out(sb.toString());
    }

    private String rate(String ename, int year1, int year2, String which) throws Exception
    {
        Map<Integer, Map<String, Object>> cdata = wpp.forCountry(ename);

        double sum = 0;
        int nyears = 0;

        for (int year = year1; year <= year2; year++)
        {
            Map<String, Object> ydata = cdata.get(year);
            if (ydata == null)
                throw new Exception("Missing data");

            boolean found = false;

            for (String key : ydata.keySet())
            {
                if (which.equals("CBR") && key.toLowerCase().contains("Crude Birth Rate".toLowerCase()))
                {
                    if (found)
                        throw new Exception("Duplicate data");
                    found = true;
                    sum += ExcelRC.asRequiredDouble(ydata.get(key));
                }
                else if (which.equals("CDR") && key.toLowerCase().contains("Crude Death Rate".toLowerCase()))
                {
                    if (found)
                        throw new Exception("Duplicate data");
                    found = true;
                    sum += ExcelRC.asRequiredDouble(ydata.get(key));
                }
                else if (which.equals("TFR") && key.toLowerCase().contains("Total Fertility Rate".toLowerCase()))
                {
                    if (found)
                        throw new Exception("Duplicate data");
                    found = true;
                    sum += ExcelRC.asRequiredDouble(ydata.get(key));
                }
                else if (which.equals("NRR") && key.toLowerCase().contains("Net Reproduction Rate".toLowerCase()))
                {
                    if (found)
                        throw new Exception("Duplicate data");
                    found = true;
                    sum += ExcelRC.asRequiredDouble(ydata.get(key));
                }
            }

            if (!found)
                throw new Exception("Missing data");

            nyears++;
        }

        return String.format("%.2f", sum / nyears);
    }

    /* ======================================================================================== */

    private void show_population_header(int y1, int y2, int ystep)
    {
        StringBuilder sb = new StringBuilder("страна");

        for (int year = y1; year <= y2; year += ystep)
            sb.append(String.format(",%d", year));

        Util.out(sb.toString());
    }

    private void show_population(String rname, String ename, int y1, int y2, int ystep) throws Exception
    {
        StringBuilder sb = new StringBuilder(rname);

        Map<Integer, Map<String, Object>> cdata = wpp.forCountry(ename);
        if (cdata == null)
            throw new Exception("Missing data");

        for (int year = y1; year <= y2; year += ystep)
            sb.append("," + population(cdata, year));

        Util.out(sb.toString());

    }

    private String population(Map<Integer, Map<String, Object>> cdata, int year) throws Exception
    {
        Map<String, Object> ydata = cdata.get(year);
        if (ydata == null)
            throw new Exception("Missing data");

        boolean found = false;
        long value = 0;

        for (String key : ydata.keySet())
        {
            if (key.toLowerCase().contains("Total Population, as of 1 January".toLowerCase()))
            {
                if (found)
                    throw new Exception("Duplicate data");
                found = true;
                double dvalue = ExcelRC.asRequiredDouble(ydata.get(key));
                value = Math.round(dvalue);
            }
        }

        if (!found)
            throw new Exception("Missing data");

        return String.format("%d", value);
    }

    /* ======================================================================================== */

    @SuppressWarnings("unused")
    private void print_yearly(WPP wpp) throws Exception
    {
        this.wpp = wpp;

        defineCountries();

        Map<String, Map<Integer, Map<String, Object>>> cn2cdata = new HashMap<>();

        StringBuilder sb = new StringBuilder();

        for (String rname : Countries)
        {
            if (sb.length() != 0)
                sb.append(",,");
            sb.append("\"" + rname + "\"");

            String ename = CountryNames.r2e(rname);
            Map<Integer, Map<String, Object>> cdata = wpp.forCountry(ename);
            cn2cdata.put(rname, cdata);
        }

        Util.out("\"год\"," + sb.toString());

        for (int year = 1950; year <= 2023; year++)
        {
            sb = new StringBuilder();
            sb.append("" + year);

            for (String rname : Countries)
            {
                Map<Integer, Map<String, Object>> cdata = cn2cdata.get(rname);
                Map<String, Object> ydata = cdata.get(year);
                double cbr = ydata2double(ydata, "Crude Birth Rate");
                double cdr = ydata2double(ydata, "Crude Death Rate");
                sb.append(String.format(",%.1f,%.1f", cbr, cdr));
            }

            Util.out(sb.toString());
        }
    }

    private double ydata2double(Map<String, Object> ydata, String key) throws Exception
    {
        for (String xkey : ydata.keySet())
        {
            if (xkey.toLowerCase().contains(key.toLowerCase()))
                return ExcelRC.asRequiredDouble(ydata.get(xkey));
        }

        throw new Exception("Missing data");
    }
}
