package rtss.data.mortality.laws;

import static java.lang.Math.pow;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;

/**
 * Calculate the curve for Heligman-Pollard mortality law.
 * 
 * L. Heligman, J. H. Pollard, "Age Pattern of Mortality" // Journal of the Institute of Actuaries (1886-1994), 
 * June 1980, Vol. 107, No. 1, pp. 49-80 
 */
public class HeligmanPollard
{
    public double A;
    public double B;
    public double C;
    public double D;
    public double E;
    public double F;
    public double G;
    public double H;

    public double qx(double x)
    {
        double qp = qp(x);
        double qx = qp / (1 + qp);
        return qx;
    }

    /*
     * qp iss (qx/px) = (qx / (1-qx))
     */
    private double qp(double x)
    {
        double qp1 = pow(A, pow(x+B, C));

        double qp2 = 0;
        if (x / F > 1E-8)
        {
            double v = ln(x/F);
            v *= v;
            v = Math.exp(-E * v);
            qp2 = D * v;
        }
        
        double qp3 = G * pow(H, x);
        
        return qp1 + qp2 + qp3;
    }
    
    private double ln(double x)
    {
        return Math.log(x);
    }
    
    /*
     * Get qx curve recalibrated to promille
     */
    public double[] curve(int ppy, Bin[] bins)
    {
        Bin first = Bins.firstBin(bins);
        Bin last = Bins.lastBin(bins);
        int years = last.age_x2 - first.age_x1 + 1;
        double[] curve = new double[years * ppy];

        for (int k = 0; k < curve.length; k++)
        {
            double x = first.age_x1 + ((double) k) / ppy;
            curve[k] = qx(x) * 1000;
        }
        
        return curve;
    }
    
    @SuppressWarnings("unused")
    private double promille2qp(double promille)
    {
        return qx2qp(promille / 1000);
    }
    
    private double qx2qp(double qx)
    {
        return qx / (1 - qx);
    }
}
