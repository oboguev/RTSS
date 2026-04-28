package rtss.math.algorithms.pclm;

import Jama.Matrix;
import rtss.data.bin.Bin;
import rtss.data.curves.CurveUtil;
import rtss.util.Util;

/**
 * Penalized Composite Link Model (PCLM)
 *
 * Implementation based on:
 * S. Rizzi, J. Gampe, and P. Eilers, "Efficient Estimation of Smooth
 * Distributions From Coarsely Grouped Data", American Journal of Epidemiology,
 * Vol. 182, No. 2, 2015, pp. 138-147.
 *
 * This implementation uses the iteratively reweighted least squares (IRLS)
 * algorithm as described in the paper to disaggregate binned data into
 * smooth fine-grained estimates.
 *
 * The method assumes:
 * - Grouped counts follow Poisson distribution: Y_i ~ Poisson(μ_i)
 * - Expected counts: μ = C × γ, where γ = exp(X × β)
 * - Smoothness penalty on second-order differences of β
 * - Optimization via penalized log-likelihood maximization
 */
public class PCLM
{
    private final Bin[] bins;
    private final double lambda;
    private final int ppy;

    private final int nBins; // Number of input bins (I in the paper)
    private final int nPoints; // Number of output points (J in the paper)

    /**
     * Creates a new PCLM instance.
     *
     * @param bins Array of consecutive age bins with fields:
     *             age_x1 - starting age (inclusive)
     *             age_x2 - ending age (inclusive)
     *             avg - average value in this bin
     * @param lambda Smoothing parameter (larger = smoother result)
     * @param ppy Points per year (typically 1, can be 4 for quarterly, etc.)
     */
    public PCLM(Bin[] bins, double lambda, int ppy)
    {
        if (bins == null || bins.length == 0)
            throw new IllegalArgumentException("bins array cannot be null or empty");

        if (lambda < 0)
            throw new IllegalArgumentException("lambda must be non-negative");

        if (ppy < 1)
            throw new IllegalArgumentException("ppy must be at least 1");

        this.bins = bins;
        this.lambda = lambda;
        this.ppy = ppy;
        this.nBins = bins.length;

        // Calculate number of output points
        int firstAge = bins[0].age_x1;
        int lastAge = bins[nBins - 1].age_x2;
        this.nPoints = ppy * (lastAge - firstAge + 1);
    }

    /**
     * Executes the PCLM algorithm to disaggregate the binned data.
     *
     * @return Array of disaggregated values of length ppy * (age_range)
     * @throws Exception if the algorithm fails to converge or encounters numerical issues
     */
    public double[] pclm() throws Exception
    {
        // Build the composition matrix C (nBins × nPoints)
        Matrix C = buildCompositionMatrix();

        // Build the identity matrix X (nPoints × nPoints) - or could be B-spline basis
        Matrix X = Matrix.identity(nPoints, nPoints);

        // Build the penalty difference matrix D ((nPoints - 2) × nPoints)
        Matrix D = buildDifferenceMatrix(nPoints, 2);

        // Extract observed counts from bins
        double[] y = new double[nBins];
        for (int i = 0; i < nBins; i++)
        {
            y[i] = bins[i].avg * bins[i].widths_in_years * ppy;
        }

        // Perform IRLS iterations to estimate β
        double[] beta = performIRLS(y, C, X, D);

        // Compute γ = exp(X × β)
        Matrix betaMatrix = new Matrix(beta, nPoints);
        Matrix eta = X.times(betaMatrix);
        double[] gamma = new double[nPoints];
        for (int j = 0; j < nPoints; j++)
        {
            gamma[j] = Math.exp(eta.get(j, 0));
        }

        CurveUtil.avoidDecompositionRounding(gamma, bins);

        return gamma;
    }

