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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
 * But subseqeunt nFixedPoints can additionally be used to check for curve smoothness (under rare conditions nFixedPoints can be zero).
 *   
 * Typically, nTunablePoints does not exceed 10 (total size of two first age bins), and then nFixedPoints is 2.
 * These are the values used when bin sum values follow pattern X-DOWN-DOWN (i.e. first three bins exhibit the 
 * pattern of population decrease with age).
 * 
 * However for cases X-DOWN-UP (a flip in the 2nd bin) nTunablePoints is shorter: p[nTunablePoints] is a flip point.
 * 
 * In addition, target_diff(x) contains mortality pattern that the algorithm will try to match.
 * The size of "target_diff" array is nTunablePoints. 
 * The values of differences in the adjusted series (p(x) - p(x+1)) should be made proportional to the target_diff(x).
 * Only relative values in target_diff matter, not absolute values.
 * In other words, array target_diff describes a steepness of p(x) descent.
 * We use adjusted p(x) to build an array of actual values (p(x) - p(x+1)), lets name it actual_diff(x), 
 * then normalize each of the arrays actual_diff and target_diff so that a sum of all elements in each of the diff arrays is 1.0, 
 * then the distance between these two normalized vectors should be minimized. 
 * I.e. sum(abs(normalized actual_diff(x) - normalized target_diff(x))) should be minimized.
 * 
 * Other constraints:
 *    
 *     The sum of adjusted series elements p(0) to p(4) exactly equals psum04, just like for the original "p". 
 *     This is a hard constraint (value of age bin 0...4 should be maintained).
 *     
 *     The sum of adjusted series elements p(5) to p(9) exactly equals psum59, just like for the original "p".
 *     This is another hard constraint (value of age bin 5...9 should be maintained).
 *     
 *     The curve should be monotonically descending from point p(0) to p(nTunablePoints). 
 *     This is a hard constraint too. 
 *     
 *     The chart for p(x) should be a smooth curve, with continuous first derivative. 
 *     This is a soft constraint.
 *     We take into account p''(x) at first nTunablePoints (except point 0, where it is impossible) 
 *     and then at further max(0, nFixedPoints - 1) points.
 *     
 * The task is over-constrained because constraints for target_diff and for curve smoothness cannot be met EXACTLY both.
 * Therefore we assign relative importance weights to these two constraints, namded "importance_smoothness" and 
 * "importance_target_diff_matching".
 * 
 * The task then is to minimize a weighted sum of violations for smoothness criteria and for diff criteria;
 * while at the same time keeping bins sum value and curve descendance from point 0 to point nTunablePoints as hard constraints 
 * that should be maintained precisely.
 */
public class RefineYearlyPopulationCore
{
    /*
     * Captures objective values
     */
    public static class Objective
    {
        public double objective;
        public double monotonicityViolation;
        public double smoothnessViolation;
        public double binSumViolation;
        public double targetDiffViolation;

        public void copyFrom(Objective x)
        {
            this.objective = x.objective;
            this.monotonicityViolation = x.monotonicityViolation;
            this.smoothnessViolation = x.smoothnessViolation;
            this.binSumViolation = x.binSumViolation;
            this.targetDiffViolation = x.targetDiffViolation;
        }

        public Objective clone()
        {
            Objective x = new Objective();
            x.copyFrom(this);
            return x;
        }

        @Override
        public String toString()
        {
            return String.format("objective = %12.7e", objective);
        }

        public void print()
        {
            Util.out(String.format("diff = %9.4f   smoothness = %9.4f   monotonicity = %9.4e   sum = %9.4e   objective = %12.7e",
                                   targetDiffViolation,
                                   smoothnessViolation,
                                   monotonicityViolation,
                                   binSumViolation,
                                   objective));
        }
    }

    public static class OptimizerSettings
    {
        public double convergenceThreshold = 1e-8; // can range from 1e-6 to 1e-8 
        public double sigmaFraction = 0.001; // can range from 0.001 to 1.0
        public int minimumLambda = 2000; // from 4000 and beyond it gets very slow
    }

