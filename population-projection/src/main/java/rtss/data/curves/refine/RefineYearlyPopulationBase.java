package rtss.data.curves.refine;

import org.apache.commons.math4.legacy.analysis.MultivariateFunction;
import org.apache.commons.math4.legacy.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math4.legacy.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math4.legacy.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math4.legacy.optim.nonlinear.scalar.MultivariateFunctionPenaltyAdapter;
import org.apache.commons.math4.legacy.optim.ConvergenceChecker;
import org.apache.commons.math4.legacy.optim.InitialGuess;
import org.apache.commons.math4.legacy.optim.MaxEval;
import org.apache.commons.math4.legacy.optim.PointValuePair;
import org.apache.commons.math4.legacy.optim.SimpleValueChecker;
import org.apache.commons.math4.legacy.optim.SimpleBounds;
import org.apache.commons.math4.legacy.optim.nonlinear.scalar.PopulationSize;
import org.apache.commons.math4.legacy.optim.nonlinear.scalar.Sigma;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;

import rtss.util.Util;

// import rtss.util.Util;

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
public class RefineYearlyPopulationBase
{
    protected static double[] optimizeSeries(
            final double[] p,
            final double[] initialGuess,
            final double psum04,
            final double psum59,
            final double[] target_diff,
            final double importance_smoothness,
            final double importance_target_diff_matching,
            final int nTunablePoints,
            final int nFixedPoints)
    {
        // Define the objective function
        MultivariateFunction objectiveFunction = new MultivariateFunction()
        {
            @Override
            public double value(double[] point)
            {
                // Combine p(0) to p(nTunablePoints - 1) with the fixed p(nTunablePoints) to p(nTunablePoints + nFixedPoints - 1)
                int plength = Math.max(10, nTunablePoints + nFixedPoints);
                double[] fullP = Arrays.copyOf(point, plength);
                System.arraycopy(p, nTunablePoints, fullP, nTunablePoints, plength - nTunablePoints);

                // Calculate the objective value
                double objective = calculateObjective(fullP, target_diff,
                                                      importance_smoothness, importance_target_diff_matching,
                                                      nTunablePoints, psum04, psum59);

                return objective;
            }
        };

        // Define the constraints as penalty functions
        double[] lowerBounds = new double[nTunablePoints];
        double[] upperBounds = new double[nTunablePoints];
        Arrays.fill(lowerBounds, 0.0); // Lower bounds set to 0 (non-negative values)
        Arrays.fill(upperBounds, Double.POSITIVE_INFINITY); // No upper bounds
        adjustBounds(lowerBounds, upperBounds, psum04, psum59, p);

        double offset = 0.0; // Offset for penalty
        double[] scale = new double[nTunablePoints]; // Penalty weights for each variable
        Arrays.fill(scale, 1.0); // Uniform penalty weights

        MultivariateFunctionPenaltyAdapter constrainedObjective = new MultivariateFunctionPenaltyAdapter(objectiveFunction,
                                                                                                         lowerBounds,
                                                                                                         upperBounds,
                                                                                                         offset,
                                                                                                         scale);

        // Set up the CMAESOptimizer
        UniformRandomProvider random = RandomSource.JDK.create(); // Use UniformRandomProvider
        ConvergenceChecker<PointValuePair> checker = new SimpleValueChecker(1e-6, 1e-6);

        CMAESOptimizer optimizer = new CMAESOptimizer(10_000, // Max iterations
                                                      1e-6, // Stop fitness
                                                      true, // Active CMA
                                                      0, // Diagonal only (0 means full covariance)
                                                      1, // Check feasible count
                                                      random, // Random generator
                                                      false, // No statistics
                                                      checker // Convergence checker
        );

        // Define the input sigma (step sizes for each variable)
        double[] inputSigma = new double[nTunablePoints];
        Arrays.fill(inputSigma, 1.0); // Initial step size for each variable
        adjustInputSigma(inputSigma, psum04, psum59);

        // Define bounds for the variables
        double[] lowerBoundsForOptimizer = new double[nTunablePoints];
        double[] upperBoundsForOptimizer = new double[nTunablePoints];
        Arrays.fill(lowerBoundsForOptimizer, 0.0); // Lower bounds set to 0 (non-negative values)
        Arrays.fill(upperBoundsForOptimizer, Double.POSITIVE_INFINITY); // No upper bounds
        adjustBounds(lowerBounds, upperBounds, psum04, psum59, p);

        // Set the population size (lambda).
        // Note that it has nothing to do with demographic population, but rather is number of 
        // candidate solutions generated in each iteration of the algorithm.
        PopulationSize populationSize = new PopulationSize(chooseLambda(nTunablePoints));

        // Perform the optimization
        PointValuePair result = optimizer.optimize(new MaxEval(10_000), // Maximum number of evaluations
                                                   new ObjectiveFunction(constrainedObjective),
                                                   GoalType.MINIMIZE,
                                                   new InitialGuess(initialGuess),
                                                   new Sigma(inputSigma), // Step sizes
                                                   populationSize, // Population size
                                                   new SimpleBounds(lowerBoundsForOptimizer, upperBoundsForOptimizer) // Bounds
        );

        // Return the optimized values
        return result.getPoint();
    }

