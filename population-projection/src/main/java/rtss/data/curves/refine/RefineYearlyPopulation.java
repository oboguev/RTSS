package rtss.data.curves.refine;

import rtss.data.bin.Bin;
import rtss.util.Util;

public class RefineYearlyPopulation extends RefineYearlyPopulationBase
{
    public static double[] refine(Bin[] bins, String title, double[] p, Integer yearHint) throws Exception
    {
        final double[] p0 = p;
        
        if (Util.False)
            return p0;

        if (bins.length < 3 || 
                bins[0].widths_in_years != 5 || 
                bins[1].widths_in_years != 5 || 
                bins[3].widths_in_years != 5)
        {
            return p0;
        }

        if (Util.False && !Util.isMonotonicallyDecreasing(p, false))
            return p0;
        
        if (bins[0].avg <= bins[1].avg || bins[1].avg <= bins[2].avg)
            return p0;

        double psum_04 = Util.sum(Util.splice(p0, 0, 4));
        double psum_59 = Util.sum(Util.splice(p0, 5, 9));

        double[] attrition_09 = Util.normalize(RefineYearlyPopulationModel.select_attrition09(yearHint));
        double importance_smoothness = 0.7;
        double importance_target_diff_matching = 0.3;

        p = Util.splice(p0, 0, 11);
        
        try 
        {
            int nTunablePoints = 10;
            int nFixedPoints = 2;
            double[] px = optimizeSeries(Util.dup(p), 
                                         Util.splice(p, 0, 9), 
                                         psum_04, psum_59, 
                                         attrition_09, 
                                         importance_smoothness, 
                                         importance_target_diff_matching);
            
            Util.noop();
            // ###
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            Util.noop();
        }

        return p0;
    }
}
