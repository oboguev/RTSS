package rtss.math.algorithms.pclm;

import org.apache.commons.math3.analysis.MultivariateFunction;
// import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.optim.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;
import Jama.Matrix;

/*
 * Penalized Composite Link Model (PCLM)
 * 
 * Developed by DeepSeek
 * https://chat.deepseek.com/a/chat/s/e701734e-b05a-4788-9522-f6f203bc7b47
 * 
 *    Design Matrix (X):  This matrix maps the bins to the knots (intervals between bin edges). 
 *                        Each row corresponds to a bin, and each column corresponds to a knot.
 *                       
 *    Penalty Matrix (D):  This matrix enforces smoothness by penalizing large second differences between adjacent knots.
 *    
 *    Objective Function:  The function to minimize combines the log-likelihood (sum of squared residuals) and the roughness penalty.
 *    
 *    Optimization:        The Nelder-Mead simplex method is used to find the parameters that minimize the objective function.
 *    
 *    For unequal bin widths, the design matrix must account for the proportion of each bin that overlaps with the underlying knots (intervals). 
 *    This requires calculating the fraction of each bin that falls into each knot interval.
 *    But Penalty Matrix (D) remains the same, as it enforces smoothness on the estimated parameters (knots) regardless of bin width.
 *    
 */

public class PCLM
{
    private double[] binCounts; // Observed counts in each bin (N entries)
    private double[] binEdges; // Front edges of the bins (N + 1 entries), the last entry defines the right edge of the last bin
    private double lambda; // Smoothing parameter

    public PCLM(double[] binCounts, double[] binEdges, double lambda)
    {
        this.binCounts = binCounts;
        this.binEdges = binEdges;
        this.lambda = lambda;
    }

    public double[] fit()
    {
        int nBins = binCounts.length;
        int nKnots = binEdges.length - 1;

        // Design matrix for the composite link model (adjusted for unequal bin widths)
        Matrix X = new Matrix(nBins, nKnots);
        for (int i = 0; i < nBins; i++)
        {
            double binStart = binEdges[i];
            double binEnd = binEdges[i + 1];
            double binWidth = binEnd - binStart;

            for (int j = 0; j < nKnots; j++)
            {
                double knotStart = binEdges[j];
                double knotEnd = binEdges[j + 1];

                // Calculate the overlap between the bin and the knot
                double overlapStart = Math.max(binStart, knotStart);
                double overlapEnd = Math.min(binEnd, knotEnd);
                double overlapWidth = Math.max(0, overlapEnd - overlapStart);

                // Set the proportion of the bin that overlaps with the knot
                X.set(i, j, overlapWidth / binWidth);
            }
        }

        // Penalty matrix (second differences)
        Matrix D = new Matrix(nKnots - 2, nKnots);
        for (int i = 0; i < nKnots - 2; i++)
        {
            D.set(i, i, 1.0);
            D.set(i, i + 1, -2.0);
            D.set(i, i + 2, 1.0);
        }

        // Objective function to minimize
        SimplexOptimizer optimizer = new SimplexOptimizer(1e-10, 1e-10);
        PointValuePair result = optimizer.optimize(
                                                   new MaxEval(10000),
                                                   new ObjectiveFunction(new MultivariateFunction()
                                                   {
                                                       @Override
                                                       public double value(double[] params)
                                                       {
                                                           Matrix beta = new Matrix(params, params.length);
                                                           Matrix mu = X.times(beta);
                                                           Matrix residuals = new Matrix(binCounts, binCounts.length).minus(mu);
                                                           double logLikelihood = residuals.normF() * residuals.normF(); // Sum of squares
                                                           double penalty = D.times(beta).normF() * D.times(beta).normF(); // Roughness penalty
                                                           return logLikelihood + lambda * penalty;
                                                       }
                                                   }),
                                                   GoalType.MINIMIZE,
                                                   new InitialGuess(new double[nKnots]),
                                                   new NelderMeadSimplex(nKnots));

        return result.getPoint();
    }

    public static void main(String[] args)
    {
        // Example usage with unequal bin widths
        double[] binCounts = { 10, 20, 30, 40 }; // Observed counts in each bin
        double[] binEdges = { 0, 3, 7, 10, 15 }; // Edges of the bins (unequal widths)
        double lambda = 1.0; // Smoothing parameter

        PCLM pclm = new PCLM(binCounts, binEdges, lambda);
        double[] estimatedParams = pclm.fit();

        System.out.println("Estimated Parameters:");
        for (double param : estimatedParams)
        {
            System.out.println(param);
        }
    }
}