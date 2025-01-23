package rtss.ww2losses.helpers;

import rtss.data.population.Population;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;
import rtss.ww2losses.HalfYearEntry;
import rtss.ww2losses.util.HalfYearEntries;

public class PrintHalves
{
    private static int MAX_AGE = Population.MAX_AGE;

    public static void print(HalfYearEntries<HalfYearEntry> halves) throws Exception
    {
        Util.out("");
        Util.out("Данные по полугодиям");
        Util.out("");
        Util.out("    со.н = ожидаемое число смертей в наличном на начало войны населении в условиях мира");
        Util.out("    сд.н = добавочное число смертей в наличном на начало войны населении из-за войны");
        Util.out("    ро   = ожидаемое число рождений в условиях мира");
        Util.out("");
        Util.out("полугодие со.н сд.н ро");
        Util.out("");

        for (HalfYearEntry he : halves)
        {
            if (he.year == 1946)
                break;
            
            double d1 = 0;
            double d2 = 0;
            
            if (he.accumulated_excess_deaths != null)
                d1 = he.accumulated_excess_deaths.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);
            
            if (he.next.accumulated_excess_deaths != null)
                d2 = he.next.accumulated_excess_deaths.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);
            
            Util.out(String.format("%s %5s %5s %5s", 
                                   he.toString(),
                                   f2k(he.expected_nonwar_deaths / 1000.0),
                                   f2k((d2 - d1) / 1000.0),
                                   f2k(he.expected_nonwar_births / 1000.0)
                                   ));
        }
    }

    private static String f2k(double v)
    {
        String s = String.format("%,15.0f", v);
        while (s.startsWith(" "))
            s = s.substring(1);
        return s;
    }
}
