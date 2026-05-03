package rtss.math.algorithms;

import rtss.util.Util;

public class TestMathUtil
{
    public static void main(String[] args)
    {
        try
        {
            new TestMathUtil().test();
        }
        catch (Exception ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private void test()
    {
        double av1 = MathUtil.log_average(1000.0, 2000.0);
        double av2 = log_average_naive(1000, 1000.0, 2000.0);
        Util.noop();
    }

    private double log_average_naive(int npoints, double v1, double v2)
    {
        if (v1 == v2)
            return v1;

        double a = Math.pow(v2 / v1, 1.0 / (npoints - 1));

        // Double b = v1 * Math.pow(a, npoints - 1);

        double sum = 0;

        for (int k = 0; k < npoints; k++)
        {
            double v = Math.pow(a, k);
            sum += v;
        }
        
        sum /= npoints;

        return v1 * sum;
    }
}
