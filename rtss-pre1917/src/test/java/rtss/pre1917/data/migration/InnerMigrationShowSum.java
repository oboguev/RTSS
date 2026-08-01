package rtss.pre1917.data.migration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rtss.pre1917.LoadData;
import rtss.pre1917.data.Taxon;
import rtss.util.Util;

public class InnerMigrationShowSum
{
    public static void main(String[] args)
    {
        try
        {
            new InnerMigrationShowSum().do_main_e50();
            new InnerMigrationShowSum().do_main_rcpt();
            new InnerMigrationShowSum().do_main_breakdown();
        }
        catch (Exception ex)
        {
            Util.err("** Exception:");
            ex.printStackTrace();
        }
    }

    private InnerMigrationShowSum() throws Exception
    {
    }

    private InnerMigration innerMigration = new LoadData().loadInnerMigration();

    /* ==================================================================================================================== */

    private void do_main_e50() throws Exception
    {
        Util.out("Выход переселенцев из 50 европейских губерний и возвращение в них");
        Util.out("");

        List<String> e50 = gov_e50();

        for (int year = 1896; year <= 1903; year++)
        {
            Util.out(String.format("%d %,d %,d", year, outFlow(year, e50), inFlow(year, e50)));
        }
    }

    private List<String> gov_e50() throws Exception
    {
        List<String> tnames = new ArrayList<>();
        Taxon tx = Taxon.of("50 губерний Европейской России", 1913, null);
        for (String tname : Util.sort(tx.territories.keySet()))
        {
            if (Taxon.isComposite(tname))
                continue;

            switch (tname)
            {
            case "Московская с Москвой":
            case "Санкт-Петербургская с Санкт-Петербургом":
            case "Таврическая с Севастополем":
            case "Херсонская с Одессой":
            case "г. Москва":
            case "г. Николаев":
            case "г. Одесса":
            case "г. Санкт-Петербург":
            case "г. Севастополь":
                continue;

            default:
                break;
            }

            tnames.add(tname);
            // Util.out(tname);
        }

        return tnames;
    }

    private long inFlow(int year, List<String> tnames)
    {
        long v = 0;
        for (String tname : tnames)
            v += innerMigration.inFlow(tname, year);
        return v;
    }

    private long outFlow(int year, List<String> tnames)
    {
        long v = 0;
        for (String tname : tnames)
            v += innerMigration.outFlow(tname, year);
        return v;
    }

    /* ==================================================================================================================== */

    private void do_main_rcpt() throws Exception
    {
        show_rcpt(1896);
        show_rcpt(1897);
        show_rcpt(1898);
        show_rcpt(1899);
        show_rcpt(1896, 1899);
    }

    private void show_rcpt(int year) throws Exception
    {
        show_rcpt(year, year);
    }

    private void show_rcpt(int y1, int y2) throws Exception
    {
        Util.out("");
        if (y1 == y2)
            Util.out(String.format("Колонизуемые губернии и области за %d год: приезд, возвращение, %% возвращения", y1));
        else
            Util.out(String.format("Колонизуемые губернии и области за %d-%d годы: приезд, возвращение, %% возвращения", y1, y2));

        List<String> tnames = tnames_empire();

        for (String tname : tnames)
        {
            long inFlow = inFlow(tname, y1, y2);
            long outFlow = outFlow(tname, y1, y2);
            if (inFlow > outFlow)
                Util.out(String.format("%s %,d %,d %.1f", tname, inFlow, outFlow, 100.0 * outFlow / (double) inFlow));
        }

    }

    private List<String> tnames_empire() throws Exception
    {
        Util.out("");

        List<String> tnames = new ArrayList<>();
        Taxon tx = Taxon.of("Империя", 1913, null);

        for (String tname : tx.flattenUsedEementaryTerritories(1913))
        {
            if (Taxon.isComposite(tname))
                continue;

            switch (tname)
            {
            case "Московская с Москвой":
            case "Санкт-Петербургская с Санкт-Петербургом":
            case "Таврическая с Севастополем":
            case "Херсонская с Одессой":
            case "Бакинская с Баку":
            case "Варшавская с Варшавой":
            case "г. Москва":
            case "г. Николаев":
            case "г. Одесса":
            case "г. Санкт-Петербург":
            case "г. Севастополь":
            case "г. Баку":
            case "г. Варшава":
                continue;

            default:
                break;
            }

            tnames.add(tname);
            // Util.out(tname);
        }

        Collections.sort(tnames);

        return tnames;
    }

    private long inFlow(String tname, int y1, int y2)
    {
        long v = 0;
        for (int year = y1; year <= y2; year++)
            v += innerMigration.inFlow(tname, year);
        return v;
    }

    private long outFlow(String tname, int y1, int y2)
    {
        long v = 0;
        for (int year = y1; year <= y2; year++)
            v += innerMigration.outFlow(tname, year);
        return v;
    }

    /* ==================================================================================================================== */

    private static final String[] destiations = { "Акмолинская обл.",
                                                  "Амурская обл.",
                                                  "Енисейская",
                                                  "Забайкальская обл.",
                                                  "Иркутская",
                                                  "Приморская обл.",
                                                  "Семипалатинская обл.",
                                                  "Семиреченская обл.",
                                                  "Тобольская",
                                                  "Томская",
                                                  "Тургайская обл."
    };

    private void do_main_breakdown() throws Exception
    {
        show_breakdown(1896);
        show_breakdown(1897);
        show_breakdown(1898);
        show_breakdown(1899);
        show_breakdown(1896, 1899);
    }

    private void show_breakdown(int year) throws Exception
    {
        show_breakdown(year, year);
    }

    private void show_breakdown(int y1, int y2) throws Exception
    {
        Util.out("");
        if (y1 == y2)
            Util.out(String.format("Разбивка колонизационного потока по губерниям и областям назначения за %d год", y1));
        else
            Util.out(String.format("Разбивка колонизационного потока по губерниям и областям назначения за %d-%d годы", y1, y2));
        
        double sum = 0;

        for (String tname : destiations)
        {
            long inFlow = inFlow(tname, y1, y2);
            sum += inFlow;
        }

        for (String tname : destiations)
        {
            char quote = '"';
            long inFlow = inFlow(tname, y1, y2);
            // double pct = inFlow / sum * 100;
            Util.out(String.format("%c%s%c %,d", quote, tname, quote, inFlow));
        }
    }
}
