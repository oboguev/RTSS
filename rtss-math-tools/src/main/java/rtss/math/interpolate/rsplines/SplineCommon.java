package rtss.math.interpolate.rsplines;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;

import rtss.util.Util;

import static java.lang.Math.IEEEremainder;

/**
 * Derived from R system source code,
 * with the original files under GPL 2 (or later GPL versions)
 * (package: stats, files: spline.R splinefun.R splines.c)
 */
class Common
{
    static enum SplineMethod
    {
        PERIODIC, // 1
        NATURAL, // 2
        FMM, // 3
        HYMAN // 5
    }

    static class Coefficients
    {
        double[] x;
        double[] y;
        double[] b;
        double[] c;
        double[] d;

        public Coefficients(double[] x, double[] y)
        {
            this.x = x;
            this.y = y;
            this.b = new double[x.length];
            this.d = new double[x.length];
            this.d = new double[x.length];
            // Arrays.fill(b, 0);
            // Arrays.fill(c, 0);
            // Arrays.fill(d, 0);
        }
    }

    protected Coefficients makeCoefficients(SplineMethod method, double[] x, double[] y)
    {
        if (x.length < 2 || y.length != x.length)
            throw new IllegalArgumentException();

        verifyIncreasing(x);

        Coefficients cf = new Coefficients(x, y);

        if (method == SplineMethod.HYMAN)
        {
            verifyMonotone(y);
            spline_coef(SplineMethod.FMM, cf.x, cf.y, cf.b, cf.c, cf.d);
            hyman_filter(cf);
            spl_coef_conv(cf);
        }
        else
        {
            spline_coef(method, cf.x, cf.y, cf.b, cf.c, cf.d);
        }

        return cf;
    }

    /*
     * (x, y, b, c, d) should have the same length
     */
    protected void spline_coef(
            SplineMethod method,
            double[] x, double[] y,
            double[] b, double[] c, double[] d) throws IllegalArgumentException
    {
        if (x.length < 2 ||
            y.length != x.length ||
            b.length != x.length ||
            c.length != x.length ||
            d.length != x.length)
            throw new IllegalArgumentException();

        switch (method)
        {
        case PERIODIC:
            periodic_spline(x, y, b, c, d);
            break;

        case NATURAL:
            natural_spline(x, y, b, c, d);
            break;

        case FMM:
            fmm_spline(x, y, b, c, d);
            break;

        default:
            throw new IllegalArgumentException();
        }
    }

    void spline_eval(SplineMethod method,
            int nu, double[] u, double[] v,
            int n, double[] x, double[] y, double[] b, double[] c, double[] d)
    {
        /* 
         * Evaluate  v[l] := spline(u[l], ...),     l = 1,..,nu, i.e. 0:(nu-1)
         * Nodes x[i], coef (y[i]; b[i],c[i],d[i]); i = 1,..,n , i.e. 0:(*n-1)
         */
        final int n_1 = n - 1;
        int i, l;
        double dx;

        if (method == SplineMethod.PERIODIC && n > 1)
        {
            /* periodic */
            dx = x[n_1] - x[0];
            for (l = 0; l < nu; l++)
            {
                v[l] = IEEEremainder(u[l] - x[0], dx);
                if (v[l] < 0.0)
                    v[l] += dx;
                v[l] += x[0];
            }
        }
        else
        {
            for (l = 0; l < nu; l++)
                v[l] = u[l];
        }

        for (l = 0, i = 0; l < nu; l++)
        {
            double ul = v[l];
            if (ul < x[i] || (i < n_1 && x[i + 1] < ul))
            {
                /* 
                 * reset i  such that  x[i] <= ul <= x[i+1] 
                 */
                i = 0;
                int j = n;
                do
                {
                    int k = (i + j) / 2;
                    if (ul < x[k])
                        j = k;
                    else
                        i = k;
                }
                while (j > i + 1);
            }
            dx = ul - x[i];

            /* for natural splines extrapolate linearly left */
            double tmp = (method == SplineMethod.NATURAL && ul < x[0]) ? 0.0 : d[i];

            v[l] = y[i] + dx * (b[i] + dx * (c[i] + dx * tmp));
        }
    }

