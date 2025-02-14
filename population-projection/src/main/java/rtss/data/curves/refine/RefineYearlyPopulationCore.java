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

import ch.qos.logback.classic.Level;

import java.util.Arrays;

/*
 * This module/algirithm is a post-processing stage for decomposition of binned demographic population data
 * from 5-year groups into individual years of age.
 * 
 * First stage (not contained here) is a general-purpose disaggregator agnostic of actual mortality patterns.
 * 
 * This module receives the output of the first stage and tries to refine it to make it conform young-age mortality
 * pattern characteristic for the era.
 * 
 * Input: array "p" contains the output of 1-st stage disaggegation, including age points for age bins 0...4 and 5...9. 
 * 
 * The size of "p" is max(10, nTunablePoints + nFixedPoints), as is explained below.
 * 
 * Only first nTunablePoints at the start of "p" can be tweaked, i.e. ages 0 to (nTunablePoints - 1). 
 * But subseqeunt nFixedPoints can additionally be used to check for curve smoothness.
 *   
 * Typically, nTunablePoints does not exceed 10 (total size of two first age bins), and then nFixedPoints is 2.
 * These are the values used when bin sum values follow pattern X-DOWN-DOWN (i.e. first three bins exhibit the 
 * pattern of population decrease with age).
 * 
 * However for cases X-DOWN-UP (a flip in the 2nd bin) nTunablePoints is shorter: p[nTunablePoints] is a flip point.
 * 
 * In addition, target_diff(x) contains mortality pattern that the algorithm will try to match.
 * The size of "target_diff" array is nTunablePoints. 
 * The values of differences in the adjusted series (p(x) - p(x+1)) should be proportional to the target_diff(x).
 * Only relative values in target_diff matter, not absolute values.
 * In other words, array target_diff describes a steepness of p(x) descent.
 * We use adjusted p(x) to build an array of actual values (p(x) - p(x+1)), lets name it actual_diff(x), 
 * normalize each of the arrays actual_diff and target_diff so that a sum of all elements in each of the diff arrays is 1.0, 
 * then the distance between these two normalized vectors should be minimized. 
 * I.e. sum(abs(normalized actual_diff(x) - normalized target_diff(x))) should be minimized.
 * 
 * Other constraints:
 *    
 *     The sum of adjusted series elements p(0) to p(4) exactly equals psum04, just like for the original "p". 
 *     This is a hard constraint (value of age bin 0...4 should be maintained).
 *     
 *     The sum of adjusted series elements p(5) to p(9) exactly equals psum04, just like for the original "p".
 *     This is another hard constraint (value of age bin 5...9 should be maintained).
 *     
 *     The curve should be monotonically descending from point p(0) to p(nTunablePoints). 
 *     This is a hard constraint too. 
 *     
 *     The chart for p(x) should look like a smooth curve, with continuous first derivative. 
 *     This is a soft constraint.
 *     We take into account p''(x) at first nTunablePoints (except point 0, where it is impossible) and then at further (nFixedPoints - 1) points.
 *     
 * The task is over-constrained because constraints for target_diff and for curve smoothness cannot be met EXACTLY both.
 * Therefore we assign relative importance weights to these two constraints, namded "importance_smoothness" and "importance_target_diff_matching".
 * 
 * The task then is to minimize a weighted sum of violations for smoothness criteria and for diff criteria;
 * while at the same to,e keeping bins sum value and curve descendance from point 0 to point nTunablePoints as hard constraints 
 * that should be maintained precisely.
 */
public class RefineYearlyPopulationCore
{
    final static double RegularPenalty = 1e1;
    final static double LargePenalty = 1e4;

    public double[] optimizeSeries(
            final double[] p,
            final double[] target_diff,
            final double arg_importance_smoothness,
            final double arg_importance_target_diff_matching,
            final int nTunablePoints,
            final int nFixedPoints,
            final Level logLevel,
            final String title)
    {
        final double psum04 = Util.sum(Util.splice(p, 0, 4));
        final double psum59 = Util.sum(Util.splice(p, 5, 9));
        final double[] initialGuess = Util.splice(p, 0, nTunablePoints - 1);

        final double importance_smoothness = arg_importance_smoothness * RegularPenalty;
        final double importance_target_diff_matching = arg_importance_target_diff_matching * RegularPenalty;

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
                                                      nTunablePoints, nFixedPoints, psum04, psum59, logLevel);

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

