package rtss.ww2losses.util;

import rtss.data.asfr.AgeSpecificFertilityRates;
import rtss.data.asfr.AgeSpecificFertilityRatesByTimepoint;
import rtss.data.asfr.AgeSpecificFertilityRatesByYear;
import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.util.Util;

public class RebalanceASFR
{
    public static void rebalance_1941_halfyears(
            AgeSpecificFertilityRatesByYear yearly_asfrs,
            AgeSpecificFertilityRatesByTimepoint halfyearly_asfrs) throws Exception
    {
        AgeSpecificFertilityRates ar1940 = yearly_asfrs.getForYear(1940);
        AgeSpecificFertilityRates ar1941 = yearly_asfrs.getForYear(1941);
        AgeSpecificFertilityRates ar1941_1 = halfyearly_asfrs.getForTimepoint("1941.0");
        AgeSpecificFertilityRates ar1941_2 = halfyearly_asfrs.getForTimepoint("1941.1");

        Util.assertion(Bins.compatibleLayout(ar1940.binsReadonly(), ar1941.binsReadonly()));
        Util.assertion(Bins.compatibleLayout(ar1941_1.binsReadonly(), ar1941_2.binsReadonly()));
        Util.assertion(Bins.compatibleLayout(ar1941.binsReadonly(), ar1941_1.binsReadonly()));

        Bin[] b1940 = ar1940.binsReadonly();
        Bin[] b1941 = ar1941.binsReadonly();
        Bin[] b1941_1 = ar1941_1.binsWritable();
        Bin[] b1941_2 = ar1941_2.binsWritable();

        for (int k = 0; k < b1941.length; k++)
        {
            Util.assertion(Util.same(b1941_1[k].avg + b1941_2[k].avg, 2 * b1941[k].avg, 0.0005));

            b1941_1[k].avg = b1940[k].avg;
            b1941_2[k].avg = 2 * b1941[k].avg - b1940[k].avg;
        }

        Util.noop();
    }
}
