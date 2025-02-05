package rtss.ww2losses.helpers;

import rtss.data.selectors.Gender;
import rtss.util.Util;
import rtss.ww2losses.HalfYearEntries;
import rtss.ww2losses.HalfYearEntry;

import static rtss.data.population.forward.ForwardPopulation.years2days;

/*
 * Печатать количество смертей в каждом полугодии для возрастных линий начинающихся с января 1941
 */
public class ShowAgeSliceDeathHistory
{
    public static void show(HalfYearEntries<HalfYearEntry> halves) throws Exception
    {
        show(halves, Gender.BOTH, 0, 20);
    }

    public static void show(HalfYearEntries<HalfYearEntry> halves, Gender gender, double age1, double age2) throws Exception
    {
        if (gender == Gender.BOTH)
        {
            show(halves, Gender.MALE, age1, age2);
            show(halves, Gender.FEMALE, age1, age2);
            return;
        }

        double slice = 1.0;

        Util.out("");
        Util.out(String.format("Число смертей " + gender + " в возрастах %.1f-%.1f", age1, age2));
        Util.out("");

        printHeaders(age1, age2, slice);


        for (HalfYearEntry he : halves)
        {
            if (he.year == 1946)
                break;
            
            StringBuilder sb = new StringBuilder();
            sb.append(he.id());

            double offset = he.offset_start1941();
            
            for (double age = age1; age <= age2; age += slice)
            {
                double xage1 = age + offset;
                double xage2 = xage1 + slice;

                int nd1 = years2days(xage1);
                int nd2;
                if (slice != 1.0)
                    nd2 = years2days(xage2) - 1;
                else
                    nd2 = nd1 + 365 - 1;
                
                double v = he.actual_deaths.sumDays(gender, nd1, nd2);

                sb.append(String.format(" %8s", f2k(v)));
            }

            Util.out(sb.toString());
        }
    }
    
    private static void printHeaders(double age1, double age2, double slice)
    {
        // обозначения колонок
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-6s ", "п/год"));
        for (double age = age1; age <= age2; age += slice)
        {
            if (age == Math.floor(age))
            {
                sb.append(String.format("   %2d    ", (int) Math.floor(age)));
            }
            else
            {
                sb.append(String.format("  %4.1f   ", age));
            }
        }
        Util.out(sb.toString());

        // разделители
        sb = new StringBuilder();
        sb.append(repeat('=', 6));
        for (double age = age1; age <= age2; age += slice)
            sb.append(" " + repeat('=', 8));
        Util.out(sb.toString());
    }

    private static String repeat(char c, int times)
    {
        String s = "";
        while (s.length() != times)
            s += c;
        return s;
    }

    private static String f2s(double v)
    {
        String s = String.format("%,15.0f", v);
        while (s.startsWith(" "))
            s = s.substring(1);
        return s;
    }

    private static String f2k(double v)
    {
        return f2s(v / 1000.0);
    }
}
