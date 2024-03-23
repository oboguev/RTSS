package rtss.data;

public class Points
{
    public int npoints;
    public double[] x;
    public double[] y;
    public double[] avg;

    public Points(int npoints)
    {
        this.npoints = npoints;
        this.x = new double[npoints];
        this.y = new double[npoints];
        this.avg = new double[npoints];
    }
}
