package rtss.data.mortality.laws;

import org.apache.commons.math4.legacy.analysis.ParametricUnivariateFunction;
import org.apache.commons.math4.legacy.optim.ConvergenceChecker;
import org.apache.commons.math4.legacy.optim.SimpleValueChecker;
import org.apache.commons.math4.legacy.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math4.legacy.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.math4.legacy.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math4.legacy.optim.InitialGuess;
import org.apache.commons.math4.legacy.optim.SimpleBounds;
import org.apache.commons.math4.legacy.optim.MaxEval;
// import org.apache.commons.math4.legacy.optim.nonlinear.scalar.MultivariateOptimizer;
//import org.apache.commons.math4.legacy.core.Pair;
import org.apache.commons.math4.legacy.optim.PointValuePair;

import java.util.ArrayList;
import java.util.List;

public class HeligmanPollardFitter
{

    // Define the Heligman-Pollard function
    static class HeligmanPollardFunction implements ParametricUnivariateFunction
    {
        @Override
        public double value(double x, double... parameters)
        {
            double A = parameters[0];
            double B = parameters[1];
            double C = parameters[2];
            double D = parameters[3];
            double E = parameters[4];
            double F = parameters[5];
            double G = parameters[6];
            double H = parameters[7];

            double term1 = Math.pow(A, Math.pow(x + B, C));
            double term2 = D * Math.exp(-E * Math.pow(Math.log(x) - Math.log(F), 2));
            double term3 = G * Math.pow(H, x);

            double qx = term1 + term2 + term3;
            return qx / (1 + qx); // Convert to q(x)
        }

        @Override
        public double[] gradient(double x, double... parameters)
        {
            // Implement the gradient if needed for optimization
            // For simplicity, we'll let the optimizer approximate the gradient
            return new double[8]; // Placeholder
        }
    }

    // Fit the Heligman-Pollard model to the binned data
    public static double[] fitHeligmanPollard(List<HPBin> bins, double[] initialGuess)
    {
        // Check for bin consistency
        checkBinConsistency(bins);

        // Prepare the data for optimization
        final int totalPoints; // Declare as final for use in lambda
        {
            int tempTotalPoints = 0;
            for (HPBin bin : bins)
            {
                tempTotalPoints += 100 * bin.bin_age_width; // 100 points per year
            }
            totalPoints = tempTotalPoints;
        }

        double[] x = new double[totalPoints];
        double[] y = new double[totalPoints];

        int index = 0;
        for (HPBin bin : bins)
        {
            double start = bin.bin_starting_age;
            double end = start + bin.bin_age_width;
            double step = 1.0 / 100; // Step size for 100 points per year

            for (double age = start; age < end; age += step)
            {
                x[index] = age;
                y[index] = bin.average_qx;
                index++;
            }
        }

        // Define the objective function
        ParametricUnivariateFunction model = new HeligmanPollardFunction();
        ObjectiveFunction objective = new ObjectiveFunction(params ->
        {
            double totalDeviation = 0.0;

            // Iterate over bins
            for (HPBin bin : bins)
            {
                double start = bin.bin_starting_age;
                double end = start + bin.bin_age_width;
                double step = 1.0 / 100; // Step size for 100 points per year
                double sumPredicted = 0.0;
                int count = 0;

                // Calculate the average predicted value for the bin
                for (double age = start; age < end; age += step)
                {
                    sumPredicted += model.value(age, params);
                    count++;
                }

                double averagePredicted = sumPredicted / count;
                totalDeviation += Math.abs(averagePredicted - bin.average_qx);
            }

            return totalDeviation;
        });

        // Set up the optimizer
        UniformRandomProvider random = RandomSource.JDK.create(); // Use UniformRandomProvider
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

        // Define bounds for the parameters (optional but recommended)
        double[] lowerBounds = { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
        double[] upperBounds = { 1.0, 100.0, 10.0, 1.0, 10.0, 100.0, 1.0, 1.0 };
        SimpleBounds bounds = new SimpleBounds(lowerBounds, upperBounds);

        // Run the optimizer
        PointValuePair optimum = optimizer.optimize(
                                                    objective,
                                                    GoalType.MINIMIZE,
                                                    new InitialGuess(initialGuess),
                                                    bounds,
                                                    new MaxEval(1000));

        // Return the fitted parameters
        return optimum.getPoint();
    }

    // Check bin consistency
    private static void checkBinConsistency(List<HPBin> bins)
    {
        if (bins.isEmpty())
        {
            throw new IllegalArgumentException("The list of bins is empty.");
        }

        double previousEnd = bins.get(0).bin_starting_age;
        for (int i = 1; i < bins.size(); i++)
        {
            HPBin currentBin = bins.get(i);
            if (currentBin.bin_starting_age != previousEnd)
            {
                throw new IllegalArgumentException(
                                                   "Bins are not continuous. Gap between bin " + (i - 1) + " and bin " + i + ".");
            }
            previousEnd = currentBin.bin_starting_age + currentBin.bin_age_width;
        }
    }

    // Define the HPBin structure
    static class HPBin
    {
        double bin_starting_age;
        double bin_age_width;
        double average_qx;

        HPBin(double bin_starting_age, double bin_age_width, double average_qx)
        {
            this.bin_starting_age = bin_starting_age;
            this.bin_age_width = bin_age_width;
            this.average_qx = average_qx;
        }
    }

    // Example usage
    public static void main(String[] args)
    {
        // Create some example binned data
        List<HPBin> bins = new ArrayList<>();
        bins.add(new HPBin(0, 1, 0.01)); // Bin 0-1 years
        bins.add(new HPBin(1, 5, 0.005)); // Bin 1-6 years
        bins.add(new HPBin(6, 0.5, 0.003)); // Bin 6-6.5 years

        // Provide an initial guess for the parameters A-H
        double[] initialGuess = { 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1 };

        // Fit the model
        double[] fittedParameters = fitHeligmanPollard(bins, initialGuess);

        // Print the fitted parameters
        System.out.println("Fitted Parameters:");
        System.out.println("A: " + fittedParameters[0]);
        System.out.println("B: " + fittedParameters[1]);
        System.out.println("C: " + fittedParameters[2]);
        System.out.println("D: " + fittedParameters[3]);
        System.out.println("E: " + fittedParameters[4]);
        System.out.println("F: " + fittedParameters[5]);
        System.out.println("G: " + fittedParameters[6]);
        System.out.println("H: " + fittedParameters[7]);
    }
}