package rtss.data.mortality.laws;

import org.apache.commons.math4.legacy.analysis.ParametricUnivariateFunction;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math4.legacy.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math4.legacy.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math4.legacy.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;
import org.apache.commons.math4.legacy.optim.ConvergenceChecker;
import org.apache.commons.math4.legacy.optim.SimpleVectorValueChecker;
import org.apache.commons.math4.legacy.optim.PointVectorValuePair;
import org.apache.commons.math4.legacy.core.Pair;

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

        // Define the optimization problem
        ParametricUnivariateFunction model = new HeligmanPollardFunction();
        MultivariateJacobianFunction modelFunction = params ->
        {
            RealVector residuals = new ArrayRealVector(totalPoints);
            RealMatrix jacobian = null; // Approximated by the optimizer

            for (int i = 0; i < totalPoints; i++)
            {
                double predicted = model.value(x[i], params.toArray());
                residuals.setEntry(i, Math.abs(predicted - y[i])); // Absolute deviation
            }

            return new Pair<>(residuals, jacobian);
        };

        // Create a custom ConvergenceChecker for LeastSquaresProblem.Evaluation
        ConvergenceChecker<LeastSquaresProblem.Evaluation> checker = new ConvergenceChecker<LeastSquaresProblem.Evaluation>()
        {
            private final SimpleVectorValueChecker delegate = new SimpleVectorValueChecker(1e-6, 1e-6);

            @Override
            public boolean converged(int iteration, LeastSquaresProblem.Evaluation previous, LeastSquaresProblem.Evaluation current)
            {
                // Convert Evaluation to PointVectorValuePair
                PointVectorValuePair previousPair = new PointVectorValuePair(previous.getPoint().toArray(), previous.getResiduals().toArray(), true);
                PointVectorValuePair currentPair = new PointVectorValuePair(current.getPoint().toArray(), current.getResiduals().toArray(), true);

                return delegate.converged(iteration, previousPair, currentPair);
            }
        };

        // Set up the optimizer
        LeastSquaresProblem problem = new LeastSquaresBuilder()
                .start(initialGuess)
                .model(modelFunction)
                .target(new ArrayRealVector(new double[totalPoints])) // Target is zero (minimize deviations)
                .lazyEvaluation(false)
                .maxEvaluations(1000)
                .maxIterations(1000)
                .checker(checker)
                .build();

        // Run the optimizer
        LeastSquaresOptimizer optimizer = new LevenbergMarquardtOptimizer();
        LeastSquaresOptimizer.Optimum optimum = optimizer.optimize(problem);

        // Return the fitted parameters
        return optimum.getPoint().toArray();
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