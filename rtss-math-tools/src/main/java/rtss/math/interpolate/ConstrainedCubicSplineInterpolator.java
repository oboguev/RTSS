package rtss.math.interpolate;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.exception.NonMonotonicSequenceException;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.util.MathArrays;

import rtss.util.Util;

/**
 * Constrained Spline Interpolation algorithm (CJC Kruger).
 * Created by Anton Danshin
 * 
 * https://numxl.com/blogs/interpolation-101
 * https://jetcracker.wordpress.com/2014/12/26/constrained-cubic-spline-java
 * https://web.archive.org/web/20210330154026/https://jetcracker.wordpress.com/2014/12/26/constrained-cubic-spline-java/
 * https://pages.uoregon.edu/dgavin/software/spline.pdf
 * https://web.archive.org/web/20070307145036/http://www.korf.co.uk/spline.pdf
 */
public class ConstrainedCubicSplineInterpolator implements UnivariateInterpolator
{
    @Override
    public PolynomialSplineFunction interpolate(double x[], double y[])
            throws DimensionMismatchException, NumberIsTooSmallException, NonMonotonicSequenceException
    {
        try
        {
            Map<String, Object> params = null;
            return interpolate(x, y, params);
        }
        catch (MathIllegalArgumentException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex.getLocalizedMessage(), ex);
        }
    }

    public PolynomialSplineFunction interpolate(double x[], double y[], Map<String, Object> params) throws Exception
    {
        if (params == null)
            params = new HashMap<>();

        if (x.length != y.length)
            throw new DimensionMismatchException(x.length, y.length);

        if (x.length < 3)
            throw new NumberIsTooSmallException(LocalizedFormats.NUMBER_OF_POINTS, x.length, 3, true);

        // Number of intervals.  The number of data points is n + 1.
        final int n = x.length - 1;

        MathArrays.checkOrder(x);

        // Differences between knot points
        final double dx[] = new double[n];
        final double dy[] = new double[n];
        for (int i = 0; i < n; i++)
        {
            dx[i] = x[i + 1] - x[i];
            dy[i] = y[i + 1] - y[i];
        }

        final double f1[] = new double[n + 1]; // F'(x[i])

        for (int i = 1; i < n; i++)
        {
            double slope = dy[i - 1] * dy[i];
            if (slope > 0)
            {
                // doesn't change sign
                f1[i] = 2 / (dx[i] / dy[i] + dx[i - 1] / dy[i - 1]);
            }
            else if (slope <= 0)
            {
                // changes sign
                f1[i] = 0;
            }
        }

        f1[0] = 3 * dy[0] / (2 * (dx[0])) - f1[1] / 2;
        f1[n] = 3 * dy[n - 1] / (2 * (dx[n - 1])) - f1[n - 1] / 2;

        if (params.containsKey("f1.0"))
            f1[0] = (Double) params.get("f1.0");
        if (params.containsKey("f1.n"))
            f1[n] = (Double) params.get("f1.n");

        // cubic spline coefficients -- a contains constants, b is linear, c quadratic, d is cubic
        final double a[] = new double[n + 1];
        final double b[] = new double[n + 1];
        final double c[] = new double[n + 1];
        final double d[] = new double[n + 1];

        for (int i = 1; i <= n; i++)
        {
            final double f2a = -2 * (f1[i] + 2 * f1[i - 1]) / dx[i - 1] + 6 * dy[i - 1] / (dx[i - 1] * dx[i - 1]);
            final double f2b = 2 * (2 * f1[i] + f1[i - 1]) / dx[i - 1] - 6 * dy[i - 1] / (dx[i - 1] * dx[i - 1]);

            d[i] = (f2b - f2a) / (6 * dx[i - 1]);
            c[i] = (x[i] * f2a - x[i - 1] * f2b) / (2 * dx[i - 1]);

            /*
             * Adjust c and d so that s'' has a desired sign
             */
            FilterF2 ff2 = new FilterF2(params, c[i], d[i], dx[i - 1], i - 1);
            ff2.coerce();
            c[i] = ff2.c;
            d[i] = ff2.d;

            b[i] = (dy[i - 1] -
                    c[i] * (x[i] * x[i] - x[i - 1] * x[i - 1]) -
                    d[i] * (x[i] * x[i] * x[i] - x[i - 1] * x[i - 1] * x[i - 1]))
                   / dx[i - 1];

            a[i] = y[i - 1] - b[i] * x[i - 1] - c[i] * x[i - 1] * x[i - 1] - d[i] * x[i - 1] * x[i - 1] * x[i - 1];
        }

        final PolynomialFunction polynomials[] = new PolynomialFunction[n];
        final double coefficients[] = new double[4];
        for (int i = 1; i <= n; i++)
        {
            coefficients[0] = a[i];
            coefficients[1] = b[i];
            coefficients[2] = c[i];
            coefficients[3] = d[i];
            final double x0 = x[i - 1];
            Util.noop(); // ####
            polynomials[i - 1] = new PolynomialFunction(coefficients)
            {
                private static final long serialVersionUID = 1L;

                @Override
                public double value(double x)
                {
                    // bypass the standard Apache Commons behavior
                    return super.value(x + x0);
                }
            };
        }

        PolynomialSplineFunction sp = new PolynomialSplineFunction(x, polynomials);

        // self-test
        for (int k = 0; k < x.length; k++)
        {
            double v = sp.value(x[k]);
            if (Util.differ(v, y[k]))
                throw new Exception("Spline self-test failed");
        }

        return sp;
    }
    
    /* ====================================================================================================== */

    /*
     * When an U-shape distribution is interpolated in a mean-preserving way (MeanPreservingIntegralSpline), 
     * bins are transformed into a cumulative sum binset, the result is interpolated and then a derivative is taken.
     * Thus, resultant function is a derivative of spline: f = s'.
     * 
     * To enforce the produced curve (f) being monotonic (i.e. descending in the left part of U-shape and ascending in the right part), 
     * we need to make sure that in the left part f' < 0 and in the right part f' > 0. In the minimum bin or bins, where the
     * inflection happens, f' can be any value. 
     * 
     * f' < 0 means s'' < 0
     * f' > 0 means s'' > 0
     * 
     * s = a + b * dx + c * dx^2 + d *dx^3
     * s' = b + c * dx + d *dx^2
     * s'' = c + d * dx
     * 
     * We need accordingly ensure that:
     *      in descending segments (c + d * x) < 0 in the whole range of x = [0...dx].  
     *      in ascending segments (c + d * x) > 0 in the whole range of x = [0...dx].  
     */
    public static class FilterF2
    {
        public double c;
        public double d;
        private int sign;
        private double dx;
        boolean active = false;

        public FilterF2(Map<String, Object> params, double c, double d, double dx, int iSeg)
        {
            this.c = c;
            this.d = d;
            this.dx = dx;

            int[] signs = (int[]) params.get("f2.sign");
            if (signs == null)
                return;

            this.sign = signs[iSeg];
            if (sign == 0)
                return;

            active = true;
        }

        public void coerce()
        {
            if (active)
            {
                double v1 = c;
                double v2 = c + d * dx;

                if (sign < 0)
                {
                    if (v1 <= 0 && v2 <= 0)
                    {
                        // do nothing
                    }
                    else
                    {
                        coerce_negative();
                    }
                }
                else if (sign > 0)
                {
                    if (v1 >= 0 && v2 >= 0)
                    {
                        // do nothing
                    }
                    else
                    {
                        coerce_positive();
                    }
                }
            }
        }
        
        private void coerce_negative()
        {
            Util.noop();
            // ###
        }

        private void coerce_positive()
        {
            Util.noop();
            // ###
        }
    }
}