    final static double RegularPenalty = 1e1;
    final static double LargePenalty = 1e4;

    private final double[] p;
    private final double[] target_diff;
    private final double importance_smoothness;
    private final double importance_target_diff_matching;
    private final int nTunablePoints;
    private final int nFixedPoints;
    private final String title;
    private final double psum04;
    private final double psum59;
    private OptimizerSettings optimizerSettings;

    public boolean diagnostic = false;

    // track what we saw during the optimization scan
    private Objective min_seen_objective = null;
    private double[] min_seen_objective_p = null;

    public RefineYearlyPopulationCore(
            final double[] p,
            final double[] target_diff,
            final double arg_importance_smoothness,
            final double arg_importance_target_diff_matching,
            final int nTunablePoints,
            final int nFixedPoints,
            final String title,
            OptimizerSettings optimizerSettings)
    {
        this.p = Util.dup(p);
        this.target_diff = Util.dup(target_diff);
        this.importance_smoothness = arg_importance_smoothness * RegularPenalty;
        this.importance_target_diff_matching = arg_importance_target_diff_matching * RegularPenalty;
        this.nTunablePoints = nTunablePoints;
        this.nFixedPoints = nFixedPoints;
        this.title = title;
        this.psum04 = Util.sum_range(p, 0, 4);
        this.psum59 = Util.sum_range(p, 5, 9);

        if (optimizerSettings != null)
            this.optimizerSettings = optimizerSettings;
        else
            this.optimizerSettings = new OptimizerSettings();
    }

