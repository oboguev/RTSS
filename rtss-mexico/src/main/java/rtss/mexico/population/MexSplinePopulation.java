/**
 * Вычислить погодовое население Мексики интерполяцией сплайном между данными периписей. 
 */
package rtss.mexico.population;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.commons.math3.analysis.UnivariateFunction;

import rtss.math.interpolate.ConstrainedCubicSplineInterpolator;
import rtss.math.interpolate.FunctionRangeExtenderDirect;
import rtss.util.Util;
import rtss.util.plot.ChartXY;

import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

public class MexSplinePopulation
{
    public static void main(String[] args)
    {
        try
        {
            Util.out("Население на середину года");
            new MexSplinePopulation().do_main(0.5);

            Util.out("");
            Util.out("Население на начало года:");
            new MexSplinePopulation().do_main(0.0);

            Util.noop();
        }
        catch (Throwable ex)
        {
            Util.err("*** Exception");
            ex.printStackTrace();
        }
    }

    static Census[] censuses = {
                                 new Census(1895, 10, 20, 12_700),
                                 new Census(1900, 10, 28, 13_607),
                                 new Census(1910, 10, 27, 15_160),
                                 new Census(1921, 11, 30, 14_335),
                                 new Census(1930, 5, 15, 16_553),
                                 new Census(1940, 3, 6, 19_654),
                                 new Census(1950, 6, 6, 25_791),
                                 new Census(1960, 6, 8, 34_923),
                                 new Census(1970, 1, 28, 48_225),
                                 new Census(1980, 6, 4, 68_847),
                                 new Census(1990, 3, 12, 81_250),
                                 new Census(2000, 2, 14, 97_483),
                                 new Census(2010, 6, 12, 112_337),
                                 new Census(2020, 3, 15, 126_014)
    };

    static class Census
    {
        public Census(int year, int month, int day, int population)
        {
            this.year = year;
            this.month = month;
            this.day = day;
            this.population = population;
        }

        public double inYearPosition()
        {
            Calendar calendar = new GregorianCalendar(year, month - 1, day);
            int ndays = calendar.getActualMaximum(Calendar.DAY_OF_YEAR);
            double r = calendar.get(Calendar.DAY_OF_YEAR) - 1;
            return r / ndays;
        }

        public int year;
        public int month;
        public int day;
        public long population;
    }

    private void do_main(double offset) throws Exception
    {
        double[] x = new double[censuses.length];
        double[] y = new double[censuses.length];

        for (int k = 0; k < censuses.length; k++)
        {
            x[k] = censuses[k].year + censuses[k].inYearPosition();
            y[k] = censuses[k].population;
        }

        PolynomialSplineFunction sp = new ConstrainedCubicSplineInterpolator().interpolate(x, y);
        // PolynomialSplineFunction sp = new AkimaSplineInterpolator().interpolate(x, y);
        // PolynomialSplineFunction sp = new SteffenSplineInterpolator().interpolate(x, y);
        UnivariateFunction f = new FunctionRangeExtenderDirect(sp);
        
        List<Double> xx = new ArrayList<Double>();
        List<Double> yy = new ArrayList<Double>();
        for (int k = 1895; k <= 2020; k++)
        {
            xx.add(k + offset);
            yy.add(f.value(k + offset));
        }
        
        ChartXY chart = new ChartXY();
        chart.addSeries("points", x, y);
        chart.addSeries("spline", asArray(xx), asArray(yy));
        chart.display();
        
        if (offset == 0.5)
        {
            xx.add(2021 + 0.5);
            yy.add((double) 127_839);
            
            xx.add(2022 + 0.5);
            yy.add((double) 129_535);
            
            xx.add(2023 + 0.5);
            yy.add((double) 131_230);
        }
        
        for (int k = 0; k < xx.size(); k++)
        {
            long year = Math.round(xx.get(k) - 0.5);
            long pop = Math.round(yy.get(k));
            Util.out(String.format("%d %d", year, pop));
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
