package rtss.ww2losses.helpers;

import java.io.File;

import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Area;
import rtss.util.Util;
import rtss.util.plot.PopulationChart;
import rtss.ww2losses.HalfYearEntries;
import rtss.ww2losses.HalfYearEntry;
import rtss.ww2losses.params.AreaParameters;

public class ExportResults
{
    /*
     * Разрешение для сохраняемых графиков
     */
    public static final int IMAGE_CX = 750;
    public static final int IMAGE_CY = 1500;

    public static final int TN_CX = 450;
    public static final int TN_CY = 900;

    /* ============================================================================================= */

    public static void exportResults(
            String exportDirectory,
            AreaParameters ap,
            HalfYearEntries<HalfYearEntry> halves,

            PopulationContext allExcessDeathsByDeathAge,
            PopulationContext allExcessDeathsByAgeAt1946,

            PopulationContext deficit1946_raw_preimmigration,
            PopulationContext deficit1946_adjusted_preimmigration,
            PopulationContext deficit1946_raw_postimmigration,
            PopulationContext deficit1946_adjusted_postimmigration) throws Exception
    {
        if (exportDirectory == null || exportDirectory.trim().length() == 0)
            return;

        Area area = ap.area;
        String areaname = area.toString();
        String fn, title;

        File fdir = new File(exportDirectory);
        fdir.mkdirs();

        /* ========================================================================================================= */

        for (HalfYearEntry he : halves)
        {
            /* структура населения */
            title = "Население " + areaname + " в начале полугодия " + he.index();
            fn = String.format("%s-population-%s.txt", ap.area.name(), he.index());
            Population p = he.actual_population.toPopulation();
            Util.writeAsFile(fpath(fdir, fn), dump(p, title));

            /* избыточные смерти */
            if (he.index().equals("1941.1") || he.index().equals("1946.1"))
            {
                // skip
            }
            else
            {
                title = "Избыточные смерти населения " + areaname + " в полугодии " + he.index();
                fn = String.format("%s-excess-deaths-%s.txt", ap.area.name(), he.index());
                p = he.actual_excess_wartime_deaths.toPopulation();
                Util.writeAsFile(fpath(fdir, fn), dump(p, title));
            }

            /* иммиграция в РСФСР */
            if (ap.area == Area.RSFSR)
            {
                if (!he.immigration.isEmpty())
                {
                    title = "Минимальная межреспубликанская миграция в РСФСР в полугодии " + he.index();
                    fn = String.format("%s-immigration-%s.txt", ap.area.name(), he.index());
                    p = he.immigration.toPopulation();
                    Util.writeAsFile(fpath(fdir, fn), dump(p, title));
                }
            }
        }
        
        /* ========================================================================================================= */

        title = "Избыточные смерти населения " + areaname + " в 1941-1945 гг. по возрасту в момент смерти";
        fn = String.format("%s-all-excess-deaths-by-age-at-time-of-death.txt", ap.area.name());
        Util.writeAsFile(fpath(fdir, fn), dump(allExcessDeathsByDeathAge, title));
        exportImage(title, allExcessDeathsByDeathAge, ap, exportDirectory, "all-excess-deaths-by-age-at-time-of-death");

        title = "Избыточные смерти населения " + areaname + " в 1941-1945 гг. по возрасту, в котором умерший был бы на начало 1946 года";
        fn = String.format("%s-all-excess-deaths-by-age-at-1946.1.txt", ap.area.name());
        Util.writeAsFile(fpath(fdir, fn), dump(allExcessDeathsByAgeAt1946, title));
        exportImage(title, allExcessDeathsByAgeAt1946, ap, exportDirectory, "all-excess-deaths-by-age-at-1946.1");

        /* ========================================================================================================= */

        if (deficit1946_raw_preimmigration == null && deficit1946_adjusted_preimmigration == null)
        {
            // СССР, только "с иммиграцией" (нулевой)

            if (deficit1946_adjusted_postimmigration != null)
            {
                title = "Дефицит населения " + areaname + " на начало 1946 года (неисправленный)";
                fn = String.format("%s-deficit-1946.1-raw.txt", ap.area.name());
                Util.writeAsFile(fpath(fdir, fn), dump(deficit1946_raw_postimmigration, title));

                title = "Дефицит населения " + areaname + " на начало 1946 года (исправленный)";
                fn = String.format("%s-deficit-1946.1-adjusted.txt", ap.area.name());
                Util.writeAsFile(fpath(fdir, fn), dump(deficit1946_adjusted_postimmigration, title));
            }
            else
            {
                title = "Дефицит населения " + areaname + " на начало 1946 года";
                fn = String.format("%s-deficit-1946.txt", ap.area.name());
                Util.writeAsFile(fpath(fdir, fn), dump(deficit1946_raw_postimmigration, title));
            }
        }
        else
        {
            // РСФСР, до и после поправок не иммиграцию

            if (deficit1946_adjusted_preimmigration != null)
            {
                title = "Дефицит населения " + areaname + " на начало 1946 года (неисправленный), без учёта иммиграции";
                fn = String.format("%s-deficit-1946.1-raw-without-immigration.txt", ap.area.name());
                Util.writeAsFile(fpath(fdir, fn), dump(deficit1946_raw_preimmigration, title));

                title = "Дефицит населения " + areaname + " на начало 1946 года (исправленный), без учёта иммиграции";
                fn = String.format("%s-deficit-1946.1-adjusted-without-immigration.txt", ap.area.name());
                Util.writeAsFile(fpath(fdir, fn), dump(deficit1946_adjusted_preimmigration, title));
            }
            else
            {
                title = "Дефицит населения " + areaname + " на начало 1946 года, без учёта иммиграции";
                fn = String.format("%s-deficit-1946-without-immigration.txt", ap.area.name());
                Util.writeAsFile(fpath(fdir, fn), dump(deficit1946_raw_preimmigration, title));
            }

            if (deficit1946_adjusted_postimmigration != null)
            {
                title = "Дефицит населения " + areaname + " на начало 1946 года (неисправленный), с учётом иммиграции";
                fn = String.format("%s-deficit-1946.1-raw-with-immigration.txt", ap.area.name());
                Util.writeAsFile(fpath(fdir, fn), dump(deficit1946_raw_postimmigration, title));

                title = "Дефицит населения " + areaname + " на начало 1946 года (исправленный), с учётом иммиграции";
                fn = String.format("%s-deficit-1946.1-adjusted-with-immigration.txt", ap.area.name());
                Util.writeAsFile(fpath(fdir, fn), dump(deficit1946_adjusted_postimmigration, title));
            }
            else
            {
                title = "Дефицит населения " + areaname + " на начало 1946 года, с учётом иммиграции";
                fn = String.format("%s-deficit-1946-with-immigration.txt", ap.area.name());
                Util.writeAsFile(fpath(fdir, fn), dump(deficit1946_raw_postimmigration, title));
            }
        }

        Util.out("");
        Util.out("Файлы сохранены в директории " + exportDirectory);
    }

