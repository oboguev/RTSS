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

        int nTunablePoints = 10;
        int nFixedPoints = 2;
        
        if (bins[0].avg > bins[1].avg && bins[1].avg > bins[2].avg)
        {
            nTunablePoints = 10;  // ages 0-9
            nFixedPoints = 2;     // ages 10-11
        }
        else if (bins[0].avg > bins[1].avg && bins[1].avg < bins[2].avg)
        {
            // ### locate minimum point
            nTunablePoints = 10;  // ###
            nFixedPoints = 2;     // ###
        }
        else
        {
            return p0;
        }

        double psum_04 = Util.sum(Util.splice(p0, 0, 4));
        double psum_59 = Util.sum(Util.splice(p0, 5, 9));

        double[] attrition = Util.normalize(RefineYearlyPopulationModel.select_attrition09(yearHint));
        double importance_smoothness = 0.7;
        double importance_target_diff_matching = 0.3;

        p = Util.splice(p0, 0, 11);
        
        try 
        {
            double[] px = optimizeSeries(Util.dup(p), 
                                         Util.splice(p, 0, 9), 
                                         psum_04, psum_59, 
                                         attrition, 
                                         importance_smoothness, 
                                         importance_target_diff_matching,
                                         nTunablePoints, 
                                         nFixedPoints);
            
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
