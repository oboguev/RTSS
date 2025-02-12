package rtss.data.curves;

import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

import org.apache.commons.math4.legacy.analysis.MultivariateFunction;
import org.apache.commons.math4.legacy.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math4.legacy.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math4.legacy.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math4.legacy.optim.nonlinear.scalar.MultivariateFunctionPenaltyAdapter;
import org.apache.commons.math4.legacy.optim.ConvergenceChecker;
import org.apache.commons.math4.legacy.optim.InitialGuess;
import org.apache.commons.math4.legacy.optim.MaxEval;
import org.apache.commons.math4.legacy.optim.PointValuePair;
import org.apache.commons.math4.legacy.optim.SimpleBounds;
import org.apache.commons.math4.legacy.optim.SimpleValueChecker;

import java.util.Arrays;

/*
 * DeepSeek request:
 * 
 *     https://chat.deepseek.com/a/chat/s/a33e89c2-254a-4536-b839-9d186ca1429a
 *     
 *     Please design an algorithm.
 *     
 *     There is a series p(x) where x is an integer ranging from 0 to 11.
 *     It is known that the series is monotonically decreasing.
 *     
 *     We also know values for p(10) and p(11).
 *     
 *     We also know the sum of values p(0) to p(4), lets designate it psum04, but not individual values of elements 0 to 4.
 *     
 *     We also know the sum of values p(5) to p(9), lets designate it psum59, but not individual values of elements 5 to 9.
 *     
 *     The goal is recover the values of p(0) to p(9) under the mentioned constraints that:
 *     
 *     Constraint 1. 
 *     The sum of p(0) to p(4) exactly equals psum04. This is a hard constraint.
 *     
 *     Constraint 2. 
 *     The sum of p(5) to p(9) exactly equals psum04. This is another hard constraint.
 *     
 *     Constraint 3. 
 *     Values of p(10) and p(11) are fixed and cannot be adjusted.
 *     
 *     And also the following additional constraints:
 *     
 *     Constraint 4. 
 *     The chart for p(x) should look like a smooth curve, with continuous first derivative.
 *     
 *     Constraint 5. 
 *     There is also an array of 10 elements target_diff(x), with x ranging 0 to 9.
 *     The values of differences (p(x) - p(x+1)) should be proportional to the target_diff(x).
 *     Please note that only relative values in target_diff matter, not absolute values.
 *     In other words, array target_diff describes a steepness of p(x) descent.
 *     If you use p(x) to build an array of actual values (p(x) - p(x+1)), lets name it actual_diff(x), 
 *     normalize each of the arrays actual_diff and target_diff so that a sum of all elements in each of the diff arrays is 1.0, 
 *     then then the distance between these two normalized vectors should be minimized. 
 *     I.e. sum(abs(normalized actual_diff(x) - normalized target_diff(x))) should be minimized.
 *     I hope I explained constraint 4 clearly enough. If not, please ask me to clarify further.
 *     
 *     The task is obviously over-constrained. Constraints 4 and 5 cannot be met EXACTLY both.
 *     Let's assign relative importance weights to these two constraints.
 *     Let's designate these weights importance_smoothness and importance_target_diff_matching.
 *     The task then is to minimize a weighted sum of criteria 4 and 5 violations.
 *     
 *     Please design the algorithm and implement it in Java.
 *     
 *     Input parameters:
 *     - array p, only values of p(10) and p(11) matter
 *     - psum04
 *     - psum59
 *     - array target_diff
 *     
 *     .................
 *     
 *     Please implement method optimizeSeries using Apache Common Math.
 *      
 *     .................

 *     Please update the code for Apache Common Math 3.6.1.
 *    
 *     ...............
 *    
 *     Actually CMAESOptimizer is available in 3.6.1 but it uses a different constructor than you used in the code.
 *    
 *     ...............
 *    
 *     Sorry, the signature does not match again.
 *     Here is the signature for CMAESOptimizer in 3.6.1:
 *    
 *     CMAESOptimizer(int maxIterations, double stopFitness, boolean isActiveCMA, int diagonalOnly, int checkFeasableCount, RandomGenerator random, boolean generateStatistics, ConvergenceChecker<PointValuePair> checker) 
 *    
 *     According to https://commons.apache.org/proper/commons-math/javadocs/api-3.6.1/org/apache/commons/math3/optim/nonlinear/scalar/noderiv/CMAESOptimizer.html
 *    
 *    .......................
 *    
 *     The signature for MultivariateFunctionPenaltyAdapter in 3.6.1 is
 *    
 *     MultivariateFunctionPenaltyAdapter(MultivariateFunction bounded, double[] lower, double[] upper, double offset, double[] scale)
 *    
 *     Please see https://commons.apache.org/proper/commons-math/javadocs/api-3.6.1/org/apache/commons/math3/optim/nonlinear/scalar/MultivariateFunctionPenaltyAdapter.html     
 *     
 */

