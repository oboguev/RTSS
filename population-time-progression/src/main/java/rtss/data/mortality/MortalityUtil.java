package rtss.data.mortality;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.util.Util;

public class MortalityUtil
{
    private static final int NPOINTS_QX2MX = 1000;
    private static final int MX2QX_TABLE_SIZE = 1000;
    private static double[] mx2qx_table_mx = null;
    private static double[] mx2qx_table_qx = null;
    private static boolean useMxTable = true;
    
    /*
     * Convert mortality "qx" value to "mx"
     */
    public static double qx2mx(double qx) throws Exception
    {
        qx = validate_qx(qx);
        
        if (!useMxTable)
        {
            // linear conversion, acceptable for low values of the argument 
            double mx = qx / (1 - qx / 2);
            return mx;
        }
        else
        {
            double a = Math.pow(1 - qx, 1.0 / NPOINTS_QX2MX);
            double p = 1.0;
            double avg = 0;
            for (int k = 0; k < NPOINTS_QX2MX; k++)
            {
                avg += p;
                p *= a;
            }
            avg /= NPOINTS_QX2MX;
            
            double mx = qx / avg;
            return mx;
        }
    }

    public static double mx2qx(double mx) throws Exception
    {
        if (!useMxTable)
        {
            // linear conversion, acceptable for low values of the argument 
            mx = validate_mx(mx);
            double qx = mx / (1 + mx / 2);
            return qx;
        }
        else
        {
            build_qx2mx_table();
            mx = validate_mx(mx);
            int k1 = 0;
            int k2 = mx2qx_table_mx.length - 1;
            
            // search position in the table
            for (;;)
            {
                if (mx < mx2qx_table_mx[k1] || mx > mx2qx_table_mx[k2])
                    throw new Exception("Table search error");
                else if (mx == mx2qx_table_mx[k1])
                    return mx2qx_table_qx[k1];
                else if (mx == mx2qx_table_mx[k2])
                    return mx2qx_table_qx[k2];
                
                int k = (k1 + k2) / 2;
                if (k == k1 || k == k2)
                    break;

                if (mx == mx2qx_table_mx[k])
                    return mx2qx_table_qx[k];
                else if (mx > mx2qx_table_mx[k])
                    k1 = k;
                else
                    k2 = k;
            }
            
            // search between found table values
            double qx1 = mx2qx_table_qx[k1];
            double qx2 = mx2qx_table_qx[k2];
            for (;;)
            {
                double qx = (qx1 + qx2) / 2;
                double v = qx2mx(qx);
                if (Math.abs(mx - v) < 0.0001)
                    return qx;
                else if (mx > v)
                    qx1 = qx;
                else
                    qx2 = qx;
            }
        }
    }
    
    private static synchronized void build_qx2mx_table() throws Exception
    {
        if (mx2qx_table_mx == null)
        {
            mx2qx_table_mx = new double[MX2QX_TABLE_SIZE];
            mx2qx_table_qx = new double[MX2QX_TABLE_SIZE];
            double step = 1.0 / (MX2QX_TABLE_SIZE - 1);
            for (int k = 0; k < MX2QX_TABLE_SIZE; k++)
            {
                mx2qx_table_qx[k] = k * step; 
                mx2qx_table_mx[k] = qx2mx(mx2qx_table_qx[k]); 
            }
        }
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
    
    public static Bin[] proqx2mx(Bin[] bins) throws Exception
    {
        return qx2mx(Bins.multiply(bins, 0.001));
    }
    
    public static double[] proqx2mx(double[] proqx) throws Exception
    {
        return qx2mx(Util.multiply(proqx, 0.001));
    }

    private static double validate_qx(double qx) throws Exception
    {
        if (qx >= 0 && qx <= 1.0)
            return qx;
        throw new Exception("qx value is out of valid range");
    }

    private static double validate_mx(double mx) throws Exception
    {
        if (!useMxTable)
        {
            if (mx >= 0 && mx <= 2.0)
                return mx;
        }
        else
        {
            // restrict the range a bit
            if (mx >= 0 && mx <= mx2qx_table_mx[mx2qx_table_mx.length - 2])
                return mx;
        }
        
        throw new Exception("mx value is out of valid range");
    }
}
