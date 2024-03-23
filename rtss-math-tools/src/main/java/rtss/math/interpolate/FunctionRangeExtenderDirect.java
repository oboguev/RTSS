package rtss.math.interpolate;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

/**
 * Extend a "legal" range of spline function beyond end-knots by directly invoking the underlying polynomials
 */
public class FunctionRangeExtenderDirect implements UnivariateFunction
{
    final double[] knots;
    final PolynomialFunction[] pf;
    final PolynomialSplineFunction sp;

    final double firstX;
    final double lastX;
    final double prelastX;

    public FunctionRangeExtenderDirect(PolynomialSplineFunction sp) throws Exception
    {
        this.sp = sp;
        this.knots = sp.getKnots();
        this.pf = sp.getPolynomials();

        if (pf.length != knots.length - 1 || knots.length < 2)
            throw new Exception("Unexpected PolynomialSplineFunction");

        firstX = knots[0];
        lastX = knots[knots.length - 1];
        prelastX = knots[knots.length - 2];
    }

    @Override
    public double value(double x)
    {
        if (sp.isValidPoint(x))
            return sp.value(x);

        if (x < firstX)
        {
            return pf[0].value(x - firstX);
        }
        else if (x > lastX)
        {
            return pf[pf.length - 1].value(x - prelastX);
        }
        else
        {
            return sp.value(x);
        }
    }
}
