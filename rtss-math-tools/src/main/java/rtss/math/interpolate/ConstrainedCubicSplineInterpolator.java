package rtss.math.interpolate;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.exception.NoDataException;
import org.apache.commons.math3.exception.NonMonotonicSequenceException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.util.MathArrays;

import rtss.data.curves.CurveSegmentTrend;
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

        F2SignFilter prev_ff2 = null;

        for (int i = 1; i <= n; i++)
        {
            final double f2a = -2 * (f1[i] + 2 * f1[i - 1]) / dx[i - 1] + 6 * dy[i - 1] / (dx[i - 1] * dx[i - 1]);
            final double f2b = 2 * (2 * f1[i] + f1[i - 1]) / dx[i - 1] - 6 * dy[i - 1] / (dx[i - 1] * dx[i - 1]);

            d[i] = (f2b - f2a) / (6 * dx[i - 1]);
            c[i] = (x[i] * f2a - x[i - 1] * f2b) / (2 * dx[i - 1]);

            /*
             * Adjust c and d so that s'' has a desired sign
             */
            F2SignFilter ff2 = new F2SignFilter(prev_ff2, params, c[i], d[i], x[i - 1], dx[i - 1], i - 1);
            ff2.coerce();
            c[i] = ff2.c;
            d[i] = ff2.d;
            prev_ff2 = ff2;

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
            polynomials[i - 1] = new ConstrainedCubicSplinePolynomialFunction(coefficients, x0, x[i]);
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

    public static class ConstrainedCubicSplinePolynomialFunction extends PolynomialFunction
    {
        private static final long serialVersionUID = 1L;

        private final double x0;
        private final double x1;
        private final double a;
        private final double b;
        private final double c;
        private final double d;

        public ConstrainedCubicSplinePolynomialFunction(double[] coefficients, double x0, double x1) throws NullArgumentException, NoDataException
        {
            super(coefficients);
            this.x0 = x0;
            this.x1 = x1;
            this.a = coefficients[0];
            this.b = coefficients[1];
            this.c = coefficients[2];
            this.d = coefficients[3];
        }

        @Override
        public double value(double x)
        {
            // bypass the standard Apache Commons behavior
            return super.value(x + x0);
        }

        @Override
        public PolynomialFunction polynomialDerivative()
        {
            return new ConstrainedCubicSplinePolynomialFunction(differentiate(getCoefficients()), x0, x1);
        }

        public double f(double x)
        {
            return a + (b * x) + (c * x * x) + (d * x * x * x);
        }

        public double f1(double x)
        {
            return b + (2 * c * x ) + (3 * d * x * x);
        }

        public double f2(double x)
        {
            return (2 * c) + (6 * d * x);
        }
    }

    public static PolynomialSplineFunction derivative(UnivariateFunction f)
    {
        if (f instanceof PolynomialSplineFunction)
            return derivative((PolynomialSplineFunction) f);
        else
            throw new IllegalArgumentException();
    }

    public static PolynomialSplineFunction derivative(PolynomialSplineFunction f)
    {
        PolynomialFunction polynomials[] = new PolynomialFunction[f.getPolynomials().length];
        int k = 0;

        for (PolynomialFunction pf : f.getPolynomials())
        {
            if (pf instanceof ConstrainedCubicSplinePolynomialFunction)
                polynomials[k++] = ((ConstrainedCubicSplinePolynomialFunction) pf).polynomialDerivative();
            else
                throw new IllegalArgumentException();
        }

        return new PolynomialSplineFunction(f.getKnots(), polynomials);
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
     * s = a + b * x + c * x^2 + d * x^3
     * s' = b + 2 * c * x + 3 * d * x^2
     * s'' = 2 * c + 6 * d * x
     * 
     * We need accordingly ensure that:
     *      in descending segments s'' < 0 in the whole range of x = [x0 ... x1].  
     *      in ascending segments s'' > 0 in the whole range of x = [x0 ... x1].  
     *      in min segment s'' < 0 on the left side of the segment and > 0 on the right side of the segment  
     */
    public static class F2SignFilter
    {
        private F2SignFilter prev;
        public double c, c_initial;
        public double d, d_initial;
        private CurveSegmentTrend trend;
        private double x0;
        private double dx;
        private double x1;
        private String title;

        private double v1, v1_initial, v2, v2_initial;

        public F2SignFilter(F2SignFilter prev, Map<String, Object> params, double c, double d, double x0, double dx, int iSeg)
        {
            this.prev = prev;
            this.c_initial = this.c = c;
            this.d_initial = this.d = d;
            this.x0 = x0;
            this.dx = dx;
            this.x1 = x0 + dx;

            v1_initial = v1 = 2 * c + 6 * d * x0;
            v2_initial = v2 = 2 * c + 6 * d * x1;

            CurveSegmentTrend[] trends = (CurveSegmentTrend[]) params.get("f2.trends");
            if (trends == null)
            {
                trend = CurveSegmentTrend.NEUTRAL;
            }
            else
            {
                this.trend = trends[iSeg];
            }

            title = (String) params.get("title");
            if (title == null)
                title = "unnamed";
        }

        private void reeval_cd()
        {
            d = (v2 - v1) / (6 * dx);
            c = (v1 - 6 * d * x0) / 2;
        }

        public void coerce() throws Exception
        {
            switch (trend)
            {
            case DOWN:
                coerce_negative();
                break;

            case UP:
                coerce_positive();
                break;

            case MIN:
                coerce_min();
                break;

            case MIN1:
                // coerce_min1();
                break;

            case MIN2:
                // coerce_min2();
                break;

            case NEUTRAL:
            default:
                return;
            }

            reeval_cd();
            if (Util.True && (Util.differ(c, c_initial) || Util.differ(d, d_initial)))
                Util.out(String.format("Adjusted %s %s-%s %s: [%.3f]-[%.3f] => [%.3f]-[%.3f]", 
                                       title, f2s(x0), f2s(x1 - 1.0), trend.name(), v1_initial, v2_initial, v1, v2));
            Util.noop();
        }

        private void coerce_negative()
        {
            double vmin = Math.min(v1, v2);
            double vmt = 0.2 * vmin; // min target

            switch (sign(v1) + sign(v2))
            {
            case "++":
            case "00":
            case "0+":
            case "+0":
                // not much we can do
                return;

            case "0-":
            case "-0":
            case "--":
                // nearly ok, but tweak a bit: make one end to be at least 20% of the other
                if (v1 > vmt)
                    v1 = vmt;
                if (v2 > vmt)
                    v2 = vmt;
                return;

            case "+-":
                v1 = 0.2 * v2;
                return;

            case "-+":
                v2 = 0.2 * v1;
                return;
            }
        }

        private void coerce_positive()
        {
            double vmax = Math.max(v1, v2);
            double vmt = 0.2 * vmax; // min target

            switch (sign(v1) + sign(v2))
            {
            case "--":
            case "00":
            case "0-":
            case "-0":
                // not much we can do
                return;

            case "0+":
            case "+0":
            case "++":
                // nearly ok, but tweak a bit: make one end to be at least 20% of the other
                if (v1 < vmt)
                    v1 = vmt;
                if (v2 < vmt)
                    v2 = vmt;
                return;

            case "-+":
                v1 = 0.2 * v2;
                return;

            case "+-":
                v2 = 0.2 * v1;
                return;
            }
        }

        private void coerce_min()
        {
            if (prev != null && prev.v2 <= 0)
                v1 = Math.min(v1, prev.v2 / 2);
            if (v1 < 0)
                v2 = Math.max(v2, -v1 / 4);
        }

        private void coerce_min1()
        {
            if (prev != null && prev.v2 <= 0)
                v1 = Math.min(v1, prev.v2 / 2);
        }

        private void coerce_min2()
        {
            // ###
        }

        private String sign(double v)
        {
            if (v < 0)
                return "-";
            else if (v > 0)
                return "+";
            else
                return "0";
        }

        private String f2s(double f) throws Exception
        {
            return Util.f2s(f);
        }
    }
}
