package rtss.data.population.synthetic;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableDouble;

import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

public class PopulationADH
{
    static public boolean UsePrecomputedFiles = false; // ###
    static public boolean UseCache = true;

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

    public static Population getPopulation(Area area, int year) throws Exception
    {
        return getPopulation(area, "" + year);
    }

    public static Population getPopulation(Area area, String year) throws Exception
    {
        Population p = null;
        Integer yearHint = yearHint(year);

        String cacheKey = area.name() + "-" + year;
        if (UseCache)
        {
            p = cache.get(cacheKey);
            if (p != null)
                return p;
        }

        // try loading from resource
        if (UsePrecomputedFiles)
        {
            try
            {
                String en_year = year.replace("-границы-", "-in-borders-of-");
                String path = String.format("population_data/%s/ADH/%s/total.txt", area.name(), en_year);
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
            String m_title = "Population " + area.toString() + " " + Gender.MALE.toString() + " " + year;
            double[] m = PopulationFromExcel.loadCounts(excel_path, Gender.MALE, year, m_unknown, yearHint, m_title);
            /* population data in AHD books (and Excel file) is in thousands */
            m = Util.multiply(m, 1000);
            round(m);

            MutableDouble f_unknown = new MutableDouble();
            String f_title = "Population " + area.toString() + " " + Gender.FEMALE.toString() + " " + year;
            double[] f = PopulationFromExcel.loadCounts(excel_path, Gender.FEMALE, year, f_unknown, yearHint, f_title);
            f = Util.multiply(f, 1000);
            round(f);

            p = new Population(Locality.TOTAL,
                               m, m_unknown.doubleValue(), null,
                               f, f_unknown.doubleValue(), null);
        }

        if (UseCache)
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

    /* ========================================================================== */

    public static PopulationByLocality getPopulationByLocality(Area area, int year) throws Exception
    {
        return getPopulationByLocality(area, "" + year);
    }

    public static PopulationByLocality getPopulationByLocality(Area area, String year) throws Exception
    {
        return new PopulationByLocality(getPopulation(area, year));
    }
}
