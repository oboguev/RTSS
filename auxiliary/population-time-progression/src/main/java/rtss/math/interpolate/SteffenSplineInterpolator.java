package rtss.math.interpolate;

import static java.lang.Math.abs;
import static java.lang.Math.min;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.util.MathArrays;

/**
 * Steffen spline interpolator.
 * Monotonicity is preserved and the first derivative is continuous.
 * Guaranties monotonicity of the cubic function between two neighboring data points.
 * However, Steffen spline may have issues recovering peak and trough values by interpolating monotonic curves between each interval.
 * 
 * M. Steffen, "A Simple Method for Monotonic Interpolation in One Dimension" // Astronomy and Astrophysics, 1990 (Vol. 239, NOV (II)), pp. 443-450 
 */
public class SteffenSplineInterpolator
{
    public PolynomialSplineFunction interpolate(double[] cp_x, double[] cp_y)
    {
        if (cp_x == null || cp_y == null)
                throw new NullArgumentException();

        if (cp_x.length != cp_y.length)
                throw new DimensionMismatchException(cp_x.length, cp_y.length);
        
        MathArrays.checkOrder(cp_x);
        
        int n = cp_x.length;

        // eqs 2-5 of paper
        double[] a = new double[n];
        double[] b = new double[n];
        double[] c = new double[n];
        double[] d = new double[n];

        // eq 11 of paper
        double[] y_prime = new double[n];

        /*
         * first assign the interval and slopes for the left boundary.
         * We use the "simplest possibility" method described in the paper
         * in section 2.2
         */
        double h0 = (cp_x[1] - cp_x[0]);
        double s0 = (cp_y[1] - cp_y[0]) / h0;

        y_prime[0] = s0;

        /* 
         * Now we calculate all the necessary s, h, p, and y' variables
         * from 1 to N-2 (0 to size - 2 inclusive) 
         */
        for (int i = 1; i < n - 1; i++)
        {
            double pi;

            /* equation 6 in the paper */
            double hi = (cp_x[i + 1] - cp_x[i]);
            double him1 = (cp_x[i] - cp_x[i - 1]);

            /* equation 7 in the paper */
            double si = (cp_y[i + 1] - cp_y[i]) / hi;
            double sim1 = (cp_y[i] - cp_y[i - 1]) / him1;

            /* equation 8 in the paper */
            pi = (sim1 * hi + si * him1) / (him1 + hi);

            /* This is a Java equivalent of the FORTRAN statement below eqn 11 */
            y_prime[i] = (copysign(1.0, sim1) + copysign(1.0, si)) * min(abs(sim1), min(abs(si), 0.5 * abs(pi)));
        }

        /*
         * we also need y' for the rightmost boundary; we use the
         * "simplest possibility" method described in the paper in
         * section 2.2
         *
         * y' = s_{n-1}
         */
        y_prime[n - 1] = (cp_y[n - 1] - cp_y[n - 2]) /
                         (cp_x[n - 1] - cp_x[n - 2]);

        /* Now we can calculate all the coefficients for the whole range */
        for (int i = 0; i < n - 1; i++)
        {
            double hi = (cp_x[i + 1] - cp_x[i]);
            double si = (cp_y[i + 1] - cp_y[i]) / hi;

            /* These are from equations 2-5 in the paper */
            a[i] = (y_prime[i] + y_prime[i + 1] - 2 * si) / hi / hi;
            b[i] = (3 * si - 2 * y_prime[i] - y_prime[i + 1]) / hi;
            c[i] = y_prime[i];
            d[i] = cp_y[i];
        }
        
        PolynomialFunction polynomials[] = new PolynomialFunction[n - 1];
        for (int i = 0; i < n - 1; i++)
        {
            double[] coefficients = new double[4];
            coefficients[0] = d[i];
            coefficients[1] = c[i];
            coefficients[2] = b[i];
            coefficients[3] = a[i];
            polynomials[i] = new PolynomialFunction(coefficients);
        }

        return new PolynomialSplineFunction(cp_x, polynomials);
    }

    // copy the sign of y to x
    private double copysign(double x, double y)
    {
        if (x < 0 && y > 0 || x > 0 && y < 0)
            return -x;
        else
            return x;
    }
}