    public double[] refineSeries(
            OptimizerSettings argOptimizerSettings,
            final Level logLevel,
            final Objective initialObjective,
            Objective resultObjective)
    {
        // reset observed minimum
        min_seen_objective = null;
        min_seen_objective_p = null;

        if (argOptimizerSettings != null)
            this.optimizerSettings = argOptimizerSettings;

        if (resultObjective == null)
            resultObjective = new Objective();

        final double[] initialGuess = Util.splice(p, 0, nTunablePoints - 1);

        // Define the objective function
        MultivariateFunction objectiveFunction = new MultivariateFunction()
        {
            @Override
            public double value(double[] point)
            {
                // Combine p(0) to p(nTunablePoints - 1) with the fixed p(nTunablePoints) to p(nTunablePoints + nFixedPoints - 1)
                double[] fullP = fullP(point);

                // Calculate the objective value
                return calculateObjective(fullP, logLevel, null);
            }
        };

        // Define the constraints as penalty functions
        double[] lowerBounds = new double[nTunablePoints];
        double[] upperBounds = new double[nTunablePoints];
        Arrays.fill(lowerBounds, 0.0); // Lower bounds set to 0 (non-negative values)
        Arrays.fill(upperBounds, Double.POSITIVE_INFINITY); // No upper bounds
        adjustBounds(lowerBounds, upperBounds, p);

        double offset = 0.0; // Offset for penalty
        double[] scale = new double[nTunablePoints]; // Penalty weights for each variable
        Arrays.fill(scale, 1.0); // Uniform penalty weights

        MultivariateFunctionPenaltyAdapter constrainedObjective = new MultivariateFunctionPenaltyAdapter(objectiveFunction,
                                                                                                         lowerBounds,
                                                                                                         upperBounds,
                                                                                                         offset,
                                                                                                         scale);

        /*
         * Set up the CMAESOptimizer
         * 
         * May use checker = null to disable convergence checks
         */
        UniformRandomProvider random = RandomSource.JDK.create();
        ConvergenceChecker<PointValuePair> checker = new SimpleValueChecker(optimizerSettings.convergenceThreshold,
                                                                            optimizerSettings.convergenceThreshold);

        /*
         * Choose the population size (lambda).
         * Note that it has nothing to do with demographic population, but rather is number of 
         * candidate solutions generated in each iteration of the algorithm.
         */
        int lambda = chooseLambda();

        /*
         * Parameter checkFeasableCount sets how many candidates in a batch of lamba 
         * are to be checked against lower/upper bounds.
         * 
         * Value of 0 performs no checks of candidate feasibility (unbounded problems).
         * Value of 1 checks for feasibility only one candidate per batch of lambda).
         * Value of lambda checks all candidates for feasibility (strict bounds).
         * 
         * Values less then lambda can be used if infeasible solutions are occasionally acceptable.
         */
        int checkFeasableCount = lambda;

        CMAESOptimizer optimizer = new CMAESOptimizer(1_000_000, // Max iterations
                                                      optimizerSettings.convergenceThreshold, // Stop fitness
                                                      true, // Active CMA 
                                                      0, // Diagonal only (0 means full covariance)
                                                      checkFeasableCount, // Check feasible count
                                                      random, // Random generator
                                                      false, // No statistics
                                                      checker // Convergence checker
        );

        // Define the input sigma (step sizes for each variable)
        double[] inputSigma = new double[nTunablePoints];
        Arrays.fill(inputSigma, 1.0); // Initial step size for each variable
        adjustInputSigma(inputSigma);

        // Define bounds for the variables
        double[] lowerBoundsForOptimizer = new double[nTunablePoints];
        double[] upperBoundsForOptimizer = new double[nTunablePoints];
        Arrays.fill(lowerBoundsForOptimizer, 0.0); // Lower bounds set to 0 (non-negative values)
        Arrays.fill(upperBoundsForOptimizer, Double.POSITIVE_INFINITY); // No upper bounds
        adjustBounds(lowerBounds, upperBounds, p);

        // Perform the optimization
        PointValuePair result = optimizer.optimize(MaxEval.unlimited(), // Maximum number of evaluations
                                                   new ObjectiveFunction(constrainedObjective),
                                                   GoalType.MINIMIZE,
                                                   new InitialGuess(initialGuess),
                                                   new Sigma(inputSigma), // Step sizes
                                                   new PopulationSize(chooseLambda()), // Population size
                                                   new SimpleBounds(lowerBoundsForOptimizer, upperBoundsForOptimizer) // Bounds
        );

        // result of the optimization
        double[] px = result.getPoint();

        double[] fullP = fullP(px);

        if (logLevel == Level.TRACE || logLevel == Level.ALL || logLevel == Level.DEBUG)
        {
            // debugging output
            Util.out("");
            Util.out("RefineYearlyPopulationBase completed for " + title);
            Util.out("Objective values for the intitial curve (" + title + "):");
            calculateObjective(p, Level.TRACE, initialObjective);

            Util.out("Objective values for the result curve (" + title + "):");
            calculateObjective(fullP, Level.TRACE, resultObjective);
        }
        else
        {
            // caclculate objective values for the caller's use
            if (initialObjective != null)
                calculateObjective(p, Level.INFO, initialObjective);

            if (resultObjective != null)
                calculateObjective(fullP, Level.INFO, resultObjective);
        }

        /*
         * Did we observe a better value vector during optimization scan 
         * than the result returned by the optimizer?
         */
        if (min_seen_objective != null && min_seen_objective.objective < resultObjective.objective)
        {
            if (logLevel == Level.TRACE || logLevel == Level.ALL || logLevel == Level.DEBUG || diagnostic)
            {
                /*
                 * This is not a bug of CMAESOptimizer, but rather its "feature".
                 * 
                 * It is a common characteristic of the Covariance Matrix Adaptation Evolution Strategy (CMA-ES) algorithm.
                 * 
                 * CMA-ES is a stochastic algorithm, meaning it relies on random sampling of candidate solutions.
                 * Due to this randomness, the algorithm may not always return the absolute best point it has evaluated.
                 * 
                 * The algorithm is designed to return the mean of the final search distribution, which is its best estimate 
                 * of the optimal solution based on the information gathered during the optimization process. 
                 * The best-evaluated point is not guaranteed to be the same as the mean, especially in noisy or complex optimization landscapes.
                 * 
                 * CMA-ES Focuses on the Mean of the Distribution:
                 * 
                 * CMA-ES maintains a search distribution (a multivariate Gaussian distribution) over the search space.
                 * The algorithm updates the mean of this distribution iteratively, based on the best-performing candidate solutions (offspring).
                 * The point returned by CMAESOptimizer is typically the mean of the final search distribution, 
                 * not necessarily the best point ever evaluated.
                 * 
                 * During the optimization process, CMA-ES evaluates many candidate solutions (offspring) and tracks their objective function values.
                 * The best-evaluated point (the one with the lowest objective function value) is not always the same as the mean 
                 * of the final search distribution. The mean represents the algorithm’s current "best estimate" of the optimal solution, 
                 * but it may not correspond to the best point ever evaluated.
                 * 
                 * CMA-ES balances exploration (searching new regions of the search space) and exploitation (refining the search around promising regions).
                 * The algorithm may explore points that are better than the current mean but do not immediately update the mean to those points. 
                 * Instead, it uses information from those points to adapt the search distribution.
                 * 
                 * To retrieve the best-evaluated point, we need to track it manually during the optimization process, as we do here.                 
                 */
                String msg = String.format("Seen better choice during optimization scan than was ultimately reported by the optimizer: %f < %f",
                                           min_seen_objective.objective, resultObjective.objective);
                Util.err(msg);
            }

            // return it as the result
            if (Util.True)
            {
                px = Util.splice(min_seen_objective_p, 0, nTunablePoints - 1);
                resultObjective.copyFrom(min_seen_objective);

                if (logLevel == Level.TRACE || logLevel == Level.ALL || logLevel == Level.DEBUG)
                {
                    Util.out("Objective values for the result override curve (" + title + "):");
                    resultObjective.print();
                }
            }
        }

        // Return the optimized values
        return px;
    }

