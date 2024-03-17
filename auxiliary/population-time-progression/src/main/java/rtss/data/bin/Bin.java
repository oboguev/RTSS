package rtss.data.bin;

public class Bin
{
    public int age_x1;      // starting age of this bin
    public int age_x2;      // last age of this bin 
    public int widths_in_years;
    // public int widths_in_points;
    public double avg;
    public double mid_x;
    public Bin prev;
    public Bin next;
    public int index;
    
    public Bin(int age_x1, int age_x2, double avg)
    {
        this.age_x1 = age_x1;
        this.age_x2 = age_x2;
        this.avg = avg;
        this.widths_in_years = age_x2 - age_x1 + 1;
        mid_x = (age_x1 + age_x2) / 2.0;
        // this.widths_in_points = this.widths_in_years * PostProcess.PointsPerYear;
    }
    
    public Bin(Bin bin)
    {
        this.age_x1 = bin.age_x1;
        this.age_x2 = bin.age_x2;
        this.widths_in_years = bin.widths_in_years;
        // this.widths_in_points = bin.widths_in_points;
        this.avg = bin.avg;
        this.mid_x = bin.mid_x;
        // this.prev = bin.prev;
        // this.next = bin.next;
        // this.index = bin.index;
    }
}
