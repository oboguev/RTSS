package rtss.data.bin;

public class Bin
{
    public int age_x1;
    public int age_x2;
    public int widths_in_years;
    // public int widths_in_points;
    public double avg;
    public Bin prev;
    public Bin next;
    public int index;
    
    public Bin(int age_x1, int age_x2, double avg)
    {
        this.age_x1 = age_x1;
        this.age_x2 = age_x2;
        this.avg = avg;
        this.widths_in_years = age_x2 - age_x1;
        // this.widths_in_points = this.widths_in_years * PostProcess.PointsPerYear;
    }
    
    public static Bin[] bins(Bin... bins) throws Exception
    {
        Bin prev = null;
        
        for (Bin bin : bins)
        {
            bin.prev = prev;
            if (prev != null)
                prev.next = bin;
            prev = bin;            

            if (bin.widths_in_years < 1)
                throw new Exception("Invalid bin width");        

            if (bin.next != null && bin.age_x2 != bin.next.age_x1)
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
}
