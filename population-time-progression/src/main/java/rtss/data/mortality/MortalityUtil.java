package rtss.data.mortality;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;

public class MortalityUtil
{
    public static double qx2mx(double qx) throws Exception
    {
        qx = validate_qx(qx);
        double mx = qx / (1 - qx / 2);
        return mx;
    }

    public static double mx2qx(double mx) throws Exception
    {
        mx = validate_mx(mx);
        double qx = mx / (1 + mx / 2);
        return qx;
    }

    public static double[] qx2mx(double[] qx) throws Exception
    {
        double[] mx = new double[qx.length];
        for (int k = 0; k < qx.length; k++)
            mx[k] = qx2mx(qx[k]);
        return mx;
    }

    public static double[] mx2qx(double[] mx) throws Exception
    {
        double[] qx = new double[mx.length];
        for (int k = 0; k < qx.length; k++)
            qx[k] = mx2qx(mx[k]);
        return qx;
    }
    
    public static Bin[] qx2mx(Bin[] bins) throws Exception
    {
        bins = Bins.clone(bins);
        for (Bin bin: bins)
            bin.avg = qx2mx(bin.avg);
        return bins;
    }

    public static Bin[] mx2qx(Bin[] bins) throws Exception
    {
        bins = Bins.clone(bins);
        for (Bin bin: bins)
            bin.avg = mx2qx(bin.avg);
        return bins;
    }
    
    private static double validate_qx(double qx) throws Exception
    {
        if (qx >= 0 && qx <= 1.0)
            return qx;
        throw new Exception("qx value is out of valid range");
    }

    private static double validate_mx(double mx) throws Exception
    {
        if (mx >= 0 && mx <= 2.0)
            return mx;
        throw new Exception("mx value is out of valid range");
    }
}
