package rtss.math.pclm;

import java.util.Map;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.external.R.R;
import rtss.external.R.Script;

/**
 * Decompose bin data into single-year values with PCLM (penalized composite link model).
 * 
 * S. Rizzi et.al., "Efficient Estimation of Smooth Distributions From Coarsely Grouped Data" //
 * American Journal of Epidemiology, 2015;182(2):138–147.
 * 
 */
public class PCLM_Rizzi_2015
{
    private static final String keyModGamma = "mod$gamma";
    private static final String[] keys = {keyModGamma};
    
    /*
     * Lambda is a smoothing parameter.
     * To avoid smoothing, keep it low (e.g. 0.0001).
     */
    public static double[] pclm(Bin[] bins, double lambda) throws Exception
    {
        final String nl = "\n";

        Bin[] xbins = Bins.avg2sum(bins);
        
        final int min_year = Bins.firstBin(xbins).age_x1; 
        final int max_year = Bins.lastBin(xbins).age_x2;
        
        // to convert index (1...) to year, add ix2year
        final int ix2year = min_year - 1;   

        // to convert year to index (1...), add year2ix
        final int year2ix = -ix2year;
        
        // number of years
        final int m = max_year - min_year + 1;
        
        // number of bins
        final int n = xbins.length;
        
        // bin values
        double[] y = Bins.midpoint_y(xbins);
        
        // fill matrix C
        StringBuilder sb = new StringBuilder();
        for (Bin bin : xbins)
        {
            sb.append(String.format("C[%d, %d:%d] <- 1" + nl,
                                    bin.index + 1,
                                    bin.age_x1 + year2ix,
                                    bin.age_x2 + year2ix));
        }
        
        String script = Script.script("r-scripts/PCLM_Rizzi_2015.r", 
                                      "m", i2s(m),
                                      "n", i2s(n),
                                      "y", R.c(y),
                                      "fill_C", sb.toString(),
                                      "lambda", String.format("%s", lambda));
        
        String reply = R.execute(script, true);
        Map<String,String> rmap = R.keysFromReply(reply, keys);
        double[] d = R.indexedVectorD(rmap.get(keyModGamma));
        return d;
    }
    
    private static String i2s(int i)
    {
        return "" + i;
    }
}
