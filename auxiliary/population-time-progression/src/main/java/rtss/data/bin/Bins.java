package rtss.data.bin;

import java.util.ArrayList;

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
    
    public static int flips(Bin...bins) throws Exception
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
}
