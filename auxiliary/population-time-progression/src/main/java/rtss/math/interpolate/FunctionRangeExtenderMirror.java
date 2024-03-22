package rtss.math.interpolate;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

/**
 * Extend a "legal" range of spline function beyond end-knots by mirroring its movement around end knots 
 */
public class FunctionRangeExtenderMirror implements UnivariateFunction
{
    final double[] knots;
    final PolynomialFunction[] pf;
    final PolynomialSplineFunction sp;

    final double firstX;
    final double firstY;

    final double lastX;
    final double lastY;

    public FunctionRangeExtenderMirror(PolynomialSplineFunction sp) throws Exception
    {
        this.sp = sp;
        this.knots = sp.getKnots();
        this.pf = sp.getPolynomials();

        if (pf.length != knots.length - 1 || knots.length < 2)
            throw new Exception("Unexpected PolynomialSplineFunction");

        firstX = knots[0];
        lastX = knots[knots.length - 1];

        firstY = sp.value(firstX);
        lastY = sp.value(lastX);
    }

    @Override
    public double value(double x)
    {
        if (sp.isValidPoint(x))
            return sp.value(x);

        if (x < firstX)
        {
            double y = sp.value(2 * firstX - x);
            return 2 * firstY - y;
        }
        else if (x > lastX)
        {
            double y = sp.value(2 * lastX - x);
            return 2 * lastY - y;
        }
        else
        {
            return sp.value(x);
        }
    }

}
