package rtss.latinamerica.lambda;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.curves.InterpolatePopulationAsMeanPreservingCurve;
import rtss.data.curves.TargetResolution;
import rtss.data.curves.InterpolatePopulationAsMeanPreservingCurve.InterpolationOptionsByGender;
import rtss.data.population.struct.Population;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

public class LambdaPopulation
{
    public static List<Integer> countryPopulationYears(String cname) throws Exception
    {
        List<Integer> years = new ArrayList<>();

        String code = CountryName.e2code(cname);
        if (code == null && CountryName.r2e(cname) != null)
            code = CountryName.e2code(CountryName.r2e(cname));
        if (code == null)
            throw new IllegalArgumentException("Unknown country " + cname);

        String path = String.format("latinamerica/LAMBdA/population/%s_Popm.txt", code);
        String filedata = Util.loadResource(path);
        filedata = filedata.replace("\r", "");

        for (String line : filedata.split("\n"))
        {
            line = Util.despace(line);
            if (line.equals(""))
                continue;
            if (!line.startsWith("1") && !line.startsWith("2"))
                continue;
            String ys = line.split(" ")[0];
            int year = Integer.parseInt(ys);

            if (!years.contains(year))
                years.add(year);
        }

        Collections.sort(years);

        return years;
    }

    public static Population countryPopulation(String cname, int year) throws Exception
    {
        String code = CountryName.e2code(cname);
        if (code == null && CountryName.r2e(cname) != null)
            code = CountryName.e2code(CountryName.r2e(cname));
        if (code == null)
            throw new IllegalArgumentException("Unknown country " + cname);

        AtomicReference<Double> m_unknown = new AtomicReference<>(0.0);
        AtomicReference<Double> f_unknown = new AtomicReference<>(0.0);

        Bin[] mbins = loadBins(String.format("latinamerica/LAMBdA/population/%s_Popm.txt", code), year, m_unknown);
        Bin[] fbins = loadBins(String.format("latinamerica/LAMBdA/population/%s_Popf.txt", code), year, f_unknown);

        double[] m = bins2counts(mbins, cname, year, Gender.MALE);
        double[] f = bins2counts(fbins, cname, year, Gender.FEMALE);

        Population p = new Population(Locality.TOTAL,
                                      m, m_unknown.get().doubleValue(), null,
                                      f, f_unknown.get().doubleValue(), null);
        
        p.setTitle(String.format("%s %d", cname, year));
        
        return p;
    }

    private static Bin[] loadBins(String path, int year, AtomicReference<Double> unknown) throws Exception
    {
        List<Bin> list = new ArrayList<>();
        Bin bin = null;

        String filedata = Util.loadResource(path);
        filedata = filedata.replace("\r", "");

        for (String line : filedata.split("\n"))
        {
            line = Util.despace(line);
            if (line.equals(""))
                continue;
            if (!line.startsWith("1") && !line.startsWith("2"))
                continue;
            String[] tokens = line.split(" ");
            if (tokens.length != 3)
                throw new Exception("Invalid line " + line);

            int y = Integer.parseInt(tokens[0]);
            if (y != year)
                continue;

            int age = Integer.parseInt(tokens[1]);
            int people = Integer.parseInt(tokens[2]);

            /* skip "unknown" */
            if (age == 999)
            {
                unknown.set((double) people);
                continue;
            }

            if (bin != null)
            {
                bin.age_x2 = age - 1;
                bin.widths_in_years = bin.age_x2 - bin.age_x1 + 1;
            }

            bin = new Bin(age, age, people);
            list.add(bin);
        }

        if (bin == null)
            throw new Exception("No data for year " + year);

        bin.age_x2 = Population.MAX_AGE;
        bin.widths_in_years = bin.age_x2 - bin.age_x1 + 1;

        return Bins.bins(list);
    }

    private static double[] bins2counts(Bin[] bins, String cname, int year, Gender gender) throws Exception
    {
        bins = Bins.sum2avg(bins);
        if (Bins.firstBin(bins).age_x1 != 0 || Bins.lastBin(bins).age_x2 != Population.MAX_AGE)
            throw new Exception("Invalid population age range");

        String title = String.format("Population %s %d %s", cname, year, gender.name());

        InterpolationOptionsByGender options = new InterpolationOptionsByGender();
        options.male().usePrimaryCSASRA(true).usePrimarySPLINE(false).useSecondaryRefineYearlyAges(false);
        options.female().usePrimaryCSASRA(true).usePrimarySPLINE(false).useSecondaryRefineYearlyAges(false);

        double[] counts = bins2yearly(bins, title, null, gender, options);

        double sum1 = Util.sum(counts);
        double sum2 = Bins.sum(bins);

        if (Util.differ(sum1, sum2))
            throw new Exception("Curve count mismatches bin count");

        return counts;
    }

    private static double[] bins2yearly(Bin[] bins, String title, Integer yearHint, Gender gender, InterpolationOptionsByGender options)
            throws Exception
    {
        boolean interpolate = false;

        for (Bin bin : bins)
        {
            if (bin.widths_in_years != 1)
                interpolate = true;
        }

        if (!interpolate)
        {
            double[] v = new double[Population.MAX_AGE + 1];
            for (int k = 0; k < v.length; k++)
                v[k] = bins[k].avg;
            return v;
        }

        return InterpolatePopulationAsMeanPreservingCurve.curve(bins, title, TargetResolution.YEARLY, yearHint, gender, options.getForGender(gender));
    }

    public static void main(String[] args)
    {
        /*
         * Test
         */
        try
        {
            List<String> cnames = CountryName.rnames();
            List<Integer> years = countryPopulationYears("Гондурас");
            Population p = countryPopulation("Гондурас", 1940);
            Util.noop();
        }
        catch (Exception ex)
        {
            Util.err("** Exception");
            ex.printStackTrace();
        }
    }
}
