package rtss.tools;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import rtss.math.interpolate.ConstrainedCubicSplineInterpolator;
import rtss.util.Util;
import rtss.util.plot.ChartXY;

public class FitCurve
{
    public static void main(String[] args)
    {
        try
        {
            new FitCurve().do_main();
            Util.noop();
        }
        catch (Throwable ex)
        {
            System.err.println("*** Exception");
            ex.printStackTrace();
        }
    }

    // private double x[] = {1918, 1928, 1950, 1964, 1973, 1983, 2000, 2014, 2020};
    // private double y[] = {1.8, 2.3, 3.8, 4.2, 3.9, 3.1, 2.2 , 1.8, 1.7};

    private double x[] = { 1918, 1928, 1950, 1964, 1973, 1983, 2000, 2014, 2020 };
    private double y[] = { 1.7, 2.25, 3.75, 4.1, 3.85, 3.05, 2.15, 1.7, 1.65 };

    private void do_main()
    {
        PolynomialSplineFunction f = new ConstrainedCubicSplineInterpolator().interpolate(x, y);
        double x0 = x[0];
        double x1 = x[x.length - 1];
        List<Double> xx = new ArrayList<Double>();
        List<Double> yy = new ArrayList<Double>();
        for (double xz = x0; xz <= x1; xz += 1)
        {
            xx.add(xz);
            yy.add(f.value(xz));
        }
        ChartXY chart = new ChartXY();
        chart.addSeries("points", x, y);
        chart.addSeries("spline", asArray(xx), asArray(yy));
        chart.display();

        for (int k = 0; k < xx.size(); k++)
        {
            Util.out(String.format("%d %f", Math.round(xx.get(k)), yy.get(k)));
        }
    }

    private double[] asArray(List<Double> x)
    {
        double a[] = new double[x.size()];
        for (int k = 0; k < x.size(); k++)
            a[k] = x.get(k);
        return a;
    }
}
