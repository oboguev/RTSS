package rtss.un.wpp.misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import rtss.un.wpp.WPP;
import rtss.un.wpp.WPP2024;
import rtss.util.Util;

/*
 * To load the WPP file, use Java heap size 16G:
 *     java -Xmx16g
 */
public class HighestRates
{
    public static void main(String[] args)
    {
        try (WPP wpp = new WPP2024())
        {
            new HighestRates(wpp).do_main();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private final WPP wpp;

    private HighestRates(WPP wpp)
    {
        this.wpp = wpp;
    }

    private void do_main() throws Exception
    {
        for (String country : wpp.listCountries())
            do_country(country);
        Collections.sort(countryRates);

        for (CountryRate cr : countryRates)
        {
            Util.out(String.format("%s = %.1f in %s", cr.country, cr.rate, cr.displayYears()));
        }
    }

    private List<CountryRate> countryRates = new ArrayList<>();

    private void do_country(String country) throws Exception
    {
        Map<Integer, Map<String, Object>> cm = wpp.forCountry(country);
        CountryRate cr = new CountryRate(country);

        for (int year : Util.sort(cm.keySet()))
        {
            Map<String, Object> ym = cm.get(year);
            Object o = ym.get("Rate of Natural Change (per 1,000 population)");
            if (o == null)
                continue;
            String so = Util.despace(o.toString()).trim();
            if (so.length() == 0)
                continue;
            double rate = Double.parseDouble(so);
            rate = Math.round(rate * 10) * 0.1;
            if (rate > cr.rate)
            {
                cr.rate = rate;
                cr.years.clear();
                cr.years.add(year);
            }
            else if (rate == cr.rate)
            {
                cr.years.add(year);
            }
        }

        countryRates.add(cr);
    }

    private static class CountryRate implements Comparable<CountryRate>
    {
        public final String country;
        public double rate = -1;
        public List<Integer> years = new ArrayList<>();

        public CountryRate(String country)
        {
            this.country = country;
        }

        @Override
        public int compareTo(CountryRate o)
        {
            if (rate > o.rate)
                return -1;
            else if (rate < o.rate)
                return 1;
            else
                return 0;
        }
        
        String displayYears()
        {
            if (years.size() == 0)
                return "no years";
            
            if (years.size() == 1)
                return "" + years.get(0);
            
            Integer lastSeen = null;
            Integer lastInserted = null;
            StringBuilder sb = new StringBuilder();
            
            for (int year : years)
            {
                if (lastInserted == null)
                {
                    // first
                    lastInserted = lastSeen = year;
                    sb.append("" + year);
                }
                else if (year == lastSeen + 1)
                {
                    // continuous
                    lastSeen = year;
                }
                else
                {
                    // end of continuous range
                    sb.append("-" + lastSeen);
                    sb.append(", " + year);
                    lastInserted = lastSeen = year;
                }
            }
            
            if (lastSeen != lastInserted)
                sb.append("-" + lastSeen);
            
            return sb.toString();
        }
    }
}
