package rtss.math.interpolate.rsplines;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.util.MathArrays;
import org.apache.commons.math3.util.MathArrays.OrderDirection;

/**
 * Forsythe, Malcolm and Moler spline.
 * An exact cubic is fitted through the four points at each end of the data,
 * and this is used to determine the end conditions.
 * 
 * Forsythe, Malcolm, Moler, "Computer methods for mathematical computations" (1977), p. 70-79
 * 
 */
public class FMMlSplineInterpolator extends SplineCommonCore implements UnivariateInterpolator
{
    @Override
    public UnivariateFunction interpolate(double[] x, double[] y)
            throws MathIllegalArgumentException, DimensionMismatchException
    {
        if (x.length != y.length)
            throw new DimensionMismatchException(x.length, y.length);

        if (x.length < 2)
            throw new NumberIsTooSmallException(LocalizedFormats.NUMBER_OF_POINTS, x.length, 3, true);

        MathArrays.checkOrder(x, OrderDirection.INCREASING, true, true);

        Coefficients cf = new Coefficients(x, y);
        fmm_spline(x, y, cf.b, cf.c, cf.d);

        // ###

        int n = 10; // ### 
        final PolynomialFunction polynomials[] = new PolynomialFunction[n];
        final double coefficients[] = new double[4];
        for (int i = 1; i <= n; i++)
        {
            // ### coefficients[0] = a[i];
            // ### coefficients[1] = b[i];
            // ### coefficients[2] = c[i];
            // ### coefficients[3] = d[i];
            // ### final double x0 = x[i - 1];
            polynomials[i - 1] = new PolynomialFunction(coefficients);
        }

        return new PolynomialSplineFunction(x, polynomials);
    }

    /*
     *  Splines a la Forsythe Malcolm and Moler
     *  ---------------------------------------
     *  In this case the end-conditions are determined by fitting
     *  cubic polynomials to the first and last 4 points and matching
     *  the third derivitives of the spline at the end-points to the
     *  third derivatives of these cubics at the end-points.
     */
    private void fmm_spline(double[] x, double[] y, double[] b, double[] c, double[] d)
    {
        int n = x.length;

        if (n < 2)
            throw new IllegalArgumentException();

        if (n < 3)
        {
            double t = (y[1] - y[0]);
            b[0] = t / (x[1] - x[0]);
            b[1] = b[0];
            c[0] = c[1] = d[0] = d[1] = 0.0;
            return;
        }

        final int nm1 = n - 1;
        int i;

        /* Set up tridiagonal system */
        /* b = diagonal, d = offdiagonal, c = right hand side */

        d[0] = x[1] - x[0];
        c[1] = (y[1] - y[0]) / d[0];/* = +/- Inf  for x[1]=x[2] -- problem? */
        for (i = 1; i < n - 1; i++)
        {
            d[i] = x[i + 1] - x[i];
            b[i] = 2.0 * (d[i - 1] + d[i]);
            c[i + 1] = (y[i + 1] - y[i]) / d[i];
            c[i] = c[i + 1] - c[i];
        }

        /* End conditions. */
        /* Third derivatives at x[0] and x[n-1] obtained */
        /* from divided differences */

        b[0] = -d[0];
        b[n - 1] = -d[nm1 - 1];
        c[0] = c[n - 1] = 0.0;
        if (n > 3)
        {
            c[0] = c[2] / (x[3] - x[1]) - c[1] / (x[2] - x[0]);
            c[n - 1] = c[nm1 - 1] / (x[n - 1] - x[n - 3]) - c[n - 3] / (x[nm1 - 1] - x[n - 4]);
            c[0] = c[0] * d[0] * d[0] / (x[3] - x[0]);
            c[n - 1] = -c[n - 1] * d[nm1 - 1] * d[nm1 - 1] / (x[n - 1] - x[n - 4]);
        }

        /* Gaussian elimination */

        for (i = 1; i <= n - 1; i++)
        {
            double t = d[i - 1] / b[i - 1];
            b[i] = b[i] - t * d[i - 1];
            c[i] = c[i] - t * c[i - 1];
        }

        /* Backward substitution */

        c[n - 1] = c[n - 1] / b[n - 1];
        for (i = nm1 - 1; i >= 0; i--)
            c[i] = (c[i] - d[i] * c[i + 1]) / b[i];

        /* c[i] is now the sigma[i-1] of the text */
        /* Compute polynomial coefficients */

        b[n - 1] = (y[n - 1] - y[n - 2]) / d[n - 2] + d[n - 2] * (c[n - 2] + 2.0 * c[n - 1]);
        for (i = 0; i <= nm1 - 1; i++)
        {
            b[i] = (y[i + 1] - y[i]) / d[i] - d[i] * (c[i + 1] + 2.0 * c[i]);
            d[i] = (c[i + 1] - c[i]) / d[i];
            c[i] = 3.0 * c[i];
        }
        c[n - 1] = 3.0 * c[n - 1];
        d[n - 1] = d[nm1 - 1];
    }
}
