package rtss.data.mortality.synthetic;

import rtss.data.bin.Bin;

public class MakeCurve
{
    public double[] averages(Bin... bins)
    {
        double[] d = new double[Bin.widths_in_years(bins)];
        
        int x = 0;
        
        for (Bin bin : bins)
        {
            for (int k = 0; k < bin.widths_in_years; k++)
                d[x++] = bin.avg;
        }
        
        return d;
    }

    public double[] curve(Bin... bins)
    {
        // ###
        return averages(bins);
    }
}
