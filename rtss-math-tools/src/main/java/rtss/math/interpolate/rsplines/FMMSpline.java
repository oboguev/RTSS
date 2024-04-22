package rtss.math.interpolate.rsplines;

import org.apache.commons.math3.analysis.UnivariateFunction;

public class FMMSpline extends SplineCommonCore implements UnivariateFunction
{
    private Coefficients cf;
    
    public FMMSpline(double[] x, double[] y)
    {
        cf = makeCoefficients(SplineMethod.FMM, x, y);
    }

    @Override
    public double value(double x)
    {
        return spline_eval(SplineMethod.FMM, x, cf);
    }
}
