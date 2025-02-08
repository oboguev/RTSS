package rtss.data.bin;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.opencsv.CSVReader;

import rtss.util.Util;
import rtss.util.XY;

public class Bins
{
    public static Bin[] bins(Bin... bins) throws Exception
    {
        Bin prev = null;
        int index = 0;

        for (Bin bin : bins)
        {
            bin.index = index++;

            bin.prev = prev;
            if (prev != null)
                prev.next = bin;
            prev = bin;

            if (bin.widths_in_years < 1)
                throw new Exception("Invalid bin width");

            if (bin.next != null && bin.age_x2 + 1 != bin.next.age_x1)
                throw new Exception("Bins not continuous");
        }

        return bins;
    }
    
    public static Bin[] bins(List<Bin> bins) throws Exception
    {
        return bins(bins.toArray(new Bin[0]));
    }

    public static Bin[] clone(Bin... bins) throws Exception
    {
        List<Bin> list = new ArrayList<>();
        for (Bin bin : bins)
            list.add(new Bin(bin));
        return Bins.bins(list);
    }

    public static Bin firstBin(Bin... bins)
    {
        return bins[0];
    }

    public static Bin lastBin(Bin... bins)
    {
        return bins[bins.length - 1];
    }

    public static int widths_in_years(Bin... bins)
    {
        int w = 0;
        for (Bin bin : bins)
            w += bin.widths_in_years;
        return w;
    }

    public static int[] start_x(Bin... bins)
    {
        int[] x = new int[bins.length];
        int k = 0;
        for (Bin bin : bins)
            x[k++] = bin.age_x1;
        return x;
    }

    public static double[] midpoint_x(Bin... bins)
    {
        double[] x = new double[bins.length];
        int k = 0;
        for (Bin bin : bins)
            x[k++] = bin.mid_x;
        return x;
    }

    public static double[] midpoint_y(Bin... bins)
    {
        double[] y = new double[bins.length];
        int k = 0;
        for (Bin bin : bins)
            y[k++] = bin.avg;
        return y;
    }
    
    public static int[] widths(Bin...bins)
    {
        return widths(1, bins);
    }
    
    public static int[] widths(int ppy, Bin...bins)
    {
        int[] w = new int[bins.length];
        int k = 0;
        for (Bin bin : bins)
            w[k++] = bin.widths_in_years * ppy;
        return w;
    }

    public static Bin[] multiply(Bin[] bins, double f) throws Exception
    {
        Bin[] xbins = Bins.clone(bins);
        for (Bin bin : xbins)
            bin.avg *= f;
        return xbins;
    }

    public static Bin[] yearlyBins(Bin[] bins) throws Exception
    {
        List<Bin> list = new ArrayList<Bin>();
        for (Bin bin : bins)
        {
            for (int x = bin.age_x1; x <= bin.age_x2; x++)
                list.add(new Bin(x, x, bin.avg));
        }
        return Bins.bins(list);
    }

    public static Bin[] combine_equals(Bin... bins) throws Exception
    {
        ArrayList<Bin> ar = new ArrayList<>();

        Bin prev = null;
        for (Bin bin : bins)
        {
            // clone the bin
            bin = new Bin(bin);

            if (prev == null || prev.avg != bin.avg)
            {
                ar.add(bin);
                prev = bin;
            }
            else
            {
                prev.age_x2 = bin.age_x2;
                prev.widths_in_years = prev.age_x2 - prev.age_x1 + 1;
            }
        }

        bins = ar.toArray(new Bin[0]);

        /*
         * recalculate links and indexes
         */
        return bins(bins);
    }

    /*
     * Get the number of inner minimum and inner maximum points for the curve,
     * not including endpoints  
     */
    public static int flips(Bin... bins) throws Exception
    {
        int flips = 0;

        for (Bin bin : combine_equals(bins))
        {
            if (bin.prev != null && bin.next != null)
            {
                if (bin.avg < bin.prev.avg && bin.avg < bin.next.avg)
                    flips++;
                if (bin.avg > bin.prev.avg && bin.avg > bin.next.avg)
                    flips++;
            }
        }

        return flips;
    }

    /*
     * Identify the bin with minimum avg value 
     */
    public static Bin findMinBin(Bin... bins) throws Exception
    {
        Bin minBin = null;

        for (Bin bin : bins)
        {
            if (minBin == null)
            {
                minBin = bin;
            }
            else if (bin.avg <= minBin.avg)
            {
                minBin = bin;
            }
        }

        return minBin;
    }

