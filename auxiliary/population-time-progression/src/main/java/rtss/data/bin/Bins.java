package rtss.data.bin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        double[] x = new double[bins.length];
        int k = 0;
        for (Bin bin : bins)
            x[k++] = bin.avg;
        return x;
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
}