    /**
     * Builds the composition matrix C that maps fine-grained points to coarse bins.
     * C is an (nBins × nPoints) matrix where C[i,j] indicates how much point j
     * contributes to bin i.
     */
    private Matrix buildCompositionMatrix()
    {
        Matrix C = new Matrix(nBins, nPoints);

        int firstAge = bins[0].age_x1;

        for (int i = 0; i < nBins; i++)
        {
            Bin bin = bins[i];

            // Calculate which points belong to this bin
            int pointStart = (bin.age_x1 - firstAge) * ppy;
            int pointEnd = (bin.age_x2 - firstAge + 1) * ppy - 1;

            // Set C[i, j] = 1 for all points j that belong to bin i
            for (int j = pointStart; j <= pointEnd && j < nPoints; j++)
            {
                C.set(i, j, 1.0);
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
     * This implements the algorithm from Appendix 1 of the paper.
     *
     * @param y Observed bin counts
     * @param C Composition matrix
     * @param X Basis matrix (identity or B-spline)
     * @param D Difference matrix for penalty
     * @return Estimated coefficients β
     * @throws Exception if convergence fails
     */
    private double[] performIRLS(double[] y, Matrix C, Matrix X, Matrix D) throws Exception
    {
        int nx = X.getColumnDimension();
        double la2 = Math.sqrt(lambda);

        // Initialize β with log of average count per point
        double totalCount = 0.0;
        for (double yi : y)
        {
            totalCount += yi;
        }
        double bstart = Math.log(totalCount / nx);
        double[] b = new double[nx];
        for (int j = 0; j < nx; j++)
        {
            b[j] = bstart;
        }

        // IRLS iterations
        final int MAX_ITERATIONS = 50;
        final double CONVERGENCE_THRESHOLD = 1e-6;

        for (int it = 0; it < MAX_ITERATIONS; it++)
        {
            double[] b0 = b.clone();

            // Compute η = X × β
            Matrix betaMatrix = new Matrix(b, nx);
            Matrix eta = X.times(betaMatrix);

            // Compute γ = exp(η)
            double[] gam = new double[nx];
            for (int j = 0; j < nx; j++)
            {
                gam[j] = Math.exp(eta.get(j, 0));
            }

            // Compute μ = C × γ
            Matrix gamMatrix = new Matrix(gam, nx);
            Matrix mu = C.times(gamMatrix);
            double[] muArray = mu.getColumnPackedCopy();

            // Build weights: w = [1/μ_1, ..., 1/μ_I, la2, ..., la2]
            double[] w = new double[nBins + nx - 2];
            for (int i = 0; i < nBins; i++)
            {
                w[i] = 1.0 / muArray[i];
            }
            for (int i = nBins; i < w.length; i++)
            {
                w[i] = la2;
            }

            // Compute Γ (Gamma matrix) = diag(γ)
            Matrix Gamma = new Matrix(nx, nx);
            for (int j = 0; j < nx; j++)
            {
                Gamma.set(j, j, gam[j]);
            }

            // Compute Q = C × Γ × X
            Matrix Q = C.times(Gamma).times(X);

            // Build z = [y - μ + Q×β, 0, ..., 0]
            Matrix Qb = Q.times(betaMatrix);
            double[] z = new double[nBins + nx - 2];
            for (int i = 0; i < nBins; i++)
            {
                z[i] = y[i] - muArray[i] + Qb.get(i, 0);
            }
            // Remaining elements are already 0

            // Stack matrices: [Q; D] and solve weighted least squares
            // (Q^T W Q + λ D^T D) β = Q^T W z
            Matrix A = stackMatrices(Q, D);
            Matrix zMatrix = new Matrix(z, z.length);

            // Weighted least squares: A^T W A β = A^T W z
            b = solveWeightedLeastSquares(A, zMatrix, w);

            // Check convergence
            double db = 0.0;
            for (int j = 0; j < nx; j++)
            {
                db = Math.max(db, Math.abs(b[j] - b0[j]));
            }

            if (db < CONVERGENCE_THRESHOLD)
            {
                return b;
            }
        }

        throw new Exception("PCLM algorithm failed to converge after " + MAX_ITERATIONS + " iterations");
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

        return betaMatrix.getColumnPackedCopy();
    }
}
