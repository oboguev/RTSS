package rtss.math.interpolate.rsplines;

import org.apache.commons.math3.analysis.UnivariateFunction;

public class HymanSpline extends SplineCommonCore implements UnivariateFunction
{
    private Coefficients cf;
    
    public HymanSpline(double[] x, double[] y)
    {
        cf = makeCoefficients(SplineMethod.HYMAN, x, y);
    }

    @Override
    public double value(double x)
    {
        return spline_eval(SplineMethod.HYMAN, x, cf);
    }
}