    /*
     *  Natural Splines
     *  ---------------
     *  Here the end-conditions are determined by setting the second
     *  derivative of the spline at the end-points to equal to zero.
     *
     *  There are n-2 unknowns (y[i]'' at x[2], ..., x[n-1]) and n-2
     *  equations to determine them.  Either Cholesky or Gaussian
     *  elimination could be used.
     */
    private void natural_spline(double[] x, double[] y, double[] b, double[] c, double[] d)
            throws IllegalArgumentException
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

        /* Set up the tridiagonal system */
        /* b = diagonal, d = offdiagonal, c = right hand side */

        d[0] = x[1] - x[0];
        c[1] = (y[1] - y[0]) / d[0];
        for (i = 1; i < n - 1; i++)
        {
            d[i] = x[i + 1] - x[i];
            b[i] = 2.0 * (d[i - 1] + d[i]);
            c[i + 1] = (y[i + 1] - y[i]) / d[i];
            c[i] = c[i + 1] - c[i];
        }

        /* Gaussian elimination */

        for (i = 2; i < n - 1; i++)
        {
            double t = d[i - 1] / b[i - 1];
            b[i] = b[i] - t * d[i - 1];
            c[i] = c[i] - t * c[i - 1];
        }

        /* Backward substitution */

        c[nm1 - 1] = c[nm1 - 1] / b[nm1 - 1];
        for (i = n - 3; i > 0; i--)
            c[i] = (c[i] - d[i] * c[i + 1]) / b[i];

        /* End conditions */

        c[0] = c[n - 1] = 0.0;

        /* Get cubic coefficients */

        b[0] = (y[1] - y[0]) / d[0] - d[i - 1] * c[1];
        c[0] = 0.0;
        d[0] = c[1] / d[0];
        b[n - 1] = (y[n - 1] - y[nm1 - 1]) / d[nm1 - 1] + d[nm1 - 1] * c[nm1 - 1];
        for (i = 1; i < n - 1; i++)
        {
            b[i] = (y[i + 1] - y[i]) / d[i] - d[i] * (c[i + 1] + 2.0 * c[i]);
            d[i] = (c[i + 1] - c[i]) / d[i];
            c[i] = 3.0 * c[i];
        }
        c[n - 1] = 0.0;
        d[n - 1] = 0.0;
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

