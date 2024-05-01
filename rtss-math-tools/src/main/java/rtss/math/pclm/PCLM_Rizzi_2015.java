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
    private static final String[] keys = { keyModGamma };

    @SuppressWarnings("unused")
    private final Bin[] bins;
    private final Bin[] xbins;
    private final double lambda;
    private final int ppy;

    private final int min_year;
    private final int max_year;

    private final int ix2year;
    private final int year2ix;

    /*
     * Lambda is a smoothing parameter.
     * To avoid smoothing, keep it low (e.g. 0.0001).
     * 
     * Do not use high ppy (above 10), as it will blow the size of matrixes. 
     */
    private PCLM_Rizzi_2015(Bin[] bins, double lambda, int ppy) throws Exception
    {
        this.bins = bins;
        this.lambda = lambda;
        this.ppy = ppy;

        this.xbins = Bins.avg2sum(bins);
        this.min_year = Bins.firstBin(xbins).age_x1;
        this.max_year = Bins.lastBin(xbins).age_x2;

        ix2year = min_year - 1;
        year2ix = -ix2year;
    }

    public static double[] pclm(Bin[] bins, double lambda, int ppy) throws Exception
    {
        return new PCLM_Rizzi_2015(bins, lambda, ppy).pclm();
    }

    private double[] pclm() throws Exception
    {
        final String nl = "\n";

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
                                    year2ix(bin.age_x1),
                                    year2ix(bin.age_x2)));
        }

        String script = Script.script("r-scripts/PCLM_Rizzi_2015.r",
                                      "m", i2s(m),
                                      "n", i2s(n),
                                      "y", R.c(y),
                                      "fill_C", sb.toString(),
                                      "lambda", String.format("%s", lambda));

        String reply = R.execute(script, true);
        Map<String, String> rmap = R.keysFromReply(reply, keys);
        double[] d = R.indexedVectorD(rmap.get(keyModGamma));
        return d;
    }

    private int year2ix(int year)
    {
        return year + year2ix;
    }

    private static String i2s(int i)
    {
        return "" + i;
    }
}