    /* ============================================================================================= */

    public static void exportImage(String title, PopulationContext p, AreaParameters ap, String exportDirectory, String suffix) throws Exception
    {
        if (exportDirectory == null || exportDirectory.trim().length() == 0)
            return;

        title = "";

        new PopulationChart(title)
                .show("", p.toPopulation())
                .exportImage(IMAGE_CX, IMAGE_CY, imageFilename(exportDirectory, ap, suffix + ".png"))
                .exportImage(TN_CX, TN_CY, imageFilename(exportDirectory, ap, suffix + "-tn.png"));
    }

    public static String imageFilename(String exportDirectory, AreaParameters ap, String suffix) throws Exception
    {
        if (exportDirectory == null || exportDirectory.trim().length() == 0)
            return null;

        File fdir = new File(exportDirectory);
        fdir.mkdirs();

        fdir = new File(fdir, "images");
        fdir.mkdirs();

        String fn = ap.area.name() + "-" + suffix;

        return fpath(fdir, fn);
    }

    /* ============================================================================================= */

    private static String fpath(File fdir, String fn) throws Exception
    {
        File fp = new File(fdir, fn);
        return fp.getAbsoluteFile().getCanonicalPath();
    }

    private static String dump(PopulationContext p, String title) throws Exception
    {
        return dump(p.toPopulation(), title);
    }

    private static String dump(Population p, String title) throws Exception
    {
        StringBuilder sb = new StringBuilder();

        if (title != null && title.trim().length() != 0)
        {
            for (String line : title.split("\n"))
                sb.append("# " + line + Util.nl);
            sb.append(Util.nl);
        }

        sb.append(p.dump(true));

        return sb.toString();
    }
}