    /*
     *  Periodic Spline
     *  ---------------
     *  The end conditions here match spline (and its derivatives)
     *  at x[1] and x[n].
     *
     *  Note: There is an explicit check that the user has supplied
     *  data with y[1] equal to y[n].
     */
    protected void periodic_spline(double[] x, double[] y, double[] b, double[] c, double[] d)
            throws IllegalArgumentException
    {
        int n = x.length;
        double[] e = new double[n];

        if (n < 2 || y[0] != y[n - 1])
            throw new IllegalArgumentException();

        if (n == 2)
        {
            b[0] = b[1] = c[0] = c[1] = d[0] = d[1] = 0.0;
            return;
        }
        else if (n == 3)
        {
            b[0] = b[1] = b[2] = -(y[0] - y[1]) * (x[0] - 2 * x[1] + x[2]) / (x[2] - x[1]) / (x[1] - x[0]);
            c[0] = -3 * (y[0] - y[1]) / (x[2] - x[1]) / (x[1] - x[0]);
            c[1] = -c[0];
            c[2] = c[0];
            d[0] = -2 * c[0] / 3 / (x[1] - x[0]);
            d[1] = -d[0] * (x[1] - x[0]) / (x[2] - x[1]);
            d[2] = d[0];
            return;
        }

        /* else --------- n >= 4 --------- */

        double s;
        final int nm1 = n - 1;
        int i;

        /* Set up the matrix system */
        /* A = diagonal  B = off-diagonal  C = rhs */

        double[] A = b;
        double[] B = d;
        double[] C = c;

        B[0] = x[1] - x[0];
        B[nm1 - 1] = x[n - 1] - x[nm1 - 1];
        A[0] = 2.0 * (B[0] + B[nm1 - 1]);
        C[0] = (y[1] - y[0]) / B[0] - (y[n - 1] - y[nm1 - 1]) / B[nm1 - 1];

        for (i = 1; i < n - 1; i++)
        {
            B[i] = x[i + 1] - x[i];
            A[i] = 2.0 * (B[i] + B[i - 1]);
            C[i] = (y[i + 1] - y[i]) / B[i] - (y[i] - y[i - 1]) / B[i - 1];
        }

        /* Cholesky decomposition */

        double[] L = b;
        double[] M = d;
        double[] E = e;

        L[0] = sqrt(A[0]);
        E[0] = (x[n - 1] - x[nm1 - 1]) / L[0];
        s = 0.0;
        for (i = 0; i <= nm1 - 3; i++)
        {
            M[i] = B[i] / L[i];
            if (i != 0)
                E[i] = -E[i - 1] * M[i - 1] / L[i];
            L[i + 1] = sqrt(A[i + 1] - M[i] * M[i]);
            s = s + E[i] * E[i];
        }
        M[nm1 - 2] = (B[nm1 - 2] - E[nm1 - 3] * M[nm1 - 3]) / L[nm1 - 2];
        L[nm1 - 1] = sqrt(A[nm1 - 1] - M[nm1 - 2] * M[nm1 - 2] - s);

        /* Forward Elimination */

        double[] Y = c;
        double[] D = c;

        Y[0] = D[0] / L[0];
        s = 0.0;
        for (i = 1; i <= nm1 - 2; i++)
        {
            Y[i] = (D[i] - M[i - 1] * Y[i - 1]) / L[i];
            s = s + E[i - 1] * Y[i - 1];
        }
        Y[nm1 - 1] = (D[nm1 - 1] - M[nm1 - 2] * Y[nm1 - 2] - s) / L[nm1 - 1];

        double[] X = c;

        X[nm1 - 1] = Y[nm1 - 1] / L[nm1 - 1];
        X[nm1 - 2] = (Y[nm1 - 2] - M[nm1 - 2] * X[nm1 - 1]) / L[nm1 - 2];
        for (i = nm1 - 3; i >= 0; i--)
            X[i] = (Y[i] - M[i] * X[i + 1] - E[i] * X[nm1]) / L[i];

        /* Wrap around */

        X[n - 1] = X[0];

        /* Compute polynomial coefficients */

        for (i = 0; i <= nm1 - 1; i++)
        {
            s = x[i + 1] - x[i];
            b[i] = (y[i + 1] - y[i]) / s - s * (c[i + 1] + 2.0 * c[i]);
            d[i] = (c[i + 1] - c[i]) / s;
            c[i] = 3.0 * c[i];
        }
        b[n - 1] = b[0];
        c[n - 1] = c[0];
        d[n - 1] = d[0];
    }

    /*
     * Takes an object z containing equal-length vectors z.x, z.y, z.b, z.c, z.d 
     * defining a cubic spline interpolating z.x, z.y 
     * and forces z.c and z.d to be consistent with z.y and z.b (gradient of spline). 
     * 
     * This is intended for use in conjunction with Hyman's monotonicity filter.
     * Note that R's spline routine has s''(x)/2 as c and s'''(x)/6 as d.
     */
    protected void spl_coef_conv(Coefficients z)
    {
        double[] h = diff(z.x);
        double[] yy = multiply(diff(z.y), -1);
        double[] b0 = Util.splice(z.b, 0, z.b.length - 2);
        double[] b1 = Util.splice(z.b, 1, z.b.length - 1);

        double[] cc = new double[b0.length];
        double[] dd = new double[b0.length];
        for (int k = 0; k < cc.length; k++)
        {
            cc[k] = -(3 * yy[k] + (2 * b0[k] + b1[k]) * h[k]) / pow2(h[k]);
            dd[k] = (2 * yy[k] / h[k] + b0[k] + b1[k]) / pow2(h[k]);
        }

        int n = cc.length;
        double c1 = (3 * yy[n - 1] + (b0[n - 1] + 2 * b1[n - 1]) * h[n - 1]) / pow2(h[n - 1]);
        z.c = concat(cc, c1);
        z.d = concat(dd, dd[n - 1]);
    }

