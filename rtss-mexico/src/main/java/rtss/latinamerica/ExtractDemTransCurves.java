package rtss.latinamerica;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtss.math.algorithms.smooth.SmoothSeries;
import rtss.util.Util;
import rtss.util.excel.Excel;
import rtss.util.excel.ExcelRC;

public class ExtractDemTransCurves
{
    public static void main(String[] args)
    {
        try
        {
            new ExtractDemTransCurves().do_main();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    public static class Country
    {
        public String cname;
        public int startYear;
        public String sheet;
        public double[] cbr;
        public double[] cdr;
        public Map<Integer, Double> mcbr = new HashMap<>();
        public Map<Integer, Double> mcdr = new HashMap<>();
        public double weight = 1.0;
    }

    private List<Country> countries = new ArrayList<>();

    private void addCountry(Country c) throws Exception
    {
        countries.add(c);
    }

    private void addCountry(String cname, int startYear, String sheet) throws Exception
    {
        addCountry(cname, startYear, sheet, null);
    }

    private void addCountry(String cname, int startYear, String sheet, LoadCountryOptions options) throws Exception
    {
        addCountry(getCountry(cname, startYear, sheet, options));
    }

    private Country getCountry(String cname, int startYear, String sheet) throws Exception
    {
        return getCountry(cname, startYear, sheet, null);
    }

    private Country getCountry(String cname, int startYear, String sheet, LoadCountryOptions options) throws Exception
    {
        Country c = new Country();
        c.cname = cname;
        c.startYear = startYear;
        c.sheet = sheet;
        loadCountry(c, options);
        return c;
    }

    private void defineCountries() throws Exception
    {
        Country c1, c2;

        /*
         * Датировка по 10%-му снижению смертности.
         * Когда значение смертности ныряет под 90% начального значения или, в случае колебаний, начального значения тренда.
         */
        addCountry("Аргентина", 1885, "Аргентина-Колвер");
        addCountry("Бразилия", 1893, "Бразилия");
        addCountry("Гватемала", 1948, "Гватемала-Колвер-Бриньоли");
        addCountry("Гондурас", 1948, "Гондурас-Колвер-Бриньоли");
        addCountry("Колумбия", 1922, "Колумбия");
        addCountry("Коста-Рика", 1923, "Коста-Рика-Бриньоли-обе-Колвер", new LoadCountryOptions().useSmoothCBR());
        addCountry("Мексика", 1917, "Мексика-Бриньоли-CONAPO-реформа"); 
        addCountry("Никарагуа", 1949, "Никарагуа-Бриньоли");
        addCountry("Панама", 1913, "Панама", new LoadCountryOptions().useSmoothCBR());
        addCountry("Перу", 1940, "Перу");
        addCountry("Сальвадор", 1938, "Сальвадор-Колвер-Бриньоли", new LoadCountryOptions().useSmoothCBR());  // 1938-1943 -- смертность "раздумывала"
        addCountry("Эквадор", 1915, "Эквадор");
        
        // Венесуэла усреднить и с 1921
        c1 = getCountry("Венесуэла", 1921, "Венесуэла-Колвер");
        c2 = getCountry("Венесуэла", 1921, "Венесуэла-Бриньоли");
        addCountry(average(c1, c2, 1921));

        // Чили усреднить и с 1922
        c1 = getCountry("Чили", 1922, "Чили-Бриньоли", new LoadCountryOptions().useSmoothCBR());
        c2 = getCountry("Чили", 1922, "Чили-Колвер", new LoadCountryOptions().useSmoothCBR());
        addCountry(average(c1, c2, 1922));

        /*
         * Устранить влияние войн, эпидемий и природных катастроф
         */
        if (haveCountry("Гватемала"))
        {
            interpolate("Гватемала", 1976, 1976);
            interpolate("Гватемала", 1980, 1982);
            
        }
        
        if (haveCountry("Гондурас"))
        {
            interpolate("Гондурас", 1973, 1974);
            interpolate("Гондурас", 1998, 1998);
        }
        
        interpolate("Колумбия", 1985, 1985);

        interpolate("Никарагуа", 1972, 1972);
        interpolate("Никарагуа", 1978, 1980);
        interpolate("Никарагуа", 1983, 1988);
        interpolate("Никарагуа", 1998, 1998);
        
        interpolate("Перу", 1970, 1970);
        interpolate("Сальвадор", 1979, 1983);

        /*
         * Устранить влияние пандемии ковида
         */
        for (Country c : countries)
            interpolate(c, 2020, 2022);
    }

    private void do_main() throws Exception
    {
        defineCountries();

        // countries.sort(Comparator.comparingInt(c -> c.startYear));
        countries.sort(Comparator.comparing(c -> c.cname));

        for (Country c : countries)
            eval(c); // ###

        out("cbr");
        out("cdr");
    }

    private Country getCountry(String cname) throws Exception
    {
        Country rc = null;

        for (Country c : countries)
        {
            if (c.cname.equals(cname))
            {
                if (rc != null)
                    throw new Exception("Две страны: " + cname);
                rc = c;
            }
        }

        if (rc == null)
            throw new Exception("Нет страны: " + cname);

        return rc;
    }

    private boolean haveCountry(String cname) throws Exception
    {
        for (Country c : countries)
        {
            if (c.cname.equals(cname))
                return true;
        }

        return false;
    }

    private void out(String what)
    {
        final char quote = '"';
        
        countries.sort(Comparator.comparingInt(c -> c.startYear));
        int nyears = countries.get(0).cbr.length;

        Util.out("");
        Util.out("Вывод " + what.toUpperCase());

        countries.sort(Comparator.comparing(c -> c.cname));

        StringBuilder sb = new StringBuilder("набор");
        for (Country c : countries)
            sb.append("," + quote + c.sheet + quote);
        sb.append(",среднее");
        Util.out(sb.toString());

        sb = new StringBuilder("вес");
        for (Country c : countries)
            sb.append("," + c.weight);
        sb.append(",");
        Util.out(sb.toString());

        sb = new StringBuilder("начало");
        for (Country c : countries)
            sb.append("," + c.startYear);
        sb.append(",");
        Util.out(sb.toString());

        sb = new StringBuilder("год");
        for (Country c : countries)
            sb.append("," + quote + c.cname + quote);
        sb.append(",среднее");
        sb.append(",\"сглаженное среднее\"");
        Util.out(sb.toString());

        /* ------------------ calculate average ------------------------- */
        
        double[] average = new double[nyears];

        for (int year = 0; year < nyears; year++)
        {
            double sum = 0;
            double weights = 0;

            for (Country c : countries)
            {
                if (year < c.cbr.length)
                {
                    double v, v0;
                    switch (what)
                    {
                    case "cbr":
                        v = c.cbr[year];
                        v0 = c.cbr[0];
                        break;

                    case "cdr":
                        v = c.cdr[year];
                        v0 = c.cdr[0];
                        break;

                    default:
                        throw new IllegalArgumentException();
                    }

                    v = v / v0;

                    sum += c.weight * v;
                    weights += c.weight;
                }
            }
            
            average[year] = sum / weights;
        }
        
        double lambda = 5.0;
        double[] smooth = SmoothSeries.smoothWhittaker(average, lambda, null);
        smooth[0] = 1.0;

        /* ------------------ generate output ----------------------- */

        for (int year = 0; year < nyears; year++)
        {
            sb = new StringBuilder("" + year);

            for (Country c : countries)
            {
                sb.append(",");

                if (year < c.cbr.length)
                {
                    double v, v0;
                    switch (what)
                    {
                    case "cbr":
                        v = c.cbr[year];
                        v0 = c.cbr[0];
                        break;

                    case "cdr":
                        v = c.cdr[year];
                        v0 = c.cdr[0];
                        break;

                    default:
                        throw new IllegalArgumentException();
                    }

                    v = v / v0;

                    sb.append(String.format("%.3f", v));
                }
            }

            sb.append(String.format(",%.3f", average[year]));
            sb.append(String.format(",%.3f", smooth[year]));
            
            Util.out(sb.toString());
        }
    }

    private void eval(Country c)
    {
        double v = 1.0;

        double cbr0 = c.cbr[0];
        double cdr0 = c.cdr[0];

        for (int k = 0; k < c.cbr.length; k++)
        {
            double ngr = 50 * c.cbr[k] / cbr0 - 30 * c.cdr[k] / cdr0;
            v *= (1 + ngr / 1000);
        }

        Util.out(String.format("%s %.2f", c.sheet, v));
    }

    private void loadCountry(Country c, LoadCountryOptions options) throws Exception
    {
        if (options == null)
            options = new LoadCountryOptions();
        
        ExcelRC rc = Excel.readSheet("latinamerica/Latin-America-Vital-Rates-Yearly.xlsx", true, c.sheet);
        Integer tnr = null;
        Integer tnc = null;

        for (int nr = 0; nr < rc.size() && !rc.isEndRow(nr); nr++)
        {
            int ncols = rc.get(nr).size();
            for (int nc = 0; nc < ncols; nc++)
            {
                if (nc + 3 >= ncols)
                    break;
                String s1 = rc.asString(nr, nc);
                String s2 = rc.asString(nr, nc + 1);
                String s3 = rc.asString(nr, nc + 2);
                String s4 = rc.asString(nr, nc + 3);
                if ("год".equals(s1) && "рождаемость".equals(s2) && "смертность".equals(s3) && "еп".equals(s4))
                {
                    if (tnr != null || tnc != null)
                        throw new Exception("Duplicate location in " + c.sheet);
                    tnc = nc;
                    tnr = nr;
                }
            }
        }

        if (tnr == null || tnc == null)
            throw new Exception("Unabe to locate series in " + c.sheet);
        
        if (options.useSmoothCBR && !"р-сгл".equals(rc.asString(tnr, tnc + 4)))
            throw new Exception("Unabe to locate series in " + c.sheet);
        
        final int col_year = tnc;
        final int col_cbr = options.useSmoothCBR ? tnc + 4 : tnc + 1;
        final int col_cdr = tnc + 2;

        for (int nr = tnr + 1; nr < rc.size() && !rc.isEndRow(nr); nr++)
        {
            String ys = rc.asString(nr, col_year);
            if (ys == null || ys.equals(""))
                continue;

            if (ys.endsWith(".0"))
                ys = Util.stripTail(ys, ".0");
            int year = Integer.parseInt(ys);

            double cbr;
            double cdr;

            if (rc.isEmpty(nr, col_cbr) && rc.isEmpty(nr, col_cdr))
                continue;

            try
            {
                cbr = rc.asDouble(nr, col_cbr);
                cdr = rc.asDouble(nr, col_cdr);
            }
            catch (Exception ex)
            {
                throw new Exception("Unable to parse series in " + c.sheet, ex);
            }

            if (c.mcbr.containsKey(year))
                throw new Exception("Duplicate year in " + c.sheet);
            c.mcbr.put(year, cbr);
            c.mcdr.put(year, cdr);
        }

        fill(c);
    }

    private void fill(Country c)
    {
        int maxYear = Collections.max(c.mcbr.keySet());
        // int minYear = Collections.min(cbrs.keySet());

        c.cbr = new double[maxYear - c.startYear + 1];
        c.cdr = new double[maxYear - c.startYear + 1];

        for (int year = c.startYear; year <= maxYear; year++)
        {
            c.cbr[year - c.startYear] = c.mcbr.get(year);
            c.cdr[year - c.startYear] = c.mcdr.get(year);
        }
    }

    private Country average(Country c1, Country c2, int startYear)
    {
        Country c = new Country();
        if (!c1.cname.equals(c2.cname))
            throw new IllegalArgumentException();
        c.cname = c1.cname;
        c.sheet = c1.cname + "-среднее";
        c.startYear = startYear;

        int miny1 = Collections.min(c1.mcbr.keySet());
        int miny2 = Collections.min(c2.mcbr.keySet());

        int maxy1 = Collections.max(c1.mcbr.keySet());
        int maxy2 = Collections.max(c2.mcbr.keySet());

        if (maxy1 != maxy2)
            throw new IllegalArgumentException();

        for (int year = Math.min(miny1, miny2); year <= maxy1; year++)
        {
            if (year >= miny1 && year >= miny2)
            {
                c.mcbr.put(year, (c1.mcbr.get(year) + c2.mcbr.get(year)) / 2);
                c.mcdr.put(year, (c1.mcdr.get(year) + c2.mcdr.get(year)) / 2);
            }
            else if (year >= miny1)
            {
                c.mcbr.put(year, c1.mcbr.get(year));
                c.mcdr.put(year, c1.mcdr.get(year));
            }
            else
            {
                c.mcbr.put(year, c2.mcbr.get(year));
                c.mcdr.put(year, c2.mcdr.get(year));
            }
        }

        fill(c);

        return c;
    }

    private void interpolate(String cname, int y1, int y2) throws Exception
    {
        interpolate(getCountry(cname), y1, y2);
    }

    private void interpolate(Country c, int y1, int y2) throws Exception
    {
        interpolate(c.mcbr, y1, y2);
        interpolate(c.mcdr, y1, y2);
        fill(c);
    }

    private void interpolate(Map<Integer, Double> m, int y1, int y2) throws Exception
    {
        y1--;
        y2++;
        double v1 = m.get(y1);
        double v2 = m.get(y2);
        for (int year = y1 + 1; year < y2; year++)
        {
            double v = v1 + (v2 - v1) * (year - y1) / (double) (y2 - y1);
            m.put(year, v);
        }
    }
    
    public static class LoadCountryOptions
    {
        public boolean useSmoothCBR;
        
        public LoadCountryOptions useSmoothCBR()
        {
            useSmoothCBR = true;
            return this;
        }
    }
}