        CMAESOptimizer optimizer = new CMAESOptimizer(500_000, // Max iterations
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
        PointValuePair result = optimizer.optimize(MaxEval.unlimited(), // Maximum number of evaluations
                                                   new ObjectiveFunction(constrainedObjective),
                                                   GoalType.MINIMIZE,
                                                   new InitialGuess(initialGuess),
                                                   new Sigma(inputSigma), // Step sizes
                                                   populationSize, // Population size
                                                   new SimpleBounds(lowerBoundsForOptimizer, upperBoundsForOptimizer) // Bounds
        );

        // result of the optimization
        double[] px = result.getPoint();

        // debugging output
        if (logLevel == Level.TRACE || logLevel == Level.ALL || logLevel == Level.DEBUG)
        {
            Util.out("");
            Util.out("RefineYearlyPopulationBase completed for " + title);
            Util.out("Objective values for the intitial curve (" + title + "):");
            calculateObjective(p, target_diff,
                               importance_smoothness, importance_target_diff_matching,
                               nTunablePoints, nFixedPoints, psum04, psum59, Level.TRACE);

            Util.out("Objective values for the result curve (" + title + "):");
            int plength = Math.max(10, nTunablePoints + nFixedPoints);
            double[] fullP = Arrays.copyOf(px, plength);
            System.arraycopy(p, nTunablePoints, fullP, nTunablePoints, plength - nTunablePoints);
            calculateObjective(fullP, target_diff,
                               importance_smoothness, importance_target_diff_matching,
                               nTunablePoints, nFixedPoints, psum04, psum59, Level.TRACE);
        }

        // Return the optimized values
        return px;
    }

    /* ---------------------------------------------------------------------------------------- */

    // objectibe function
    private double calculateObjective(
            final double[] p,
            final double[] target_diff,
            final double importance_smoothness,
            final double importance_target_diff_matching,
            final int nTunablePoints,
            final int nFixedPoints,
            final double psum04,
            final double psum59,
            final Level logLevel)
    {
        double monotonicityViolation = calculateMonotonicityViolation(p, nTunablePoints);

        // Calculate smoothness violation
        double smoothnessViolation = calculateSmoothnessViolation(p, nTunablePoints, nFixedPoints);

        // Add penalties for violating the bin sum constraints
        double sumViolation = calculateBinSumViolation(p, psum04, psum59);

        // Calculate target difference violation
        double targetDiffViolation = calculateTargetDiffViolation(p, target_diff);

        // Return weighted sum of violations
        double objective = monotonicityViolation + sumViolation +
                           importance_smoothness * smoothnessViolation +
                           importance_target_diff_matching * targetDiffViolation;

        if (logLevel == Level.TRACE || logLevel == Level.ALL)
        {
            Util.out(String.format("diff = %9.4f   smoothness = %9.4f   monotonicity = %9.4e   sum = %9.4e   objective = %12.7e",
                                   targetDiffViolation,
                                   smoothnessViolation,
                                   monotonicityViolation,
                                   sumViolation,
                                   objective));
        }

        return objective;
    }

    private double calculateMonotonicityViolation(double[] p, int nTunablePoints)
    {
        double monotonicityViolation = 0.0;

        p = Util.normalize(p);

        for (int k = 0; k < nTunablePoints; k++)
        {
            double d = p[k] - p[k + 1];
            if (d < 0)
                monotonicityViolation += Math.abs(d) * LargePenalty;
        }

        return monotonicityViolation;
    }

    private double calculateBinSumViolation(double[] p, double psum04, double psum59)
    {
        double sum04 = Arrays.stream(p, 0, 5).sum(); // Sum of p(0) to p(4)
        double sum59 = Arrays.stream(p, 5, 10).sum(); // Sum of p(5) to p(9)
        double penalty04 = LargePenalty * Math.abs(sum04 - psum04) / psum04; // Large penalty for violating psum04
        double penalty59 = LargePenalty * Math.abs(sum59 - psum59) / psum59; // Large penalty for violating psum59
        return penalty04 + penalty59;
    }

    private double calculateSmoothnessViolation(double[] p, int nTunablePoints, int nFixedPoints)
    {
        double smoothnessViolation = 0.0;

        p = Util.normalize(p);

        for (int i = 1; i < p.length - 1 && i <= nTunablePoints + nFixedPoints - 2; i++)
            smoothnessViolation += Math.abs(d2(p, i));

        return smoothnessViolation;
    }

