PCLM Implementation - rtss.math.algorithms.pclm2
================================================

Overview
--------
This package contains a faithful Java implementation of the Penalized Composite Link Model (PCLM)
algorithm as described in:

    S. Rizzi, J. Gampe, and P. Eilers, "Efficient Estimation of Smooth Distributions From
    Coarsely Grouped Data", American Journal of Epidemiology, Vol. 182, No. 2, 2015, pp. 138-147.

The PCLM algorithm disaggregates binned/grouped data into smooth fine-grained estimates while
preserving the constraint that the disaggregated values sum to the original grouped totals.

Classes
-------
- PCLM.java: Main implementation of the PCLM algorithm
- PCLMTest.java: Demonstration of various use cases
- PCLMConservationTest.java: Verification of the conservation property

Usage
-----
```java
import rtss.data.bin.Bin;
import rtss.math.algorithms.pclm2.PCLM;

// Create bins representing grouped data
Bin[] bins = {
    new Bin(0, 4, 50.0),    // age 0-4: avg value 50
    new Bin(5, 9, 20.0),    // age 5-9: avg value 20
    new Bin(10, 14, 15.0)   // age 10-14: avg value 15
};

// Parameters
double lambda = 1.0;  // Smoothing parameter (larger = smoother)
int ppy = 1;          // Points per year (1 for annual, 4 for quarterly, etc.)

// Execute PCLM
PCLM pclm = new PCLM(bins, lambda, ppy);
double[] result = pclm.pclm();

// result contains disaggregated values for each point
```

Parameters
----------
- bins: Array of Bin objects with fields:
    - age_x1: Starting age (inclusive)
    - age_x2: Ending age (inclusive)
    - avg: Average value in this age range

- lambda: Smoothing parameter
    - lambda = 0: No smoothing (unstable for small sample sizes)
    - lambda = 1-10: Light to moderate smoothing
    - lambda = 10-100: Strong smoothing
    - Recommended: Choose lambda by minimizing AIC (not yet implemented in this version)

- ppy: Points per year
    - ppy = 1: Annual disaggregation (most common)
    - ppy = 4: Quarterly disaggregation
    - ppy = 12: Monthly disaggregation

Algorithm Details
-----------------
The PCLM uses an Iteratively Reweighted Least Squares (IRLS) approach to estimate the
disaggregated distribution γ by:

1. Model: E(Y_i) = μ_i = Σ_j C_ij * γ_j, where γ_j = exp(Σ_k X_jk * β_k)

2. Composition matrix C maps fine-grained points to coarse bins:
   C_ij = 1 if point j belongs to bin i, 0 otherwise

3. Penalty on second-order differences of β coefficients:
   P = λ * Σ(Δ²β_k)²

4. Optimization via penalized Poisson log-likelihood maximization

5. Convergence when max|Δβ| < 10⁻⁶ or after 50 iterations

Properties
----------
1. Conservation: The sum of disaggregated values within each bin equals the original bin total
   (verified to machine precision)

2. Smoothness: The penalty term ensures smooth transitions between adjacent values

3. Flexibility: Handles irregular bin widths, open-ended intervals, and various data types

Comparison with Original Implementation
----------------------------------------
This Java implementation is faithful to the R reference code in the paper (Appendix 2).

Key correspondences:
- C matrix: Composition matrix (same structure)
- X matrix: Identity matrix (or could be B-spline basis)
- D matrix: Second-order difference matrix
- lambda: Smoothing parameter (identical)
- IRLS iteration: Same algorithm and convergence criterion

Differences from previous implementation (rtss.math.algorithms.pclm):
- Uses IRLS (correct) instead of Nelder-Mead simplex (incorrect)
- Uses Poisson log-likelihood (correct) instead of squared residuals (incorrect)
- Uses exponential link function (correct) instead of linear (incorrect)
- Correctly interprets composition matrix structure

Testing
-------
Run tests to verify correctness:

```bash
mvn exec:java -Dexec.mainClass="rtss.math.algorithms.pclm2.PCLMTest"
mvn exec:java -Dexec.mainClass="rtss.math.algorithms.pclm2.PCLMConservationTest"
```

The conservation test verifies that:
- Disaggregated values sum to original totals (within numerical precision)
- Conservation holds for different lambda values
- The algorithm produces smooth, plausible results

References
----------
[1] Rizzi, S., Gampe, J., and Eilers, P. (2015). "Efficient Estimation of Smooth
    Distributions From Coarsely Grouped Data". American Journal of Epidemiology,
    182(2):138-147.

[2] R implementation: See rtss-math-tools/src/main/resources/r-scripts/PCLM_Rizzi_2015.r

Dependencies
------------
- JAMA 1.0.3: Matrix operations
- rtss-runtime: Bin class definition

Author
------
Implemented following the Rizzi et al. (2015) specification
Date: 2026-04-25
