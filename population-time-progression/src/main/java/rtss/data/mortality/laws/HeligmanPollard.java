package rtss.data.mortality.laws;

import java.util.LinkedHashMap;
import java.util.Map;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.curves.PoorCurveException;
import rtss.external.R.R;
import rtss.external.R.Script;
import rtss.util.Util;

import static java.lang.Math.pow; 

/**
 * Fit Heligman-Pollard mortality law into given age-specific mortality rates.
 * 
 * L. Heligman, J. H. Pollard, "Age Pattern of Mortality" // Journal of the Institute of Actuaries (1886-1994), 
 * June 1980, Vol. 107, No. 1, pp. 49-80 
 */
public class HeligmanPollard
{
    private static final String keyMessage = "fit$opt.diagnosis$message";
    private static final String keyDeviance = "fit$deviance";
    private static final String keyCoefficients = "fit$coefficients";
    private static final String keyResiduals = "fit$residuals";
    
    private static final String[] keys = {keyMessage, keyDeviance, keyCoefficients, keyResiduals};
    
    private double A;
    private double B;
    private double C;
    private double D;
    private double E;
    private double F;
    private double G;
    private double H;
    
    private Bin[] bins;
    
    public HeligmanPollard(Bin[] bins) throws PoorCurveException, Exception
    {
        /*
         * Unfortunately, existing R implementation of Heligman-Pollard estimator
         * fits the curve to points rather than intervals, as the result intervals means are severely 
         * deviated.
         * 
         * Also, for basic "HP" formula it fits qp rather than qx curve.
         * Although this can perhaps be amended by pre-conversion of input data to the qp form (see qx2qp).   
         */
        if (Util.True)
            bins = Bins.yearlyBins(bins);
            
        this.bins = bins = Bins.clone(bins);
                
        int[] bin_start_age = new int[bins.length];
        double[] deaths = new double[bins.length];
        double[] qp_deaths = new double[bins.length];
        double[] exposure = new double[bins.length];
        
        for (Bin bin : bins)
        {
            int i = bin.index;
            bin_start_age[i] = bin.age_x1;
            qp_deaths[i] = deaths[i] = bin.avg * bin.widths_in_years / 1000;
            // qp_deaths[i] = qx2qp(qp_deaths[i]);
            exposure[i] = bin.widths_in_years;
        }
        
        String script = Script.script("r-scripts/heligman-pollard.r", 
                               "bin_start_age", R.c(bin_start_age),
                               "death_count", R.c(qp_deaths),
                               "exposure", R.c(exposure));
        String reply = R.execute(script, true);
        Map<String,String> m = R.keysFromReply(reply, keys);

        if (m.get(keyMessage).contains("false convergence"))
            throw new PoorCurveException("Unable to fit Heligman-Pollard");
        
        LinkedHashMap<String,Double> mc = R.namedVectorSD(m.get(keyCoefficients));
        
        A = mc.get("A");
        B = mc.get("B");
        C = mc.get("C");
        D = mc.get("D");
        E = mc.get("E");
        F = mc.get("F_");
        G = mc.get("G");
        H = mc.get("H");
    }
    
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
    public double[] curve(int ppy)
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