    private static double calculateObjective(
            double[] p,
            double[] target_diff,
            double importance_smoothness,
            double importance_target_diff_matching,
            int nTunablePoints,
            double psum04,
            double psum59)
    {
        double monotonicityViolation = calculateMonotonicityViolation(p, nTunablePoints);

        // Calculate smoothness violation
        double smoothnessViolation = calculateSmoothnessViolation(p);

        // Add penalties for violating the sum constraints
        double sumViolation = calculateSumViolation(p, psum04, psum59);

        // Calculate target difference violation
        double targetDiffViolation = calculateTargetDiffViolation(p, target_diff);

        // Return weighted sum of violations
        double objective = monotonicityViolation + sumViolation +
                           importance_smoothness * smoothnessViolation +
                           importance_target_diff_matching * targetDiffViolation;

        util_out(String.format("diff = %9.4f   smoothness = %9.4f   monotonicity = %9.4e   sum = %9.4e   objective = %12.7e",
                               targetDiffViolation,
                               smoothnessViolation,
                               monotonicityViolation,
                               sumViolation,
                               objective));

        return objective;
    }

    private static double calculateMonotonicityViolation(double[] p, int nTunablePoints)
    {
        double monotonicityViolation = 0.0;

        p = util_normalize(p);

        for (int k = 0; k < nTunablePoints; k++)
        {
            double d = p[k] - p[k + 1];
            if (d < 0)
                monotonicityViolation += Math.abs(d) * 1e6;
        }

        return monotonicityViolation;
    }

    private static double calculateSumViolation(double[] p, double psum04, double psum59)
    {
        double sum04 = Arrays.stream(p, 0, 5).sum(); // Sum of p(0) to p(4)
        double sum59 = Arrays.stream(p, 5, 10).sum(); // Sum of p(5) to p(9)
        double penalty04 = 1e6 * Math.abs(sum04 - psum04) / psum04; // Large penalty for violating psum04
        double penalty59 = 1e6 * Math.abs(sum59 - psum59) / psum59; // Large penalty for violating psum59
        return penalty04 + penalty59;
    }

    private static double calculateSmoothnessViolation(double[] p)
    {
        double smoothnessViolation = 0.0;

        p = util_normalize(p);

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

        p = util_normalize(p);

        for (int i = 0; i < actual_diff.length; i++)
            actual_diff[i] = p[i] - p[i + 1];

        // Normalize actual_diff and target_diff
        double sumActualDiff = Arrays.stream(actual_diff).sum();
        double sumTargetDiff = Arrays.stream(target_diff).sum();
        double[] normalizedActualDiff = Arrays.stream(actual_diff).map(d -> d / sumActualDiff).toArray();
        double[] normalizedTargetDiff = Arrays.stream(target_diff).map(d -> d / sumTargetDiff).toArray();

        // Calculate sum of absolute differences
        double targetDiffViolation = 0.0;
        for (int i = 0; i < normalizedActualDiff.length; i++)
            targetDiffViolation += Math.abs(normalizedActualDiff[i] - normalizedTargetDiff[i]);

        return targetDiffViolation;
    }

    /* ---------------------------------------------------------------------------------------- */

    /*
     * Arrays @lowerBounds and @upperBounds have size nTunablePoints. 
     */
    private static void adjustBounds(double[] lowerBounds, double[] upperBounds, double psum04, double psum59, final double[] p)
    {
        for (int k = 0; k <= 4 && k < upperBounds.length; k++)
            upperBounds[k] = psum04;

        for (int k = 5; k <= 9 && k < upperBounds.length; k++)
            upperBounds[k] = psum59;

        for (int k = 0; k < lowerBounds.length; k++)
            lowerBounds[k] = p[lowerBounds.length];
    }

    /*
     * Sigma is the initial step size or standard deviation of the search distribution. 
     * It controls the spread of the initial population of candidate solutions around the starting point.
     * In CMA-ES, the search distribution is a multivariate Gaussian distribution, 
     * and sigma scales the covariance matrix of this distribution.
     * 
     * Sigma determines how far the algorithm initially explores from the starting point. 
     * A larger sigma means the algorithm will explore a wider area, 
     * while a smaller sigma focuses the search closer to the starting point.
     * Over time, the algorithm adapts sigma (and the covariance matrix) to improve the search direction and step size.
     * 
     * If sigma is too large, the algorithm may waste time exploring irrelevant regions of the search space.
     * If sigma is too small, the algorithm may get stuck in a local optimum or converge too slowly.
     * The choice of sigma is problem-dependent and should reflect the scale of the problem and the expected distance to the optimum.
     * 
     * A good rule of thumb is to set sigma to a fraction of the expected range of the variables. For example:
     * If your variables are expected to vary between -10 and 10, a reasonable sigma might be 2–5.
     * If you have no prior knowledge, you can start with sigma = 1 and adjust based on the algorithm’s performance. 
     */
    private static void adjustInputSigma(double[] inputSigma, double psum04, double psum59)
    {
        for (int k = 0; k <= 4 && k < inputSigma.length; k++)
            inputSigma[k] = psum04 * 0.1;

        for (int k = 5; k <= 9 && k < inputSigma.length; k++)
            inputSigma[k] = psum59 * 0.1;
    }

