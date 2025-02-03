package rtss.ww2losses.helpers;

import java.io.File;

import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Area;
import rtss.util.Util;
import rtss.ww2losses.HalfYearEntries;
import rtss.ww2losses.HalfYearEntry;
import rtss.ww2losses.params.AreaParameters;

public class ExportResults
{
    public static void exportResults(
            String exportDirectory,
            AreaParameters ap,
            HalfYearEntries<HalfYearEntry> halves,
            
            PopulationContext allExcessDeathsByDeathAge,
            PopulationContext allExcessDeathsByAgeAt1946,

            PopulationContext deficit1946_raw_preimmigration,
            PopulationContext deficit1946_adjusted_preimmigration,
            PopulationContext deficit1946_raw_postimmigration,
            PopulationContext deficit1946_adjusted_postimmigration

    ) throws Exception
    {
        if (exportDirectory == null || exportDirectory.trim().length() == 0)
            return;

        File fdir = new File(exportDirectory);
        fdir.mkdirs();

        String areaname = ap.area.toString();

        String fn;

        for (HalfYearEntry he : halves)
        {
            /* структура населения */
            fn = String.format("%s-population-%s.txt", ap.area.name(), he.index());
            Population p = he.actual_population.toPopulation();
            Util.writeAsFile(fpath(fdir, fn),
                             dump(p, "Население " + areaname + " в начале полугодия " + he.index()));

            /* избыточные смерти */
            if (he.index().equals("1941.1") || he.index().equals("1946.1"))
            {
                // skip
            }
            else
            {
                fn = String.format("%s-excess-deaths-%s.txt", ap.area.name(), he.index());
                p = he.actual_excess_wartime_deaths.toPopulation();
                Util.writeAsFile(fpath(fdir, fn),
                                 dump(p, "Избыточные смерти населения " + areaname + " в полугодии " + he.index()));
            }

            /* иммиграция в РСФСР */
            if (ap.area == Area.RSFSR)
            {
                fn = String.format("%s-immigration-%s.txt", ap.area.name(), he.index());
                p = he.immigration.toPopulation();
                Util.writeAsFile(fpath(fdir, fn),
                                 dump(p, "Минимальная межреспубликанская миграция в РСФСР в полугодии " + he.index()));
            }
        }

        fn = String.format("%s-all-excess-deaths-by-age-at-time-of-death.txt", ap.area.name());
        Util.writeAsFile(fpath(fdir, fn),
                         dump(allExcessDeathsByDeathAge, "Избыточные смерти населения " + areaname + " в 1941-1945 гг. по возрасту в момент смерти"));

        fn = String.format("%s-all-excess-deaths-by-age-at-1946.1.txt", ap.area.name());
        Util.writeAsFile(fpath(fdir, fn),
                         dump(allExcessDeathsByAgeAt1946, "Избыточные смерти населения " + areaname
                                                          + " в 1941-1945 гг. по возрасту, в котором умерший был бы на начало 1946 года"));


        if (deficit1946_raw_preimmigration == null && deficit1946_adjusted_preimmigration == null)
        {
            // СССР, только "с иммиграцией" (нулевой)

            if (deficit1946_adjusted_postimmigration != null)
            {
                fn = String.format("%s-deficit-1946.1-raw.txt", ap.area.name());
                Util.writeAsFile(fpath(fdir, fn),
                                 dump(deficit1946_raw_postimmigration, "Дефицит населения " + areaname + " на начало 1946 года (неисправленный)"));

                fn = String.format("%s-deficit-1946.1-adjusted.txt", ap.area.name());
                Util.writeAsFile(fpath(fdir, fn),
                                 dump(deficit1946_adjusted_postimmigration, "Дефицит населения " + areaname + " на начало 1946 года (исправленный)"));
            }
            else
            {
                fn = String.format("%s-deficit-1946.txt", ap.area.name());
                Util.writeAsFile(fpath(fdir, fn),
                                 dump(deficit1946_raw_postimmigration, "Дефицит населения " + areaname + " на начало 1946 года"));
            }
        }
        else
        {
            // РСФСР, до и после поправок не иммиграцию
            
            if (deficit1946_adjusted_preimmigration != null)
            {
                fn = String.format("%s-deficit-1946.1-raw-without-immigration.txt", ap.area.name());
                Util.writeAsFile(fpath(fdir, fn),
                                 dump(deficit1946_raw_preimmigration, "Дефицит населения " + areaname + " на начало 1946 года (неисправленный), без учёта иммиграции"));

                fn = String.format("%s-deficit-1946.1-adjusted-without-immigration.txt", ap.area.name());
                Util.writeAsFile(fpath(fdir, fn),
                                 dump(deficit1946_adjusted_preimmigration, "Дефицит населения " + areaname + " на начало 1946 года (исправленный), без учёта иммиграции"));
            }
            else
            {
                fn = String.format("%s-deficit-1946-without-immigration.txt", ap.area.name());
                Util.writeAsFile(fpath(fdir, fn),
                                 dump(deficit1946_raw_preimmigration, "Дефицит населения " + areaname + " на начало 1946 года, без учёта иммиграции"));
            }
            
            if (deficit1946_adjusted_postimmigration != null)
            {
                fn = String.format("%s-deficit-1946.1-raw-with-immigration.txt", ap.area.name());
                Util.writeAsFile(fpath(fdir, fn),
                                 dump(deficit1946_raw_postimmigration, "Дефицит населения " + areaname + " на начало 1946 года (неисправленный), с учётом иммиграции"));

                fn = String.format("%s-deficit-1946.1-adjusted-with-immigration.txt", ap.area.name());
                Util.writeAsFile(fpath(fdir, fn),
                                 dump(deficit1946_adjusted_postimmigration, "Дефицит населения " + areaname + " на начало 1946 года (исправленный), с учётом иммиграции"));
            }
            else
            {
                fn = String.format("%s-deficit-1946-with-immigration.txt", ap.area.name());
                Util.writeAsFile(fpath(fdir, fn),
                                 dump(deficit1946_raw_postimmigration, "Дефицит населения " + areaname + " на начало 1946 года, с учётом иммиграции"));
            }
        }

        Util.out("");
        Util.out("Файлы сохранены в директории " + exportDirectory);
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
