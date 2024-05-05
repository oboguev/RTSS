package rtss.data.mortality.laws;

import java.util.LinkedHashMap;
import java.util.Map;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.curves.PoorCurveException;
import rtss.external.Script;
import rtss.external.R.R;
import rtss.util.Util;

/**
 * Fit Heligman-Pollard mortality law into given age-specific mortality rates.
 * 
 * L. Heligman, J. H. Pollard, "Age Pattern of Mortality" // Journal of the Institute of Actuaries (1886-1994), 
 * June 1980, Vol. 107, No. 1, pp. 49-80 
 */
public class HeligmanPollard_R extends HeligmanPollard
{
    private static final String keyMessage = "fit$opt.diagnosis$message";
    private static final String keyDeviance = "fit$deviance";
    private static final String keyCoefficients = "fit$coefficients";
    private static final String keyResiduals = "fit$residuals";
    
    private static final String[] keys = {keyMessage, keyDeviance, keyCoefficients, keyResiduals};
    
    private Bin[] bins;
    
    public HeligmanPollard_R(Bin[] bins) throws PoorCurveException, Exception
    {
        /*
         * Unfortunately, existing R implementation of Heligman-Pollard estimator
         * fits the curve to points rather than intervals, as the result intervals means are severely 
         * deviated.
         * 
         * Also, for basic "HP" formula it fits qp rather than qx curve.
         * Although this can perhaps be amended by pre-conversion of input data to the qp form (see qx2qp).   
         */
        if (Util.False)
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
                               // "bin_start_age", R.c(bin_start_age),
                               "bin_start_age", R.c(Bins.midpoint_x(bins)),
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

    public double[] curve(int ppy)
    {
        return curve(ppy, bins);
    }
}