    /*
     * Lambda is the population size, i.e., the number of candidate solutions (offspring) generated in each iteration of the algorithm.
     * In CMA-ES, lambda determines how many points are sampled from the search distribution at each step.
     *
     * A larger lambda means more candidate solutions are evaluated in each iteration, 
     * which can improve the exploration of the search space.
     * 
     * A smaller lambda means fewer evaluations per iteration, 
     * which can speed up the algorithm but may reduce the quality of the search.
     *
     * If lambda is too small, the algorithm may converge prematurely to a suboptimal solution.
     * If lambda is too large, the algorithm may become computationally expensive, 
     * as more function evaluations are required per iteration.
     * The optimal value of lambda depends on the problem’s dimensionality and complexity.
     *
     * A common heuristic is to set lambda = 4 + floor(3 * log(n)), where n is the number of dimensions (variables) in the problem. 
     * This ensures that the population size scales appropriately with the problem size.
     * For small problems (e.g., n < 10), lambda can be smaller (e.g., 10–20).
     * For larger problems, lambda should be increased to ensure sufficient exploration.     * 
     * 
     * ======== Relationship Between sigma and lambda:
     * 
     * Sigma and lambda are independent parameters, but they interact in the optimization process.
     * sigma controls the initial step size and exploration range.
     * Lambda controls the number of points sampled in each iteration.
     * A larger lambda can compensate for a suboptimal sigma by exploring more points, 
     * but at the cost of increased computational effort.
     * 
     * ======== Practical Tips for Using CMAESOptimizer
     * 
     * Tune sigma First: 
     * Adjust sigma to ensure the initial search range is appropriate for your problem.
     * 
     * Scale lambda with Problem Size: 
     * Use the heuristic lambda = 4 + floor(3 * log(n)) to set lambda based on the number of dimensions.
     * 
     * Monitor Convergence: 
     * Run the algorithm with different parameter settings and monitor the convergence behavior to find the best configuration.
     */
    private static int chooseLambda(int nTunablePoints)
    {
        // Default formula for lambda
        int lambda = 4 + (int) (3 * Math.log(nTunablePoints));

        // impose mimimum
        lambda = Math.max(lambda, 15);

        return lambda;
    }

    /*==================================================================== */

    // extract sub-array
    public static double[] util_splice(final double[] y, int x1, int x2)
    {
        int size = x2 - x1 + 1;
        if (size <= 0)
            throw new IllegalArgumentException("array splice : negative size");

        double[] yy = new double[size];
        for (int x = x1; x <= x2; x++)
            yy[x - x1] = y[x];
        return yy;
    }

    // sum of array values
    public static double util_sum(final double[] y)
    {
        double sum = 0;
        for (int k = 0; k < y.length; k++)
            sum += y[k];
        return sum;
    }

    // normalize array so the sum of its elements is 1.0
    public static double[] util_normalize(final double[] y)
    {
        return util_normalize(y, 1.0);
    }

    // normalize array so the sum of its elements is @sum
    public static double[] util_normalize(final double[] y, double sum)
    {
        return util_multiply(y, sum / util_sum(y));
    }

    // return a new array with values representing y[] * f
    public static double[] util_multiply(final double[] y, double f)
    {
        double[] yy = new double[y.length];
        for (int x = 0; x < y.length; x++)
            yy[x] = y[x] * f;
        return yy;
    }

    public static void util_out(String s)
    {
        System.out.println(s);
    }

    /*==================================================================== */

    public static void main(String[] args)
    {
        double p[] = { 3079.1064761352536, 2863.741162691683, 2648.375849248112, 2433.010535804541, 2217.645222360971, 2002.2799089174,
                       1831.1316029723425, 1749.4931260194671, 1757.808507358823, 1852.286854731967 };

        double target_diff[] = { 0.5635055255718324, 0.1853508095605243, 0.08306347982523773, 0.04790542277049602, 0.028270367514777694,
                                 0.02770496016448214, 0.021691081984065795 };

        double importance_smoothness = 0.7;
        double importance_target_diff_matching = 0.3;
 
        double psum04 = util_sum(util_splice(p, 0, 4));
        double psum59 = util_sum(util_splice(p, 5, 9));
        
        int nTunablePoints = 7;
        int nFixedPoints = 1;
        
        double[] initialGuess = Util.splice(p, 0, nTunablePoints - 1);

 
        double[] px = optimizeSeries(
                p,
                initialGuess,
                psum04,
                psum59,
                target_diff,
                importance_smoothness,
                importance_target_diff_matching,
                nTunablePoints,
                nFixedPoints);
        
        util_out("Completed");
        util_out("");
    }
}