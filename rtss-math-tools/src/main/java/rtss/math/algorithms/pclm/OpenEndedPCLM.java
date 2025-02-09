package rtss.math.algorithms.pclm;

import org.apache.commons.math3.analysis.MultivariateFunction;
//import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.optim.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;
import Jama.Matrix;

/*
 * A version of PCLM for open-ended last interval.
 */
public class OpenEndedPCLM
{
    private double[] binCounts; // Observed counts in each bin (N entries)
    private double[] binEdges; // Edges of the bins  (N + 1 entries, first N entries are left edges of the bins, the last entry is the right edge of the last bin)
    private double lambda; // Smoothing parameter
    private double upperBound; // Artificial upper bound for the open-ended interval, overrides the last entry in binEdges 

    public OpenEndedPCLM(double[] binCounts, double[] binEdges, double lambda, double upperBound) {
        this.binCounts = binCounts;
        this.binEdges = binEdges;
        this.lambda = lambda;
        this.upperBound = upperBound;
    }

    public double[] fit()
    {
        int nBins = binCounts.length;
        int nKnots = binEdges.length;

        // Design matrix for the composite link model (adjusted for unequal bin widths and open-ended intervals)
        Matrix X = new Matrix(nBins, nKnots);
        for (int i = 0; i < nBins; i++)
        {
            double binStart = binEdges[i];
            double binEnd = (i == nBins - 1) ? upperBound : binEdges[i + 1]; // Use upperBound for the last bin
            double binWidth = binEnd - binStart;

            for (int j = 0; j < nKnots; j++)
            {
                double knotStart = binEdges[j];
                double knotEnd = (j == nKnots - 1) ? upperBound : binEdges[j + 1]; // Use upperBound for the last knot

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
        // Example usage with an open-ended interval
        double[] binCounts = { 10, 20, 30, 40, 50 }; // Observed counts in each bin
        double[] binEdges = { 0, 10, 20, 30, 40, 100 }; // Edges of the bins (last bin is open-ended: 100+)
        double lambda = 1.0; // Smoothing parameter
        double upperBound = 120; // Artificial upper bound for the open-ended interval

        OpenEndedPCLM pclm = new OpenEndedPCLM(binCounts, binEdges, lambda, upperBound);
        double[] estimatedParams = pclm.fit();

        System.out.println("Estimated Parameters:");
        for (double param : estimatedParams)
        {
            System.out.println(param);
        }
    }
}
