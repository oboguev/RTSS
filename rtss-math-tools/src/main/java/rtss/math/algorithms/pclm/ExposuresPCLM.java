package rtss.math.algorithms.pclm;

import Jama.Matrix;
import rtss.data.bin.Bin;
import rtss.data.curves.CurveUtil;
import rtss.util.Util;

/**
 * Exposure-Aware Penalized Composite Link Model (PCLM) for mortality rates.
 *
 * This extends the basic PCLM algorithm to handle mortality rates with known
 * population exposures (population counts by age). The key difference from the
 * basic PCLM is in the composition matrix, which now weights contributions by
 * exposure rather than using uniform weights.
 *
 * Implementation based on:
 * S. Rizzi, J. Gampe, and P. Eilers, "Efficient Estimation of Smooth
 * Distributions From Coarsely Grouped Data", American Journal of Epidemiology,
 * Vol. 182, No. 2, 2015, pp. 138-147.
 *
 * Key differences from basic PCLM:
 * - Input: bins contain mortality RATES (not counts), plus exposure array
 * - Composition matrix: C[i,j] = exposure[j] (not 1.0) for j in bin i
 * - Observed values: converted from rates to death counts using exposures
 * - Output: mortality rates m[x] where deaths[x] = m[x] * exposure[x]
 *
 * For a grouped age interval i, the model aggregates expected deaths as:
 *   expected_deaths[i] = sum over x in bin i of (exposure[x] * rate[x])
 *
 * The grouped rate constraint becomes:
 *   bin_rate[i] = sum(exposure[x] * rate[x]) / sum(exposure[x])
 *
 * This is an exposure-weighted average, not a simple arithmetic average.
 * This distinction is crucial for wide age groups (e.g., 85-100) where
 * exposure drops sharply with age.
 */
public class ExposuresPCLM
{
    public static int MAX_ITERATIONS = 50;
    public static double CONVERGENCE_THRESHOLD = 1e-6;

    private final Bin[] bins;
    private final double[] exposures;
    private final double lambda;
    private final int ppy;

    private final int nBins; // Number of input bins (I in the paper)
    private final int nPoints; // Number of output points (J in the paper)

    private int maxIterations = MAX_ITERATIONS;
    private double convergenceThreshold = CONVERGENCE_THRESHOLD;

    /**
     * Creates a new exposure-aware PCLM instance for mortality rate disaggregation.
     *
     * @param bins Array of consecutive age bins with fields:
     *             age_x1 - starting age (inclusive)
     *             age_x2 - ending age (inclusive)
     *             avg - average mortality RATE in this bin (e.g., deaths per person)
     * @param exposures Population exposure counts for each disaggregated point.
     *                  Length must equal ppy * (age_range), where age_range is
     *                  (last_bin.age_x2 - first_bin.age_x1 + 1).
     *                  For example, with bins covering ages 0-100 and ppy=1,
     *                  exposures should have 101 elements (one per year of age).
     *                  With ppy=4, exposures should have 404 elements (one per quarter).
     * @param lambda Smoothing parameter (larger = smoother result)
     * @param ppy Points per year (typically 1 for annual, 4 for quarterly, 12 for monthly)
     */
    public ExposuresPCLM(Bin[] bins, double[] exposures, double lambda, int ppy)
    {
        if (bins == null || bins.length == 0)
            throw new IllegalArgumentException("bins array cannot be null or empty");

        if (exposures == null || exposures.length == 0)
            throw new IllegalArgumentException("exposures array cannot be null or empty");

        if (lambda < 0)
            throw new IllegalArgumentException("lambda must be non-negative");

        if (ppy < 1)
            throw new IllegalArgumentException("ppy must be at least 1");

        this.bins = bins;
        /*
         * normalize exposures to avoid numeric overflow/underflow,
         * and since we care only about relative distribution
         * rather than actual counts 
         */
        this.exposures = Util.divide(exposures, Util.sum(exposures));
        this.lambda = lambda;
        this.ppy = ppy;
        this.nBins = bins.length;

        // Calculate expected number of output points
        int firstAge = bins[0].age_x1;
        int lastAge = bins[nBins - 1].age_x2;
        int expectedPoints = ppy * (lastAge - firstAge + 1);

        // Verify exposures array length
        if (exposures.length != expectedPoints)
        {
            throw new IllegalArgumentException(String.format("exposures array length mismatch: expected %d (ppy=%d × age_range=%d) but got %d",
                                                             expectedPoints, ppy, (lastAge - firstAge + 1), exposures.length));
        }

        this.nPoints = expectedPoints;

        // Verify all exposures are non-negative
        for (int i = 0; i < exposures.length; i++)
        {
            if (exposures[i] < 0)
            {
                throw new IllegalArgumentException(String.format("exposure[%d] is negative: %.4f", i, exposures[i]));
            }
        }
    }

    public ExposuresPCLM setMaxIterations(int maxIterations)
    {
        this.maxIterations = maxIterations;
        return this;
    }

    public ExposuresPCLM setConvergenceThreshold(double convergenceThreshold)
    {
        this.convergenceThreshold = convergenceThreshold;
        return this;
    }