    // calculate second derivative at point p[i]
    private double d2(double[] p, int i)
    {
        if (i == 0)
            return 0;

        double derivative1 = p[i] - p[i - 1];
        double derivative2 = p[i + 1] - p[i];
        return derivative2 - derivative1;
    }

    private double calculateTargetDiffViolation(double[] p, double[] target_diff)
    {
        double[] actual_diff = new double[target_diff.length];

        p = Util.normalize(p);

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
    private void adjustInputSigma(double[] inputSigma, double psum04, double psum59)
    {
        for (int k = 0; k <= 4 && k < inputSigma.length; k++)
            inputSigma[k] = psum04 * 0.001;

        for (int k = 5; k <= 9 && k < inputSigma.length; k++)
            inputSigma[k] = psum59 * 0.001;
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
     * Sigma controls the initial step size and exploration range.
     * Lambda controls the number of points sampled in each iteration.
     * A larger lambda can compensate for a suboptimal sigma by exploring more points, 
     * but at the cost of increased computational effort.
     * 
     * Also see the explanation in https://en.wikipedia.org/wiki/CMA-ES
     * 
     * ======== Practical Tips for Using CMAESOptimizer
     * 
     * Tune sigma first: 
     * Adjust sigma to ensure the initial search range is appropriate for your problem.
     * 
     * Scale lambda with problem size: 
     * Use the heuristic lambda = 4 + floor(3 * log(n)) to set lambda based on the number of dimensions.
     * 
     * Monitor convergence: 
     * Run the algorithm with different parameter settings and monitor the convergence behavior to find the best configuration.
     */
    private int chooseLambda(int nTunablePoints)
    {
        // Default formula for lambda
        int lambda = 4 + (int) (3 * Math.log(nTunablePoints));

        // impose mimimum
        lambda = Math.max(lambda, 2000);

        return lambda;
    }

    /*==================================================================== */

    /*
     * Example/test code
     */
    public static void main(String[] args)
    {
        // test_1();
        test_2();
    }

    @SuppressWarnings("unused")
    private static void test_1()
    {
        double p[] = { 3079.1064761352536, 2863.741162691683, 2648.375849248112, 2433.010535804541, 2217.645222360971, 2002.2799089174,
                       1831.1316029723425, 1749.4931260194671, 1757.808507358823, 1852.286854731967 };

        double target_diff[] = { 0.5635055255718324, 0.1853508095605243, 0.08306347982523773, 0.04790542277049602, 0.028270367514777694,
                                 0.02770496016448214, 0.021691081984065795 };

        double importance_smoothness = 0.80;
        double importance_target_diff_matching = 1.0 - importance_smoothness;

        int nTunablePoints = 7;
        int nFixedPoints = 1;

        RefineYearlyPopulationCore rc = new RefineYearlyPopulationCore();

        double[] px = rc.optimizeSeries(p,
                                        target_diff,
                                        importance_smoothness,
                                        importance_target_diff_matching,
                                        nTunablePoints,
                                        nFixedPoints,
                                        Level.TRACE,
                                        "example test_1");

        Util.unused(px);

        Util.out("Finished test_1");
    }

    @SuppressWarnings("unused")
    private static void test_2()
    {
        double p[] = { 2758.9111513137814, 2540.455070553185, 2321.998989792589, 2103.5429090319926, 1885.0868282713964, 1666.6307475108001,
                       1487.948517512541, 1392.6120548630918, 1379.5241192732535, 1444.2845608403145 };

        double target_diff[] = { 0.47382170458256234, 0.19957548710133885, 0.09807336453684555, 0.0638402089909655, 0.048492434962446936,
                                 0.03624687057799064, 0.027865462065962774, 0.021769892239033417 };

        double importance_smoothness = 0.95;
        double importance_target_diff_matching = 1.0 - importance_smoothness;

        int nTunablePoints = 8;
        int nFixedPoints = 1;

        RefineYearlyPopulationCore rc = new RefineYearlyPopulationCore();

        double[] px = rc.optimizeSeries(p,
                                        target_diff,
                                        importance_smoothness,
                                        importance_target_diff_matching,
                                        nTunablePoints,
                                        nFixedPoints,
                                        Level.TRACE,
                                        "example test_2");

        Util.unused(px);

        Util.out("Finished test_2");
    }
}