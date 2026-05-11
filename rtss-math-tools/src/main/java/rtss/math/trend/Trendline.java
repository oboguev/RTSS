package rtss.math.trend;

import java.util.List;

import org.apache.commons.math4.legacy.stat.regression.SimpleRegression;

public class Trendline
{
    public double a;
    public double b;
    
    public double predict(double x)
    {
        return a * x + b;
    }

    public static Trendline create(List<Double> x, List<Double> y)
    {
        double[] ax = x.stream().mapToDouble(Double::doubleValue).toArray();
        double[] ay = y.stream().mapToDouble(Double::doubleValue).toArray();
        return create(ax, ay);
    }
    
    public static Trendline create(double[] x, double[] y)
    {
        if (x == null || y == null)
            throw new IllegalArgumentException("x and y must not be null");

        if (x.length != y.length)
            throw new IllegalArgumentException("x and y have different length");

        if (x.length < 2)
            throw new IllegalArgumentException("at least two points are required");

        SimpleRegression regression = new SimpleRegression();
        for (int k = 0; k < x.length; k++)
        {
            if (!Double.isFinite(x[k]) || !Double.isFinite(y[k]))
                throw new IllegalArgumentException("x and y must contain only finite values");
            regression.addData(x[k], y[k]);
        }

        Trendline tl = new Trendline();

        tl.a = regression.getSlope();
        tl.b = regression.getIntercept();

        if (!Double.isFinite(tl.a) || !Double.isFinite(tl.b))
            throw new IllegalArgumentException("regression cannot be estimated; x values may all be equal");

        return tl;
    }
}