    /**
     * Executes the exposure-aware PCLM algorithm to disaggregate mortality rates.
     *
     * The algorithm converts grouped mortality rates to grouped death counts using
     * exposures, then estimates one-year (or finer) mortality rates such that:
     *   estimated_deaths[i] ≈ sum over x in bin i of (exposure[x] * rate[x])
     *
     * @return Array of disaggregated mortality rates of length ppy * (age_range)
     * @throws Exception if the algorithm fails to converge or encounters numerical issues
     */
    public double[] pclm() throws Exception
    {
        // Build the exposure-weighted composition matrix C (nBins × nPoints)
        Matrix C = buildExposureCompositionMatrix();

        // Build the identity matrix X (nPoints × nPoints)
        Matrix X = Matrix.identity(nPoints, nPoints);

        // Build the penalty difference matrix D ((nPoints - 2) × nPoints)
        Matrix D = buildDifferenceMatrix(nPoints, 2);

        // Convert grouped mortality rates to grouped death counts
        // D[i] = rate[i] * sum(exposures in bin i)
        double[] y = new double[nBins];
        int firstAge = bins[0].age_x1;

        for (int i = 0; i < nBins; i++)
        {
            Bin bin = bins[i];
            int pointStart = (bin.age_x1 - firstAge) * ppy;
            int pointEnd = (bin.age_x2 - firstAge + 1) * ppy - 1;

            // Sum exposures in this bin
            double exposureSum = 0.0;
            for (int j = pointStart; j <= pointEnd && j < nPoints; j++)
            {
                exposureSum += exposures[j];
            }

            // Convert rate to death count: deaths = rate × total_exposure
            y[i] = bin.avg * exposureSum;
        }

        // Perform IRLS iterations to estimate β
        double[] beta = performIRLS(y, C, X, D);

        // Compute γ = exp(X × β) - these are the mortality rates
        Matrix betaMatrix = new Matrix(beta, nPoints);
        Matrix eta = X.times(betaMatrix);
        double[] gamma = new double[nPoints];
        for (int j = 0; j < nPoints; j++)
        {
            gamma[j] = validate(Math.exp(eta.get(j, 0)));
        }

        // Apply rounding avoidance to maintain conservation property
        CurveUtil.avoidDecompositionRounding(gamma, bins);

        return validate(gamma);
    }

    /**
     * Builds the exposure-weighted composition matrix C.
     * C is an (nBins × nPoints) matrix where C[i,j] = exposure[j] if point j
     * belongs to bin i, and 0 otherwise.
     *
     * This differs from basic PCLM where C[i,j] would be 1.0.
     * The exposure weighting ensures that the aggregation relationship:
     *   deaths[i] = sum over j in bin i of (C[i,j] * rate[j])
     * correctly represents exposure-weighted mortality.
     */
    private Matrix buildExposureCompositionMatrix()
    {
        Matrix C = new Matrix(nBins, nPoints);

        int firstAge = bins[0].age_x1;

        for (int i = 0; i < nBins; i++)
        {
            Bin bin = bins[i];

            // Calculate which points belong to this bin
            int pointStart = (bin.age_x1 - firstAge) * ppy;
            int pointEnd = (bin.age_x2 - firstAge + 1) * ppy - 1;

            // Set C[i, j] = exposure[j] for all points j that belong to bin i
            for (int j = pointStart; j <= pointEnd && j < nPoints; j++)
            {
                C.set(i, j, exposures[j]);
            }
        }

        return C;
    }

    /**
     * Builds the difference matrix D for enforcing smoothness.
     * D computes the second-order differences: [1, -2, 1] pattern.
     *
     * @param n Dimension (number of parameters)
     * @param deg Degree of differencing (typically 2)
     * @return Difference matrix of size ((n - deg) × n)
     */
    private Matrix buildDifferenceMatrix(int n, int deg)
    {
        // Start with identity matrix
        Matrix D = Matrix.identity(n, n);

        // Apply differencing 'deg' times
        for (int d = 0; d < deg; d++)
        {
            int rows = D.getRowDimension() - 1;
            int cols = D.getColumnDimension();
            Matrix D_new = new Matrix(rows, cols);

            for (int i = 0; i < rows; i++)
            {
                for (int j = 0; j < cols; j++)
                {
                    D_new.set(i, j, D.get(i + 1, j) - D.get(i, j));
                }
            }
            D = D_new;
        }

        return D;
    }