    /*
     * Identify the bin with maximum avg value 
     */
    public static Bin findMaxBin(Bin... bins) throws Exception
    {
        Bin maxBin = null;

        for (Bin bin : bins)
        {
            if (maxBin == null)
            {
                maxBin = bin;
            }
            else if (bin.avg >= maxBin.avg)
            {
                maxBin = bin;
            }
        }

        return maxBin;
    }

    /*
     * Find bin for year of age 
     */
    public static Bin binForAge(int age, Bin... bins)
    {
        for (Bin bin : bins)
        {
            if (age >= bin.age_x1 && age <= bin.age_x2)
                return bin;
        }

        return null;
    }

    /*
     * sum of values in all bins
     */
    public static double sum(Bin... bins)
    {
        double sum = 0;
        for (Bin bin : bins)
            sum += bin.avg * bin.widths_in_years;
        return sum;
    }

    /*
     * convert bin set with values (@avg) actually meaning a sum for the bin,
     * into the bin set with values (@avg) meaning a mean for the bin 
     */
    public static Bin[] sum2avg(Bin... bins) throws Exception
    {
        Bin[] r = new Bin[bins.length];
        for (int k = 0; k < bins.length; k++)
        {
            r[k] = new Bin(bins[k]);
            r[k].avg /= r[k].widths_in_years;
        }

        return bins(r);
    }

    /*
     * convert bin set with values (@avg) actually meaning a mean for the bin,
     * into the bin set with values (@avg) meaning a sum for the bin 
     */
    public static Bin[] avg2sum(Bin... bins) throws Exception
    {
        Bin[] r = new Bin[bins.length];
        for (int k = 0; k < bins.length; k++)
        {
            r[k] = new Bin(bins[k]);
            r[k].avg *= r[k].widths_in_years;
        }

        return bins(r);
    }

    /*
     * Get yearly averages for the sequence of bins
     */
    public static double[] bins2yearly(Bin... bins)
    {
        double[] yy = new double[Bins.widths_in_years(bins)];
        int ix = 0;

        for (Bin bin : bins)
        {
            for (int k = bin.age_x1; k <= bin.age_x2; k++)
            {
                yy[ix++] = bin.avg;
            }
        }

        return yy;
    }

    /*
     * Generate X-points at PPY per year, evenly distributed across the bins interval
     */
    public static double[] ppy_x(Bin[] bins, int ppy)
    {
        double x1 = Bins.firstBin(bins).age_x1;
        double x2 = Bins.lastBin(bins).age_x2 + 1;

        // will compute interpolation for these points
        int xcount = Bins.widths_in_years(bins) * ppy;
        double[] xx = new double[xcount];
        for (int k = 0; k < xcount; k++)
            xx[k] = x1 + k * (x2 - x1) / xcount;

        return xx;
    }

    /*
     * Generate Y-points at PPY per year
     */
    public static double[] ppy_y(Bin[] bins, int ppy)
    {
        int xcount = Bins.widths_in_years(bins) * ppy;
        double[] yy = new double[xcount];
        int ix = 0;

        for (Bin bin : bins)
        {
            for (int k = 0; k < bin.widths_in_years * ppy; k++)
                yy[ix++] = bin.avg;
        }

        return yy;
    }

    /*
     * Convert an array of value points with PPY points for each year to yearly averages
     */
    public static double[] ppy2yearly(double[] y, int ppy)
    {
        double[] yy = new double[y.length / ppy];
        Arrays.fill(yy, 0);

        for (int ix = 0; ix < y.length; ix++)
            yy[ix / ppy] += y[ix];

        for (int yx = 0; yx < yy.length; yx++)
            yy[yx] /= ppy;

        return yy;
    }

    /*
     * Convert an array of yearly value points to an array of averages according to bin sizes
     */
    public static double[] yearly2bin_avg(Bin[] bins, double[] yearly)
    {
        double[] avg = new double[bins.length];
        int yx = 0;
        for (Bin bin : bins)
        {
            for (int year = bin.age_x1; year <= bin.age_x2; year++)
            {
                avg[bin.index] += yearly[yx++];
            }

            avg[bin.index] /= bin.widths_in_years;
        }

        return avg;
    }

    /*
     * Extract a subrange [start ... end] 
     */
    public static Bin[] subset(Bin start, Bin end) throws Exception
    {
        List<Bin> list = new ArrayList<>();

        for (Bin bin = start;; bin = bin.next)
        {
            if (bin == null || end == null)
                throw new IllegalArgumentException("end bin is not reachable from the start bin");
            list.add(new Bin(bin));
            if (bin == end)
                break;
        }

        return Bins.bins(list);
    }

