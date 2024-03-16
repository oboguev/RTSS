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
}
