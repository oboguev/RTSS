package rtss.ww2losses;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.population.PopulationByLocality;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.math.interpolate.TargetPrecision;
import rtss.math.interpolate.mpspline.MeanPreservingIterativeSpline;
import rtss.util.Util;
import rtss.util.plot.ChartXYSplineAdvanced;

/**
 * Вычислить положение и размер впадины в числе родившихся в 1941-1945 гг. и доживших до переписи 1959 года. 
 */
public class BirthTrough
{
    private static final int PointsPerYear = 100;
    
    public void calcTrough(Area area) throws Exception
    {
        /* load census data */
        PopulationByLocality p = PopulationByLocality.census(area, 1959);
        List<Bin> binlist = new ArrayList<>();
        for (int age = 0; age <= 40; age++)
            binlist.add(new Bin(age, age, p.get(Locality.TOTAL, Gender.BOTH, age)));
        Bin[] bins = Bins.bins(binlist);
        
        /* interpolate it into a sub-year range */
        TargetPrecision precision = new TargetPrecision().eachBinAbsoluteDifference(0.1);
        MeanPreservingIterativeSpline.Options options = new MeanPreservingIterativeSpline.Options()
                .checkPositive(true)
                .basicSplineType(AkimaSplineInterpolator.class);
        double[] yy = MeanPreservingIterativeSpline.eval(bins, PointsPerYear, options, precision);
        
        if (Util.True)
        {
            double[] xx = Bins.ppy_x(bins, PointsPerYear);
            ChartXYSplineAdvanced chart = new ChartXYSplineAdvanced("BirthTrough " + area.toString(), "x", "y");
            chart.addSeries("interpolation", xx, yy);
            chart.addSeries("bins", xx, Bins.ppy_y(bins, PointsPerYear));
            chart.display();
        }
        
        /* 4-year window */
        int winsize = 4 * PointsPerYear;
        int min_np = -1;
        double min_sum = 0;
        
        for (int np = 0; np < yy.length - winsize; np++) 
        {
            double[] yw = Util.splice(yy, np, np + winsize - 1);
            double sum = Util.sum(yw) / PointsPerYear;
            if (min_np == -1 || sum < min_sum)
            {
                min_np = np;
                min_sum = sum;
            }
            
            if (np2age(np) > 20)
                break;
        }
        
        int np1 = min_np;
        int np2 = np1 + winsize;
        int np_mid = (np1 + np2) / 2;
        
        Util.out(String.format("Minimum births window: age = [%f - %f - %f], total births: %d", 
                               np2age(np1), np2age(np_mid), np2age(np2), Math.round(min_sum)));
        Util.noop();
    }
    
    private double np2age(int np)
    {
        return ((double) np) / PointsPerYear;
    }
}
