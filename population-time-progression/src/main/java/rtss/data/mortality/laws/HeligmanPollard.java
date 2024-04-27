package rtss.data.mortality.laws;

import rtss.data.bin.Bin;
import rtss.external.R.R;
import rtss.external.R.Script;

public class HeligmanPollard
{
    public HeligmanPollard(Bin[] bins) throws Exception
    {
        int[] start_age = new int[bins.length];
        double[] deaths = new double[bins.length];
        double[] exposure = new double[bins.length];
        
        for (Bin bin : bins)
        {
            int i = bin.index;
            start_age[i] = bin.age_x1;
            deaths[i] = bin.avg * bin.widths_in_years;
            exposure[i] = 1000 * bin.widths_in_years;
        }
        
        String script = Script.script("r-scripts/heligman-pollard.r", 
                               "bin_start_age", R.c(start_age),
                               "death_count", R.c(deaths),
                               "exposure", R.c(exposure));
        String reply = R.execute(script, true);
        reply = null;
    }
}
