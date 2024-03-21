package rtss.math.mpspline;

import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.util.plot.ChartXYSplineAdvanced;

public class MeanPreservingIterativeSpline
{
    public static double[] eval(Bin[] bins, int ppy)
    {
        return new MeanPreservingIterativeSpline().do_eval(bins, ppy);
    }

    private double[] do_eval(Bin[] bins, int ppy)
    {
        double x1 = Bins.firstBin(bins).age_x1;
        double x2 = Bins.lastBin(bins).age_x2 + 1;

        int xcount = Bins.widths_in_years(bins) * ppy;
        double[] xx = new double[xcount];
        for (int k = 0; k < xcount; k++)
            xx[k] = x1 + k * (x2 - x1) / xcount;

        double[] yy = new double[xcount];
        double[] result = new double[xcount];

        // TODO: use weighted control points
        double[] cp_x = Bins.midpoint_x(bins);
        double[] cp_y = Bins.midpoint_y(bins);
        double[] binavg = Bins.midpoint_y(bins);

        for (;;)
        {
            PolynomialSplineFunction sp = new AkimaSplineInterpolator().interpolate(cp_x, cp_y);

            for (int k = 0; k < xcount; k++)
                yy[k] = splineValue(sp, xx[k]);

            for (int k = 0; k < xcount; k++)
                result[k] += yy[k];

            /*
             * re-bin the results
             */
            double[] yearly = Bins.ppy2yearly(result, ppy);
            double[] avg = Bins.yearly2bin_avg(bins, yearly);

            // ###
            new ChartXYSplineAdvanced("MPS", "x", "y")
                    .addSeries("MPS", yearly)
                    .addSeries("bins", Bins.bins2yearly(bins))
                    .display();
            // ###

            /*
             * calculate residue
             */
            cp_y = Bins.midpoint_y(bins);
            for (int ix = 0; ix < avg.length; ix++)
                cp_y[ix] -= avg[ix];

            boolean differ = false;
            double avg_diff = 0;
            for (int ix = 0; ix < avg.length; ix++)
            {
                double v1 = cp_y[ix];
                double v2 = binavg[ix];
                double diff = Math.abs(v1) / Math.max(Math.abs(v2), Math.abs(v2));
                avg_diff += diff;
                if (diff > 0.001)
                    differ = true;
            }
            avg_diff /= avg.length;

            if (!differ)
                break;
        }

        return result;
    }

    private double splineValue(PolynomialSplineFunction sp, double x)
    {
        double[] knots = sp.getKnots();

        if (x < knots[0])
        {
            PolynomialFunction[] pf = sp.getPolynomials();
            return pf[0].value(x - knots[0]);
        }
        else if (x > knots[knots.length - 1])
        {
            PolynomialFunction[] pf = sp.getPolynomials();
            return pf[pf.length - 1].value(x - knots[knots.length - 1]);
        }
        else
        {
            return sp.value(x);
        }
    }
}
