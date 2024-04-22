package rtss.math.algorithms;

import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 * Search function local extrema using Golden Section search algorithm
 * 
 * https://en.wikipedia.org/wiki/Golden-section_search
 */
public class GoldenSectionSearch
{
    public static final double invphi = (Math.sqrt(5.0) - 1) / 2.0;
    public static final double invphi2 = (3 - Math.sqrt(5.0)) / 2.0;

    /*
     * Returns subinterval of [a,b] containing minimum of f
     */
    public static double[] find_min(UnivariateFunction f, double a, double b, double tol)
    {
        return find_min(f, a, b, tol, b - a, true, 0, 0, true, 0, 0);
    }

    private static double[] find_min(UnivariateFunction f, double a, double b, double tol,
            double h, boolean noC, double c, double fc,
            boolean noD, double d, double fd)
    {
        if (Math.abs(h) <= tol)
        {
            return new double[] { a, b };
        }

        if (noC)
        {
            c = a + invphi2 * h;
            fc = f.value(c);
        }
        
        if (noD)
        {
            d = a + invphi * h;
            fd = f.value(d);
        }
        
        if (fc < fd)
        {
            return find_min(f, a, d, tol, h * invphi, true, 0, 0, false, c, fc);
        }
        else
        {
            return find_min(f, c, b, tol, h * invphi, false, d, fd, true, 0, 0);
        }
    }
    
    /*
     * Returns subinterval of [a,b] containing maximum of f
     */
    public static double[] find_max(UnivariateFunction f, double a, double b, double tol)
    {
        return find_max(f, a, b, tol, b - a, true, 0, 0, true, 0, 0);
    }

    private static double[] find_max(UnivariateFunction f, double a, double b, double tol,
            double h, boolean noC, double c, double fc,
            boolean noD, double d, double fd)
    {
        if (Math.abs(h) <= tol)
        {
            return new double[] { a, b };
        }

        if (noC)
        {
            c = a + invphi2 * h;
            fc = f.value(c);
        }
        
        if (noD)
        {
            d = a + invphi * h;
            fd = f.value(d);
        }
        
        if (fc > fd)
        {
            return find_max(f, a, d, tol, h * invphi, true, 0, 0, false, c, fc);
        }
        else
        {
            return find_max(f, c, b, tol, h * invphi, false, d, fd, true, 0, 0);
        }
    }
}