    // Combine p(0) to p(nTunablePoints - 1) with the fixed p(nTunablePoints) to p(nTunablePoints + nFixedPoints - 1)
    private double[] fullP(final double[] points)
    {
        int plength = Math.max(10, nTunablePoints + nFixedPoints);
        double[] fullP = Arrays.copyOf(points, plength);
        System.arraycopy(p, nTunablePoints, fullP, nTunablePoints, plength - nTunablePoints);
        return fullP;
    }

    /* ---------------------------------------------------------------------------------------- */

    // objectibe function
    private double calculateObjective(final double[] p, final Level logLevel, Objective ov)
    {
        double monotonicityViolation = calculateMonotonicityViolation(p);

        // Calculate smoothness violation
        double smoothnessViolation = calculateSmoothnessViolation(p);

        // Add penalties for violating the bin sum constraints
        double binSumViolation = calculateBinSumViolation(p);

        // Calculate target difference violation
        double targetDiffViolation = calculateTargetDiffViolation(p);

        // Return weighted sum of violations
        double objective = monotonicityViolation + binSumViolation +
                           importance_smoothness * smoothnessViolation +
                           importance_target_diff_matching * targetDiffViolation;

        if (ov == null)
            ov = new Objective();

        ov.objective = objective;
        ov.binSumViolation = binSumViolation;
        ov.monotonicityViolation = monotonicityViolation;
        ov.targetDiffViolation = targetDiffViolation;
        ov.smoothnessViolation = smoothnessViolation;

        if (logLevel == Level.TRACE || logLevel == Level.ALL)
            ov.print();

        // capture minimum seen state
        if (min_seen_objective == null || objective < min_seen_objective.objective)
        {
            min_seen_objective = ov.clone();
            min_seen_objective_p = Util.dup(p);
        }

        return objective;
    }

    private double calculateMonotonicityViolation(double[] p)
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

    private double calculateBinSumViolation(double[] p)
    {
        double sum04 = Arrays.stream(p, 0, 5).sum(); // Sum of p(0) to p(4)
        double sum59 = Arrays.stream(p, 5, 10).sum(); // Sum of p(5) to p(9)
        double penalty04 = LargePenalty * Math.abs(sum04 - psum04) / psum04; // Large penalty for violating psum04
        double penalty59 = LargePenalty * Math.abs(sum59 - psum59) / psum59; // Large penalty for violating psum59
        return penalty04 + penalty59;
    }

