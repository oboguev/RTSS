package rtss.ww2losses.helpers;

import java.io.File;

import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Area;
import rtss.util.Util;
import rtss.util.plot.ChartXY;
import rtss.util.plot.PopulationChart;
import rtss.ww2losses.params.AreaParameters;
import rtss.ww2losses.struct.HalfYearEntries;
import rtss.ww2losses.struct.HalfYearEntry;

public class ExportResults
{
    /*
     * Разрешение для сохраняемых графиков
     */
    public static final int IMAGE_CX = 1000;
    public static final int IMAGE_CY = 1500;

    public static final int TN_CX = 600;
    public static final int TN_CY = 900;

    /* ============================================================================================= */

    public static void exportResults(
            String exportDirectory,
            AreaParameters ap,
            HalfYearEntries<HalfYearEntry> halves,

            PopulationContext allExcessDeathsByDeathAge,
            PopulationContext allExcessDeathsByAgeAt1946,

            PopulationContext deficit1946_preimmigration,
            PopulationContext deficit1946_postimmigration) throws Exception
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
            title = "Население " + areaname + " в начале полугодия " + he.id();
            fn = String.format("%s-population-%s.txt", ap.area.name(), he.id());
            Population p = he.actual_population.toPopulation();
            Util.writeAsFile(fpath(fdir, fn), dump(p, title));

            /* избыточные смерти */
            if (he.id().equals("1941.1") || he.id().equals("1946.1"))
            {
                // skip
            }
            else
            {
                p = he.actual_excess_wartime_deaths.toPopulation();
                title = "Избыточные смерти населения " + areaname + " в полугодии " + he.id();
                Util.writeAsFile(fpath(fdir, ap, "excess-deaths-" + he.id()), dump(p, title));
            }

            /* иммиграция в РСФСР */
            if (ap.area == Area.RSFSR)
            {
                if (!he.immigration.isEmpty())
                {
                    p = he.immigration.toPopulation();
                    title = "Минимальная межреспубликанская миграция в РСФСР в полугодии " + he.id();
                    Util.writeAsFile(fpath(fdir, ap, "immigration-" + he.id()), dump(p, title));
                }
            }
        }

        /* ========================================================================================================= */

        title = "Избыточные смерти населения " + areaname + " в 1941-1945 гг. по возрасту в момент смерти";
        Util.writeAsFile(fpath(fdir, ap, "all-excess-deaths-by-age-at-time-of-death"), dump(allExcessDeathsByDeathAge, title));
        exportImage(title, allExcessDeathsByDeathAge, ap, exportDirectory, "all-excess-deaths-by-age-at-time-of-death");

        title = "Избыточные смерти населения " + areaname + " в 1941-1945 гг. по возрасту, в котором умерший был бы на начало 1946 года";
        Util.writeAsFile(fpath(fdir, ap, "all-excess-deaths-by-age-at-1946.1"), dump(allExcessDeathsByAgeAt1946, title));
        exportImage(title, allExcessDeathsByAgeAt1946, ap, exportDirectory, "all-excess-deaths-by-age-at-1946.1");

        /* ========================================================================================================= */

        if (deficit1946_preimmigration == null)
        {
            // СССР, только "с иммиграцией" (нулевой)
            title = "Дефицит населения " + areaname + " на начало 1946 года";
            Util.writeAsFile(fpath(fdir, ap, "deficit-1946.1"), dump(deficit1946_postimmigration, title));
        }
        else
        {
            // РСФСР, до и после поправок не иммиграцию
            title = "Дефицит населения " + areaname + " на начало 1946 года, без учёта иммиграции";
            Util.writeAsFile(fpath(fdir, ap, "deficit-1946-without-immigration"), dump(deficit1946_preimmigration, title));

            title = "Дефицит населения " + areaname + " на начало 1946 года, с учётом иммиграции";
            fn = String.format("%s-deficit-1946-with-immigration.txt", ap.area.name());
            Util.writeAsFile(fpath(fdir, fn), dump(deficit1946_postimmigration, title));
        }

        Util.out("");
        Util.out("Файлы сохранены в директории " + exportDirectory);
    }

    /* ============================================================================================= */

    public static void exportBirths(
            String exportDirectory,
            double[] ussr,
            double[] rsfsr) throws Exception
    {
        final int cx = 900;
        final int cy = 600;        
        
        ChartXY chart = new ChartXY("Число рождений в населениях СССР и РСФСР с начала 1941 по конец 1945 года, по дням");
        chart.addSeries("СССР", ussr);
        chart.addSeries("РСФСР", rsfsr);
        chart.defaultShapesVisible(false);
        chart.exportImage(cx, cy, imageFilename(exportDirectory, "USSR-RFSFR-births" + ".png"));
        // chart.display();
    }

    /* ============================================================================================= */

    public static void exportImage(String title, PopulationContext p, AreaParameters ap, String exportDirectory, String suffix) throws Exception
    {
        if (exportDirectory == null || exportDirectory.trim().length() == 0)
            return;

        title = "";

        new PopulationChart(title)
                .show("", p)
                .exportImage(IMAGE_CX, IMAGE_CY, imageFilename(exportDirectory, ap, suffix + ".png"))
                .exportImage(TN_CX, TN_CY, imageTnFilename(exportDirectory, ap, suffix + ".png"));
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

    public static String imageTnFilename(String exportDirectory, AreaParameters ap, String suffix) throws Exception
    {
        if (exportDirectory == null || exportDirectory.trim().length() == 0)
            return null;

        File fdir = new File(exportDirectory);
        fdir.mkdirs();

        fdir = new File(fdir, "images-tn");
        fdir.mkdirs();

        String fn = ap.area.name() + "-" + suffix;

        return fpath(fdir, fn);
    }

    public static String imageFilename(String exportDirectory, String suffix) throws Exception
    {
        if (exportDirectory == null || exportDirectory.trim().length() == 0)
            return null;

        File fdir = new File(exportDirectory);
        fdir.mkdirs();

        fdir = new File(fdir, "images");
        fdir.mkdirs();

        String fn = suffix;

        return fpath(fdir, fn);
    }

    /* ============================================================================================= */

    private static String fpath(File fdir, String fn) throws Exception
    {
        File fp = new File(fdir, fn);
        return fp.getAbsoluteFile().getCanonicalPath();
    }

    private static String fpath(File fdir, AreaParameters ap, String fn) throws Exception
    {
        fn = String.format("%s-%s.txt", ap.area.name(), fn);
        return fpath(fdir, fn);
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
