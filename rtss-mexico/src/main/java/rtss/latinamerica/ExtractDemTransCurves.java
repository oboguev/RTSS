package rtss.latinamerica;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    }

    private List<Country> countries = new ArrayList<>();

    private void addCountry(String cname, int startYear, String sheet)
    {
        Country c = new Country();
        c.cname = cname;
        c.startYear = startYear;
        c.sheet = sheet;
        countries.add(c);
    }

    private void defineCountries() throws Exception
    {
        /*
         * Датировка по 10%-му снижению смертности.
         * Когда значение смертности ныряет под 90% начального значения или, в случае колебаний, начального значения тренда.
         */
        addCountry("Аргентина", 1885, "Аргентина-Колвер");
        addCountry("Бразилия", 1893, "Бразилия");
        addCountry("Венесуэла", 1921, "Венесуэла-Колвер");
        addCountry("Венесуэла", 1921, "Венесуэла-Бриньоли");
        addCountry("Гватемала", 1936, "Гватемала"); // ### не включать 
        addCountry("Гондурас", 1944, "Гондурас-правка");
        addCountry("Колумбия", 1922, "Колумбия");
        addCountry("Коста-Рика", 1925, "Коста-Рика-Бриньоли");
        addCountry("Коста-Рика", 1921, "Коста-Рика-Колвер");
        addCountry("Мексика", 1917, "Мексика-Бриньоли-CONAPO-реформа");
        addCountry("Мексика", 1917, "Мексика-Колвер-CONAPO-реформа");
        addCountry("Панама", 1918, "Панама");
        addCountry("Перу", 1940, "Перу");
        addCountry("Сальвадор", 1942, "Сальвадор");
        addCountry("Чили", 1912, "Чили-Бриньоли");
        addCountry("Чили", 1912, "Чили-Колвер");
        addCountry("Эквадор", 1915, "Эквадор");
    }

    private void do_main() throws Exception
    {
        defineCountries();

        for (Country c : countries)
            loadCountry(c);
        
        countries.sort(Comparator.comparingInt(c -> c.startYear));
        countries.sort(Comparator.comparing(c -> c.cname));
        
        // ###
    }
    
    private void loadCountry(Country c) throws Exception
    {
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
        
        Map<Integer,Double> cbrs = new HashMap<>();
        Map<Integer,Double> cdrs = new HashMap<>();
        
        for (int nr = tnr + 1; nr < rc.size() && !rc.isEndRow(nr); nr++)
        {
            String ys = rc.asString(nr, tnc);
            if (ys == null || ys.equals(""))
                continue;
            
            if (ys.endsWith(".0"))
                ys = Util.stripTail(ys, ".0");
            int year = Integer.parseInt(ys);

            double cbr;
            double cdr;
            
            if (rc.isEmpty(nr, tnc + 1) && rc.isEmpty(nr, tnc + 2))
                continue;
            
            try
            {
                cbr = rc.asDouble(nr, tnc + 1);
                cdr = rc.asDouble(nr, tnc + 2);
            }
            catch (Exception ex)
            {
                throw new Exception("Unable to parse series in " + c.sheet, ex);
            }

            if (cbrs.containsKey(year))
                throw new Exception("Duplicate year in " + c.sheet);
            cbrs.put(year, cbr);
            cdrs.put(year, cdr);
        }
        
        // int minYear = Collections.min(cbrs.keySet());
        int maxYear = Collections.max(cbrs.keySet());
        
        c.cbr = new double[maxYear - c.startYear + 1];
        c.cdr = new double[maxYear - c.startYear + 1];
        
        for (int year = c.startYear; year <= maxYear; year++)
        {
            c.cbr[year - c.startYear] = cbrs.get(year);
            c.cdr[year - c.startYear] = cdrs.get(year);
        }
    }
}