    private double calculateSmoothnessViolation(double[] p)
    {
        double smoothnessViolation = 0.0;

        p = Util.normalize(p);

        // under rare conditions nFixedPoints can be zero
        for (int i = 1; i < p.length - 1 && i <= nTunablePoints - 1 + Math.max(0, nFixedPoints - 1); i++)
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

    private double calculateTargetDiffViolation(double[] p)
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
    private void adjustBounds(double[] lowerBounds, double[] upperBounds, final double[] p)
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
    private void adjustInputSigma(double[] inputSigma)
    {
        for (int k = 0; k <= 4 && k < inputSigma.length; k++)
            inputSigma[k] = psum04 * optimizerSettings.sigmaFraction;

        for (int k = 5; k <= 9 && k < inputSigma.length; k++)
            inputSigma[k] = psum59 * optimizerSettings.sigmaFraction;
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
    private int chooseLambda()
    {
        // Default formula for lambda
        int lambda = 4 + (int) (3 * Math.log(nTunablePoints));

        // impose mimimum
        lambda = Math.max(lambda, optimizerSettings.minimumLambda);

        return lambda;
    }

    /*======================================================================================================== */

    public class OptimizationResult extends Objective
    {
        public double[] px;
        public OptimizerSettings optimizerSettings;
    }

    /*
     * Try to solve the problem using various settings for the optimizer 
     */
    public double[] refineSeriesIterative(Level outerLogLevel, Level innerLogLevel)
    {
        /*
         * From lambda 4,000 and above it gets really slow
         */
        final int lambdas[] = { 200, 500, 1000, 2000 };
        final double sigmas[] = { 0.000001, 0.00001, 0.0001, 0.001, 0.005, 0.01, 0.05, 0.1, 0.2, 0.3, 0.5, 0.7, 1.0 };

        List<OptimizationResult> results = new ArrayList<>();
        OptimizationResult very_initial = null;

        for (int lambda : lambdas)
        {
            if (outerLogLevel == Level.TRACE || outerLogLevel == Level.ALL)
                Util.out("");

            for (double sigma : sigmas)
            {
                OptimizerSettings optimizerSettings = new OptimizerSettings();
                optimizerSettings.sigmaFraction = sigma;
                optimizerSettings.minimumLambda = lambda;
                optimizerSettings.convergenceThreshold = 1e-8;

                OptimizationResult result = new OptimizationResult();
                result.optimizerSettings = optimizerSettings;

                OptimizationResult initialObjective = new OptimizationResult();
                initialObjective.optimizerSettings = optimizerSettings;

                result.px = refineSeries(optimizerSettings, innerLogLevel, initialObjective, result);

                initialObjective.px = Util.splice(p, 0, nTunablePoints - 1);
                results.add(initialObjective);
                if (very_initial == null)
                    very_initial = initialObjective;

                double[] fullP = fullP(result.px);

                // check if it is the same as the initial curve
                String same = "";
                if (Util.same(fullP, p))
                    same = "  same-as-initial";

                // check if the returned curve is monotonically decreasing
                String nonmonotnic = "";
                if (!verifyMonotonicity(fullP))
                    nonmonotnic = "  non-monotonic";

                // check if the returned curve presverves bins sums
                String preserves = "";
                double sum04 = Util.sum_range(fullP, 0, 4);
                double sum59 = Util.sum_range(fullP, 5, 9);
                if (Util.differ(sum04, psum04, 0.001) || Util.differ(sum59, psum59, 0.001))
                    preserves = "  non-sum-preserving";

                if (outerLogLevel == Level.TRACE || outerLogLevel == Level.ALL)
                {
                    Util.out(String.format("lambda=%-4d  sigma=%8.6f  initial objective = %12.7e  result objective = %12.7e%s%s%s",
                                           lambda,
                                           sigma,
                                           initialObjective.objective,
                                           result.objective,
                                           same,
                                           nonmonotnic,
                                           preserves));
                }

                if (same.isEmpty() &&
                    nonmonotnic.isEmpty() &&
                    preserves.isEmpty())
                {
                    results.add(result);
                }
            }
        }

        OptimizationResult min_result = null;

        for (OptimizationResult r : results)
        {
            if (min_result == null || r.objective < min_result.objective)
                min_result = r;
        }

        if (outerLogLevel == Level.TRACE || outerLogLevel == Level.ALL || outerLogLevel == Level.DEBUG)
        {
            Util.out("");
            Util.out("Objective values for the initial curve (" + title + "):");
            very_initial.print();
            Util.out("Objective values for the final itervative result curve (" + title + "):");
            min_result.print();
        }

        return min_result.px;
    }

    private boolean verifyMonotonicity(double[] p)
    {
        for (int k = 0; k < nTunablePoints; k++)
        {
            if (p[k] < p[k + 1])
                return false;
        }

        return true;
    }

    /*======================================================================================================== */

    /*
     * Example/test code
     */
    public static void main(String[] args)
    {
        // test_1(false);
        // test_1(true);

        // test_2(false);
        test_2(true);
    }

    @SuppressWarnings("unused")
    private static void test_1(boolean iterative)
    {
        double p[] = { 3079.1064761352536, 2863.741162691683, 2648.375849248112, 2433.010535804541, 2217.645222360971, 2002.2799089174,
                       1831.1316029723425, 1749.4931260194671, 1757.808507358823, 1852.286854731967 };

        double target_diff[] = { 0.5635055255718324, 0.1853508095605243, 0.08306347982523773, 0.04790542277049602, 0.028270367514777694,
                                 0.02770496016448214, 0.021691081984065795 };

        double importance_smoothness = 0.80;
        double importance_target_diff_matching = 1.0 - importance_smoothness;

        int nTunablePoints = 7;
        int nFixedPoints = 1;

        RefineYearlyPopulationCore rc = new RefineYearlyPopulationCore(p,
                                                                       target_diff,
                                                                       importance_smoothness,
                                                                       importance_target_diff_matching,
                                                                       nTunablePoints,
                                                                       nFixedPoints,
                                                                       "example test_2",
                                                                       null);

        rc.diagnostic = true;

        if (iterative)
        {
            double[] px = rc.refineSeriesIterative(Level.TRACE, Level.INFO);
            Util.out("");
            Util.out("Finished iterative_test_1");
        }
        else
        {
            double[] px = rc.refineSeries(null, Level.TRACE, null, null);
            Util.out("Finished test_1");
        }
    }

    @SuppressWarnings("unused")
    private static void test_2(boolean iterative)
    {
        double p[] = { 2758.9111513137814, 2540.455070553185, 2321.998989792589, 2103.5429090319926, 1885.0868282713964, 1666.6307475108001,
                       1487.948517512541, 1392.6120548630918, 1379.5241192732535, 1444.2845608403145 };

        double target_diff[] = { 0.47382170458256234, 0.19957548710133885, 0.09807336453684555, 0.0638402089909655, 0.048492434962446936,
                                 0.03624687057799064, 0.027865462065962774, 0.021769892239033417 };

        double importance_smoothness = 0.95;
        double importance_target_diff_matching = 1.0 - importance_smoothness;

        int nTunablePoints = 8;
        int nFixedPoints = 1;

        RefineYearlyPopulationCore rc = new RefineYearlyPopulationCore(p,
                                                                       target_diff,
                                                                       importance_smoothness,
                                                                       importance_target_diff_matching,
                                                                       nTunablePoints,
                                                                       nFixedPoints,
                                                                       "example test_2",
                                                                       null);

        rc.diagnostic = true;

        if (iterative)
        {
            double[] px = rc.refineSeriesIterative(Level.TRACE, Level.INFO);
            Util.out("");
            Util.out("Finished iterative_test_2");
        }
        else
        {
            double[] px = rc.refineSeries(null, Level.TRACE, null, null);
            Util.out("Finished test_2");
        }
    }
}