    /**
     * Performs Iteratively Reweighted Least Squares (IRLS) to estimate β.
     * This implements the algorithm from Appendix 1 of the Rizzi et al. paper.
     *
     * @param y Observed bin death counts (converted from rates using exposures)
     * @param C Exposure-weighted composition matrix
     * @param X Basis matrix (identity or B-spline)
     * @param D Difference matrix for penalty
     * @return Estimated coefficients β
     * @throws Exception if convergence fails
     */
    private double[] performIRLS(double[] y, Matrix C, Matrix X, Matrix D) throws Exception
    {
        int nx = X.getColumnDimension();
        double la2 = validate(Math.sqrt(lambda));

        // Initialize β with log of average rate
        // Start with total deaths / total exposure as initial rate estimate
        double totalDeaths = 0.0;
        for (double yi : y)
        {
            totalDeaths += yi;
        }
        double totalExposure = 0.0;
        for (double exp : exposures)
        {
            totalExposure += exp;
        }

        double avgRate = totalDeaths / totalExposure;
        double bstart = Math.log(Math.max(avgRate, 1e-10)); // Avoid log(0)

        validate(totalDeaths);
        validate(totalExposure);
        validate(avgRate);
        validate(bstart);

        double[] b = new double[nx];
        for (int j = 0; j < nx; j++)
        {
            b[j] = bstart;
        }

        // IRLS iterations

        for (int it = 0; it < maxIterations; it++)
        {
            double[] b0 = validate(b.clone());

            // Compute η = X × β
            Matrix betaMatrix = validate(new Matrix(b, nx));
            Matrix eta = validate(X.times(betaMatrix));

            // Compute γ = exp(η) - these are the rates
            double[] gam = new double[nx];
            for (int j = 0; j < nx; j++)
            {
                gam[j] = validate(Math.exp(eta.get(j, 0)));
            }

            // Compute μ = C × γ - these are the expected death counts
            Matrix gamMatrix = validate(new Matrix(gam, nx));
            Matrix mu = validate(C.times(gamMatrix));
            double[] muArray = validate(mu.getColumnPackedCopy());

            // Build weights: w = [1/μ_1, ..., 1/μ_I, la2, ..., la2]
            double[] w = new double[nBins + nx - 2];
            for (int i = 0; i < nBins; i++)
            {
                // Avoid division by zero
                w[i] = validate(1.0 / Math.max(muArray[i], 1e-10));
            }
            for (int i = nBins; i < w.length; i++)
            {
                w[i] = validate(la2);
            }

            // Compute Γ (Gamma matrix) = diag(γ)
            Matrix Gamma = validate(new Matrix(nx, nx));
            for (int j = 0; j < nx; j++)
            {
                Gamma.set(j, j, gam[j]);
            }

            // Compute Q = C × Γ × X
            Matrix Q = validate(C.times(Gamma).times(X));

            // Build z = [y - μ + Q×β, 0, ..., 0]
            Matrix Qb = validate(Q.times(betaMatrix));
            double[] z = new double[nBins + nx - 2];
            for (int i = 0; i < nBins; i++)
            {
                z[i] = validate(y[i] - muArray[i] + Qb.get(i, 0));
            }
            // Remaining elements are already 0

            // Stack matrices: [Q; D] and solve weighted least squares
            Matrix A = validate(stackMatrices(Q, D));
            Matrix zMatrix = validate(new Matrix(z, z.length));

            // Weighted least squares: A^T W A β = A^T W z
            b = validate(solveWeightedLeastSquares(A, zMatrix, w));

            // Check convergence
            double db = 0.0;
            for (int j = 0; j < nx; j++)
            {
                db = validate(Math.max(db, Math.abs(b[j] - b0[j])));
            }

            if (db < convergenceThreshold)
            {
                return b;
            }
        }

        throw new Exception("ExposuresPCLM algorithm failed to converge after " + maxIterations + " iterations");
    }

    /**
     * Stacks two matrices vertically: [A; B]
     */
    private Matrix stackMatrices(Matrix A, Matrix B)
    {
        int rowsA = A.getRowDimension();
        int rowsB = B.getRowDimension();
        int cols = A.getColumnDimension();

        Matrix result = new Matrix(rowsA + rowsB, cols);

        for (int i = 0; i < rowsA; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                result.set(i, j, A.get(i, j));
            }
        }

        for (int i = 0; i < rowsB; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                result.set(rowsA + i, j, B.get(i, j));
            }
        }

        return result;
    }

    /**
     * Solves weighted least squares: A^T W A β = A^T W z
     * where W is a diagonal weight matrix.
     */
    private double[] solveWeightedLeastSquares(Matrix A, Matrix z, double[] w)
    {
        int m = A.getRowDimension();
        int n = A.getColumnDimension();

        Util.unused(n);
        validate(w);

        // Build diagonal weight matrix W
        Matrix W = new Matrix(m, m);
        for (int i = 0; i < m; i++)
        {
            W.set(i, i, w[i]);
        }

        // Compute A^T W A
        Matrix AtW = A.transpose().times(W);
        Matrix AtWA = AtW.times(A);

        // Compute A^T W z
        Matrix AtWz = AtW.times(z);

        // Solve the system
        Matrix betaMatrix = AtWA.solve(AtWz);

        return validate(betaMatrix.getColumnPackedCopy());
    }

    private double validate(double v)
    {
        if (!Double.isFinite(v))
            throw new RuntimeException("Numeric overflow or underflow");
        return v;
    }

    private double[] validate(double[] v)
    {
        for (int k = 0; k < v.length; k++)
            validate(v[k]);
        return v;
    }

    private Matrix validate(Matrix v)
    {
        validate(v.getRowPackedCopy());
        return v;
    }
}
