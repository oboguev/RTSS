package rtss.math.interpolate;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

/**
 * Extend a "legal" range of spline function beyond end-knots by mirroring its movement around end knots 
 */
public class FunctionRangeExtenderMirror implements UnivariateFunction
{
    private final UnivariateFunction sp;
    private final double firstX;
    private final double lastX;
    
    public FunctionRangeExtenderMirror(UnivariateFunction sp, double firstX, double lastX) throws Exception
    {
        this.sp = sp;
        this.firstX = firstX;
        this.lastX = lastX;
    }

    public FunctionRangeExtenderMirror(PolynomialSplineFunction sp) throws Exception
    {
        this.sp = sp;

        final double[] knots = sp.getKnots();
        final PolynomialFunction[] pf = sp.getPolynomials();

        if (pf.length != knots.length - 1 || knots.length < 2)
            throw new Exception("Unexpected PolynomialSplineFunction");

        firstX = knots[0];
        lastX = knots[knots.length - 1];
    }

    @Override
    public double value(double x)
    {
        if (isValidPoint(x))
            return sp.value(x);

        if (x < firstX)
        {
            double firstY = sp.value(firstX);
            double y = sp.value(2 * firstX - x);
            return 2 * firstY - y;
        }
        else if (x > lastX)
        {
            double lastY = sp.value(lastX);
            double y = sp.value(2 * lastX - x);
            return 2 * lastY - y;
        }
        else
        {
            return sp.value(x);
        }
    }
    
    private boolean isValidPoint(double x)
    {
        if (sp instanceof PolynomialSplineFunction)
            return ((PolynomialSplineFunction) sp).isValidPoint(x);
        
        return x >= firstX && x <= lastX;
    }
}