    public static XY<double[]> cumulativePoints(Bin[] bins)
    {
        double[] x = new double[bins.length + 1];
        double[] y = new double[bins.length + 1];

        x[0] = bins[0].age_x1;
        y[0] = 0;
        int k = 1;
        for (Bin bin : bins)
        {
            x[k] = bin.age_x2 + 1;
            y[k] = y[k - 1] + bin.avg * bin.widths_in_years;
            k++;
        }

        return XY.of(x, y);
    }

    /*
     * Parse text file with bin data.
     * Each line has format "year value".
     */
    public static Bin[] loadBinsYearly(String path, String sep) throws Exception
    {
        return parseBinsYearly(Util.loadResource(path), sep);
    }

    public static Bin[] parseBinsYearly(String text, String sep) throws Exception
    {
        text = cleanDataContent(text, sep).replace(" ", ",");
        List<String[]> list;
        try (CSVReader reader = new CSVReader(new StringReader(text)))
        {
            list = reader.readAll();
        }

        List<Bin> binlist = new ArrayList<>();

        for (String[] sa : list)
        {
            int age = Integer.parseInt(sa[0]);
            double avg = Double.parseDouble(sa[1]);

            Bin bin = new Bin(age, age, avg);
            binlist.add(bin);
        }

        return bins(binlist);
    }

    /*
     * Parse text file with bin data.
     * Each line has format "year-first year-last value".
     * E.g. 
     *   10 14 ...
     *   15 19 ...
     *   20 24 ...
     */
    public static Bin[] loadBinsMultiYear(String path, String sep) throws Exception
    {
        return parseBinsYearly(Util.loadResource(path), sep);
    }

    public static Bin[] parseBinsMultiYear(String text, String sep) throws Exception
    {
        List<String[]> list;
        try (CSVReader reader = new CSVReader(new StringReader(text)))
        {
            list = reader.readAll();
        }

        List<Bin> binlist = new ArrayList<>();

        for (String[] sa : list)
        {
            int age1 = Integer.parseInt(sa[0]);
            int age2 = Integer.parseInt(sa[1]);
            double avg = Double.parseDouble(sa[2]);

            Bin bin = new Bin(age1, age2, avg);
            binlist.add(bin);
        }

        return bins(binlist);
    }

    /*
     * Parse text file with bin data.
     * Each line has format "year-first year-past-last value".
     * E.g. 
     *   10 15 ...
     *   15 20 ...
     *   20 25 ...
     */
    public static Bin[] loadBinsMultiYearPast(String path, String sep) throws Exception
    {
        return parseBinsMultiYearPast(Util.loadResource(path), sep);
    }

    public static Bin[] parseBinsMultiYearPast(String text, String sep) throws Exception
    {
        List<String[]> list;
        try (CSVReader reader = new CSVReader(new StringReader(text)))
        {
            list = reader.readAll();
        }

        List<Bin> binlist = new ArrayList<>();

        for (String[] sa : list)
        {
            int age1 = Integer.parseInt(sa[0]);
            int age2 = Integer.parseInt(sa[1]);
            double avg = Double.parseDouble(sa[2]);

            Bin bin = new Bin(age1, age2 - 1, avg);
            binlist.add(bin);
        }

        return bins(binlist);
    }

    private static String cleanDataContent(String text, String sep)
    {
        text = text.replace("\r\n", "\n");
        text = text.replace("\t", " ").replaceAll(" +", " ");
        text = Util.removeComments(text);
        text = Util.trimLines(text);
        text = Util.removeEmptyLines(text);
        if (!sep.contains(","))
            text = text.replace(",", "");
        return text;
    }

    public static Bin[] fromValues(double[] a) throws Exception
    {
        List<Bin> list = new ArrayList<>();

        int age = 0;
        for (double v : a)
        {
            list.add(new Bin(age, age, v));
            age++;
        }

        return Bins.bins(list);
    }
    
    public static boolean compatibleLayout(Bin[] bins1, Bin[] bins2)
    {
        if (bins1.length != bins2.length)
            return false;
        
        for (int k = 0; k < bins1.length; k++)
        {
            if (bins1[k].age_x1 != bins2[k].age_x1)
                return false;
            
            if (bins1[k].age_x2 != bins2[k].age_x2)
                return false;
        }
        
        return true;
    }
    
    public static boolean isEqualWidths(Bin... bins)
    {
        Bin first = bins[0];
        
        for (Bin bin : bins)
        {
            if (bin.widths_in_years != first.widths_in_years)
                return false;
        }
        
        return true;
    }
}
