package rtss.latinamerica;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import rtss.util.Util;
import rtss.util.excel.Excel;
import rtss.util.excel.ExcelRC;

public class AverageLatinAmericaPopulation
{
    public static void main(String[] args)
    {
        try
        {
            new AverageLatinAmericaPopulation().do_main();
        }
        catch (Exception ex)
        {
            Util.err("** Exception:");
            ex.printStackTrace();
        }
    }

    private void do_main() throws Exception
    {
        LinkedHashMap<String, Map<Integer, Double>> brignoli = loadPopulations("latinamerica/Brignoli-population/Brignoli-population.xlsx",
                                                                               "годовые",
                                                                               0.001);

        LinkedHashMap<String, Map<Integer, Double>> sala = loadPopulations("latinamerica/Latin-America-Population.xlsx",
                                                                           "SALA WPP 2024 195x",
                                                                           1.0);

        LinkedHashMap<String, Map<Integer, Double>> result = new LinkedHashMap<>();
        List<String> cnames = new ArrayList<>();

        for (String cname : brignoli.keySet())
        {
            Map<Integer, Double> pb = brignoli.get(cname);
            Map<Integer, Double> ps = sala.get(cname);

            if (ps == null)
            {
                result.put(cname, pb);
                cnames.add(cname);
            }
            else
            {
                Map<Integer, Double> m = new HashMap<>();
                for (int year = 1900; year <= 2023; year++)
                    m.put(year, (ps.get(year) + pb.get(year)) / 2);
                
                result.put(cname, m);

                if (cname.equals("Венесуэла"))
                {
                    for (int year = 2001; year <= 2023; year++)
                        m.put(year, ps.get(year));
                }

                cnames.add(cname);
            }
        }

        out(cnames, result);
    }

    private LinkedHashMap<String, Map<Integer, Double>> loadPopulations(String path, String sheet, double multiplier) throws Exception
    {
        String cname = null;

        try
        {
            LinkedHashMap<String, Map<Integer, Double>> cm = new LinkedHashMap<>();

            ExcelRC rc = Excel.readSheet(path, false, sheet);
            int col_year = 0;

            if (!"год".equals(rc.get(0, col_year)))
                throw new Exception("Несовпадение структуры файла");

            int ncols = rc.get(0).size();

            for (int col = 1; col < ncols; col++)
            {
                if (rc.isEmpty(0, col))
                    continue;

                cname = rc.asString(0, col);

                cname = cname.replace("\r\n", "");
                cname = cname.replace("\n", "");
                cname = Util.despace(cname);

                if (cname.equals("Венецуэлла"))
                    cname = "Венесуэла";

                if (cname.equals("Эль-Сальвадор"))
                    cname = "Сальвадор";

                if (cm.containsKey(cname))
                    throw new Exception("Duplicate country");
                cm.put(cname, loadCountry(rc, col, col_year, 1900, 2023, multiplier));
            }

            return cm;
        }
        catch (Exception ex)
        {
            throw new Exception("path=" + path + ", cname=" + cname, ex);
        }

    }

    private Map<Integer, Double> loadCountry(ExcelRC rc, int col_country, int col_year, int y1, int y2, double multiplier) throws Exception
    {
        Map<Integer, Double> m = new HashMap<>();

        for (int nr = 1; nr < rc.size() && !rc.isEndRow(nr); nr++)
        {
            if (rc.isEmpty(nr, col_year))
                continue;
            try
            {
                int year = rc.asInt(nr, col_year);
                if (year >= y1 && year <= y2)
                {
                    if (m.containsKey(year))
                        throw new Exception("Duplicate year");
                    double pop = rc.asDouble(nr, col_country);
                    m.put(year, pop * multiplier);
                }
            }
            catch (Exception ex)
            {
                throw new Exception("nr=" + nr, ex);
            }
        }

        return m;
    }

    private void out(List<String> cnames, LinkedHashMap<String, Map<Integer, Double>> result)
    {
        final String comma = ",";
        final String quote = "\"";

        StringBuilder sb = new StringBuilder();
        sb.append("год");
        for (String cname : cnames)
        {
            sb.append(comma + quote + cname + quote);
        }
        Util.out(sb.toString());

        for (int year = 1900; year <= 2023; year++)
        {
            sb = new StringBuilder();
            sb.append("" + year);
            for (String cname : cnames)
                sb.append(String.format(",%.3f", result.get(cname).get(year)));
            Util.out(sb.toString());
        }
    }
}