public class RefineYearlyPopulation
{

    public static void main(String[] args)
    {
        // Example input parameters
        double[] p = new double[12];
        p[0] = 15.0; // Initial guess for p(0)
        p[1] = 14.0; // Initial guess for p(1)
        p[2] = 13.0; // Initial guess for p(2)
        p[3] = 12.0; // Initial guess for p(3)
        p[4] = 11.0; // Initial guess for p(4)
        p[5] = 10.0; // Initial guess for p(5)
        p[6] = 9.0; // Initial guess for p(6)
        p[7] = 8.0; // Initial guess for p(7)
        p[8] = 7.0; // Initial guess for p(8)
        p[9] = 6.0; // Initial guess for p(9)
        p[10] = 5.0; // Known value
        p[11] = 4.0; // Known value

        double psum04 = 50.0; // Sum of p(0) to p(4)
        double psum59 = 30.0; // Sum of p(5) to p(9)
        double[] target_diff = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }; // Target differences
        double importance_smoothness = 0.5; // Weight for smoothness constraint
        double importance_target_diff_matching = 0.5; // Weight for target difference matching

        // Reconstruct the series
        double[] reconstructedP = reconstructSeries(p, psum04, psum59, target_diff, importance_smoothness, importance_target_diff_matching);

        // Output the reconstructed series
        System.out.println("Reconstructed series p(0) to p(9):");
        for (int i = 0; i < 10; i++)
        {
            System.out.println("p(" + i + ") = " + reconstructedP[i]);
        }
    }

    public static double[] reconstructSeries(double[] p, double psum04, double psum59, double[] target_diff, double importance_smoothness,
            double importance_target_diff_matching)
    {
        // Use the initial guess from p(0) to p(9)
        double[] initialGuess = Arrays.copyOfRange(p, 0, 10);

        // Optimize the series
        double[] optimizedP = optimizeSeries(p, initialGuess, psum04, psum59, target_diff, importance_smoothness, importance_target_diff_matching);

        return optimizedP;
    }

    private static double[] optimizeSeries(double[] p, double[] initialGuess, double psum04, double psum59, double[] target_diff,
            double importance_smoothness, double importance_target_diff_matching)
    {
        // Define the objective function
        MultivariateFunction objectiveFunction = new MultivariateFunction()
        {
            @Override
            public double value(double[] point)
            {
                // Combine p(0) to p(9) with the fixed p(10) and p(11)
                double[] fullP = Arrays.copyOf(point, 12);
                fullP[10] = p[10];
                fullP[11] = p[11];

                // Calculate the objective value
                return calculateObjective(fullP, target_diff, importance_smoothness, importance_target_diff_matching);
            }
        };

        // Define the constraints as penalty functions
        double[] lowerBounds = new double[10];
        double[] upperBounds = new double[10];
        Arrays.fill(lowerBounds, Double.NEGATIVE_INFINITY); // No lower bounds
        Arrays.fill(upperBounds, Double.POSITIVE_INFINITY); // No upper bounds

        double offset = 0.0; // Offset for penalty
        double[] scale = new double[10]; // Penalty weights for each variable
        Arrays.fill(scale, 1.0); // Uniform penalty weights

        MultivariateFunctionPenaltyAdapter constrainedObjective = new MultivariateFunctionPenaltyAdapter(
                                                                                                         objectiveFunction,
                                                                                                         lowerBounds,
                                                                                                         upperBounds,
                                                                                                         offset,
                                                                                                         scale);

        // Set up the CMAESOptimizer
        RandomGenerator random = new JDKRandomGenerator();
        ConvergenceChecker<PointValuePair> checker = new SimpleValueChecker(1e-6, 1e-6);
        CMAESOptimizer optimizer = new CMAESOptimizer(
                                                      10000, // Max iterations
                                                      1e-6, // Stop fitness
                                                      true, // Active CMA
                                                      0, // Diagonal only (0 means full covariance)
                                                      1, // Check feasible count
                                                      random, // Random generator
                                                      false, // No statistics
                                                      checker // Convergence checker
        );

        // Define the input sigma (step sizes for each variable)
        double[] inputSigma = new double[10];
        Arrays.fill(inputSigma, 1.0); // Initial step size for each variable

        // Define bounds for the variables
        double[] lowerBoundsForOptimizer = new double[10];
        double[] upperBoundsForOptimizer = new double[10];
        Arrays.fill(lowerBoundsForOptimizer, Double.NEGATIVE_INFINITY); // No lower bounds
        Arrays.fill(upperBoundsForOptimizer, Double.POSITIVE_INFINITY); // No upper bounds

        // Perform the optimization
        PointValuePair result = optimizer.optimize(
                                                   new MaxEval(10000), // Maximum number of evaluations
                                                   new ObjectiveFunction(constrainedObjective),
                                                   GoalType.MINIMIZE,
                                                   new InitialGuess(initialGuess),
                                                   new SimpleBounds(lowerBoundsForOptimizer, upperBoundsForOptimizer) // No bounds
        );

        // Return the optimized values
        return result.getPoint();
    }

    private static double calculateObjective(double[] p, double[] target_diff, double importance_smoothness, double importance_target_diff_matching)
    {
        // Calculate smoothness violation
        double smoothnessViolation = calculateSmoothnessViolation(p);

        // Calculate target difference violation
        double targetDiffViolation = calculateTargetDiffViolation(p, target_diff);

        // Return weighted sum of violations
        return importance_smoothness * smoothnessViolation + importance_target_diff_matching * targetDiffViolation;
    }

    private static double calculateSmoothnessViolation(double[] p)
    {
        double smoothnessViolation = 0.0;
        for (int i = 1; i < p.length - 1; i++)
        {
            double derivative1 = p[i] - p[i - 1];
            double derivative2 = p[i + 1] - p[i];
            smoothnessViolation += Math.pow(derivative2 - derivative1, 2);
        }
        return smoothnessViolation;
    }

    private static double calculateTargetDiffViolation(double[] p, double[] target_diff)
    {
        double[] actual_diff = new double[target_diff.length];
        for (int i = 0; i < actual_diff.length; i++)
        {
            actual_diff[i] = p[i] - p[i + 1];
        }

        // Normalize actual_diff and target_diff
        double sumActualDiff = Arrays.stream(actual_diff).sum();
        double sumTargetDiff = Arrays.stream(target_diff).sum();
        double[] normalizedActualDiff = Arrays.stream(actual_diff).map(d -> d / sumActualDiff).toArray();
        double[] normalizedTargetDiff = Arrays.stream(target_diff).map(d -> d / sumTargetDiff).toArray();

        // Calculate sum of absolute differences
        double targetDiffViolation = 0.0;
        for (int i = 0; i < normalizedActualDiff.length; i++)
        {
            targetDiffViolation += Math.abs(normalizedActualDiff[i] - normalizedTargetDiff[i]);
        }

        return targetDiffViolation;
    }
}