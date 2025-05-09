package rtss.data.population.synthetic;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableDouble;

import rtss.data.curves.InterpolatePopulationAsMeanPreservingCurve.InterpolationOptionsByGender;
import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

public class PopulationADH
{
    static private boolean UsePrecomputedFiles = Util.True;
    static private boolean UseCache = Util.True;
    static private String FilesVersion = "ADH";

    private static Map<String, Population> cache = new HashMap<>();

    private static int[] ageBinWidths = { 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 16 };

    public static int[] AgeBinWidthsYears()
    {
        return Util.dup(ageBinWidths);
    }

    public static int[] AgeBinWidthsDays()
    {
        return Util.multiply(ageBinWidths, 365);
    }

    public static void setFilesVersion(String filesVersion)
    {
        FilesVersion = filesVersion;
    }

    public static Population getPopulation(Area area, int year) throws Exception
    {
        return getPopulation(area, year, null);
    }

    public static Population getPopulation(Area area, int year, InterpolationOptionsByGender options) throws Exception
    {
        return getPopulation(area, "" + year, options);
    }

    public static Population getPopulation(Area area, String year) throws Exception
    {
        return getPopulation(area, year, null);
    }

    public static Population getPopulation(Area area, String year, InterpolationOptionsByGender options) throws Exception
    {
        Population p = null;
        Integer yearHint = yearHint(year);

        String cacheKey = FilesVersion + "---" + area.name() + "---" + year;

        if (UseCache && mayUseCache(options))
        {
            p = cache.get(cacheKey);
            if (p != null)
                return p;
        }

        // try loading from resource
        if (UsePrecomputedFiles && mayUseCache(options))
        {
            try
            {
                String en_year = year.replace("-границы-", "-in-borders-of-");
                String path = String.format("population_data/%s/%s/%s/total.txt", area.name(), FilesVersion, en_year);
                p = Population.load(path, Locality.TOTAL);
            }
            catch (Exception ex)
            {
                // ignore
                Util.noop();
            }
        }

        if (p == null)
        {
            String excel_path = String.format("population_data/%s/%s-population-ADH.xlsx", area.name(), area.name());

            MutableDouble m_unknown = new MutableDouble();
            String m_title = area.toString() + " " + Gender.MALE.toShortString() + " " + year;
            double[] m = PopulationFromExcel.loadCounts(excel_path, Gender.MALE, year, m_unknown, yearHint, m_title, options);
            /* population data in AHD books (and Excel file) is in thousands */
            m = Util.multiply(m, 1000);
            round(m);

            MutableDouble f_unknown = new MutableDouble();
            String f_title = area.toString() + " " + Gender.FEMALE.toShortString() + " " + year;
            double[] f = PopulationFromExcel.loadCounts(excel_path, Gender.FEMALE, year, f_unknown, yearHint, f_title, options);
            f = Util.multiply(f, 1000);
            round(f);

            p = new Population(Locality.TOTAL,
                               m, m_unknown.doubleValue(), null,
                               f, f_unknown.doubleValue(), null);
        }

        if (UseCache && mayUseCache(options))
        {
            p.seal();
            cache.put(cacheKey, p);
        }

        return p;
    }

    private static void round(double[] x)
    {
        for (int age = 0; age < x.length; age++)
            x[age] = Math.round(x[age]);
    }

    private static Integer yearHint(String year)
    {
        if (year.contains("-"))
        {
            String[] sa = year.split("-");
            year = sa[0];
        }

        return Integer.parseInt(year);
    }

    private static boolean mayUseCache(InterpolationOptionsByGender options)
    {
        return options == null || options.allowCache() == true;
    }

    /* ========================================================================== */

    public static PopulationByLocality getPopulationByLocality(Area area, int year, InterpolationOptionsByGender options) throws Exception
    {
        return getPopulationByLocality(area, "" + year, options);
    }

    public static PopulationByLocality getPopulationByLocality(Area area, String year, InterpolationOptionsByGender options) throws Exception
    {
        return new PopulationByLocality(getPopulation(area, year, options));
    }

    public static PopulationByLocality getPopulationByLocality(Area area, int year) throws Exception
    {
        return getPopulationByLocality(area, year, null);
    }

    public static PopulationByLocality getPopulationByLocality(Area area, String year) throws Exception
    {
        return getPopulationByLocality(area, year, null);
    }
}