    /*
     * Filters cubic spline function to yield co-monotonicity in accordance
     * with Hyman (1983) SIAM J. Sci. Stat. Comput. 4(4):645-654, 
     * 
     * z.x is knot position
     * z.y is value at knot
     * z.b is gradient at knot
     * 
     * See also Dougherty, Edelman and Hyman 1989 Mathematics of Computation 52:471-494.
     */
    protected void hyman_filter(Coefficients z)
    {
        double[] ss = div(diff(z.y), diff(z.x));
        double[] s0 = concat(ss[0], ss);
        double[] s1 = concat(ss, ss[ss.length - 1]);
        double[] t1 = pmin(abs(s0), abs(s1));
        double[] sig = Util.dup(z.b);
        for (int k = 0; k < s0.length; k++)
        {
            if (s0[k] * s1[k] > 0)
                sig[k] = s1[k];
        }

        double[] b = z.b;

        for (int k = 0; k < s0.length; k++)
        {
            if (sig[k] >= 0)
            {
                b[k] = min(max(0, b[k]), 3 * t1[k]);
            }
            else
            {
                b[k] = max(min(0, b[k]), -3 * t1[k]);

            }
        }
    }

    /*
     * Return an array of stepwise differences between the elements of x:
     *     diff[k] = x[k + 1] - x[k]
     * Returned array is one element shorter than the argument array.    
     */
    protected double[] diff(double[] x)
    {
        double[] d = new double[x.length - 1];
        for (int k = 0; k < d.length; k++)
            d[k] = x[k + 1] - x[k];
        return d;
    }

    /*
     * Return result[k] = a(k) / b(k)
     */
    protected double[] div(double[] a, double[] b)
    {
        if (a.length != b.length)
            throw new IllegalArgumentException();

        double[] d = new double[a.length];
        for (int k = 0; k < d.length; k++)
            d[k] = a[k] / b[k];

        return d;
    }

    /*
     * Return a concatenation of a scalar and appended array
     */
    protected double[] concat(double a, double[] b)
    {
        return concat(new double[] { a }, b);
    }

    /*
     * Return a concatenation of array and appended scalar
     */
    protected double[] concat(double[] a, double b)
    {
        return concat(a, new double[] { b });
    }

    /*
     * Return a concatenation of two arrays
     */
    protected double[] concat(double[] a, double[] b)
    {
        double[] d = new double[a.length + b.length];
        int ix = 0;
        for (int k = 0; k < a.length; k++)
            d[ix++] = a[k];
        for (int k = 0; k < b.length; k++)
            d[ix++] = b[k];
        return d;
    }

    /*
     * Return result[k] = abs(x[k])
     */
    protected double[] abs(double[] x)
    {
        double[] a = new double[x.length];
        for (int k = 0; k < x.length; k++)
            a[k] = Math.abs(x[k]);
        return a;
    }

    /*
     * Return result[k] = v * x[k]
     */
    protected double[] multiply(double[] x, double v)
    {
        double[] a = new double[x.length];
        for (int k = 0; k < x.length; k++)
            a[k] = x[k] * v;
        return a;
    }

    /*
     * Return "parallel minimum": result[k] = min(a[k], b[k])
     */
    protected double[] pmin(double[] a, double[] b)
            throws IllegalArgumentException
    {
        if (a.length != b.length)
            throw new IllegalArgumentException();

        double[] m = new double[a.length];
        for (int k = 0; k < m.length; k++)
            m[k] = min(a[k], b[k]);
        return m;
    }

    /*
     * Return "parallel maximum": result[k] = max(a[k], b[k])
     */
    @SuppressWarnings("unused")
    protected double[] pmax(double[] a, double[] b)
            throws IllegalArgumentException
    {
        if (a.length != b.length)
            throw new IllegalArgumentException();

        double[] m = new double[a.length];
        for (int k = 0; k < m.length; k++)
            m[k] = max(a[k], b[k]);
        return m;
    }

    /*
     * return x^2
     */
    protected double pow2(double x)
    {
        return x * x;
    }

    /*
     * Verify the sequence is strictly increasing
     */
    protected void verifyIncreasing(double[] x)
            throws IllegalArgumentException
    {
        for (double d : diff(x))
        {
            if (d <= 0)
                throw new IllegalArgumentException();
        }
    }

    /*
     * Verify the sequence is monotone: increasing or decreasing,
     * zero-change segments are also ok
     */
    protected void verifyMonotone(double[] y)
            throws IllegalArgumentException
    {
        int sign = 0;

        for (double d : diff(y))
        {
            if (d > 0)
            {
                if (sign == -1)
                    throw new IllegalArgumentException();
                sign = 1;
            }
            else if (d < 0)
            {
                if (sign == 1)
                    throw new IllegalArgumentException();
                sign = -1;
            }
        }
    }
}
