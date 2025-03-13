package rtss.data.curves;

import rtss.util.Util;

/*
 * Деформировать "носик" кривой l(x) с дневным разрешением в пределах первого года.
 * Использовать полином 4-й степени.
 */
public class SculptDailyLX
{
    public static double[] scultDailyLX(double[] f, double convexity) throws Exception
    {
        int x1 = 365;
        
        // Clone the original array
        double[] fClone = f.clone();

        // Boundary conditions
        double f0 = f[0]; // f(0)
        double fx1 = f[x1]; // f(x1)
        double dfx1 = (f[x1 + 1] - f[x1]) / 1.0; // Approximate f'(x1)
        double d2fx1 = (f[x1 + 1] - 2 * f[x1] + f[x1 - 1]) / 1.0; // Approximate f''(x1)

        // Coefficients of the quartic polynomial f(x) = ax^4 + bx^3 + cx^2 + dx + e
        double e = f0; // From f(0) = f0

        // Solve the system of equations for a, b, c, d:
        // 1. a*x1^4 + b*x1^3 + c*x1^2 + d*x1 + e = fx1
        // 2. 4a*x1^3 + 3b*x1^2 + 2c*x1 + d = dfx1
        // 3. 12a*x1^2 + 6b*x1 + 2c = d2fx1
        // 4. a = convexityControl (extra condition to control convexity)

        double a = convexity; // Control parameter for convexity

        // Substitute a into the equations and solve for b, c, d
        double[][] A = {
                         { x1 * x1 * x1, x1 * x1, x1 },
                         { 3 * x1 * x1, 2 * x1, 1 },
                         { 6 * x1, 2, 0 }
        };

        double[] B = {
                       fx1 - e - a * x1 * x1 * x1 * x1,
                       dfx1 - 4 * a * x1 * x1 * x1,
                       d2fx1 - 12 * a * x1 * x1
        };

        // Solve the system Ax = B for x = [b, c, d]
        double[] coefficients = solveLinearSystem(A, B);
        double b = coefficients[0];
        double c = coefficients[1];
        double d = coefficients[2];

        // Fill in the missing values
        for (int x = 1; x < x1; x++)
        {
            fClone[x] = a * x * x * x * x + b * x * x * x + c * x * x + d * x + e;
        }
        
        validate(Util.splice(fClone, 0, x1 + 100), convexity);

        return fClone;
    }

    // Helper method to solve a 3x3 linear system Ax = B
    private static double[] solveLinearSystem(double[][] A, double[] B)
    {
        int n = B.length;
        double[] x = new double[n];

        // Using Gaussian elimination
        for (int i = 0; i < n; i++)
        {
            // Pivot for the current row
            double pivot = A[i][i];
            for (int j = i + 1; j < n; j++)
            {
                double factor = A[j][i] / pivot;
                B[j] -= factor * B[i];
                for (int k = i; k < n; k++)
                {
                    A[j][k] -= factor * A[i][k];
                }
            }
        }

        // Back substitution
        for (int i = n - 1; i >= 0; i--)
        {
            x[i] = B[i];
            for (int j = i + 1; j < n; j++)
            {
                x[i] -= A[i][j] * x[j];
            }
            x[i] /= A[i][i];
        }

        return x;
    }
    
    private static void validate(double[] f, double convexity) throws Exception
    {
        double[] d1 = derivative(f);
        double[] d2 = derivative(d1);

        if (!Util.isMonotonicallyDecreasing(f, true))
            throw new Exception("Improperly scuplted lx (non-decreasing)");
        
        if (!Util.isNegative(d1))
            throw new Exception("Improperly scuplted lx (non-negative d1)");
        
        if (convexity >= 0 && !Util.isPositive(d2))
            throw new Exception("Improperly scuplted lx (non-positive d2)");
    }

    private static double[] derivative(double[] p)
    {
        if (p.length <= 1)
            return new double[0];

        double[] d = new double[p.length - 1];
        for (int i = 0; i <= p.length - 2; i++)
            d[i] = p[i + 1] - p[i];
        return d;
    }

    @SuppressWarnings("unused")
    private static double[] d2(double[] p)
    {
        return derivative(derivative(p));
    }
}
