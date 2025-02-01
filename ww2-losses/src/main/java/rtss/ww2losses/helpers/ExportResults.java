package rtss.ww2losses.helpers;

import java.io.File;

import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationContext;
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
            PopulationContext deficit1946_raw,
            PopulationContext deficit1946_adjusted) throws Exception
    {
        if (exportDirectory == null || exportDirectory.trim().length() == 0)
            return;

        File fdir = new File(exportDirectory);
        fdir.mkdirs();

        String fn;

        for (HalfYearEntry he : halves)
        {
            /* структура населения */
            fn = String.format("%s-population-%s.txt", ap.area.name(), he.index());
            Population p = he.actual_population.toPopulation();
            Util.writeAsFile(fpath(fdir, fn), p.dump(true));

            /* избыточные смерти */
            if (he.index().equals("1941.1") || he.index().equals("1946.1"))
            {
                // skip
            }
            else
            {
                fn = String.format("%s-excess-deaths-%s.txt", ap.area.name(), he.index());
                p = he.actual_excess_wartime_deaths.toPopulation();
                Util.writeAsFile(fpath(fdir, fn), p.dump(true));
            }
        }

        fn = String.format("%s-all-excess-deaths-by-age-at-time-of-death.txt", ap.area.name());
        Util.writeAsFile(fpath(fdir, fn), allExcessDeathsByDeathAge.toPopulation().dump(true));

        fn = String.format("%s-all-excess-deaths-by-age-at-1946.1.txt", ap.area.name());
        Util.writeAsFile(fpath(fdir, fn), allExcessDeathsByAgeAt1946.toPopulation().dump(true));

        if (deficit1946_adjusted != null)
        {
            fn = String.format("%s-deficit-1946.1-raw.txt", ap.area.name());
            Util.writeAsFile(fpath(fdir, fn), deficit1946_raw.toPopulation().dump(true));

            fn = String.format("%s-deficit-1946.1-adjusted.txt", ap.area.name());
            Util.writeAsFile(fpath(fdir, fn), deficit1946_adjusted.toPopulation().dump(true));
        }
        else
        {
            fn = String.format("%s-deficit-1946.txt", ap.area.name());
            Util.writeAsFile(fpath(fdir, fn), deficit1946_raw.toPopulation().dump(true));
        }

        Util.out("Файлы сохранены в директории " + exportDirectory);
    }

    private static String fpath(File fdir, String fn) throws Exception
    {
        File fp = new File(fdir, fn);
        return fp.getAbsoluteFile().getCanonicalPath();
    }
}
