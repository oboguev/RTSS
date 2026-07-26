package rtss.pre1917.eval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fills missing annual numbers of births and deaths by interpolating annual
 * demographic rates and reconciling them iteratively with the population
 * balance equation.
 *
 * <p>The beginning-of-year population in 1896 is treated as the fixed anchor:
 *
 * <pre>
 * P(y + 1) = P(y) + B(y) - D(y) + M(y)
 * E(y)     = (P(y) + P(y + 1)) / 2
 * b(y)     = B(y) / E(y)
 * d(y)     = D(y) / E(y)
 * </pre>
 *
 * <p>Known observations are never changed. Missing internal values are based
 * on log-linear interpolation of rates. Missing values outside the observed
 * range use a constant robust edge rate: the median of up to five nearest
 * observed annual rates.
 *
 * <p>The deliberately misspelled method names {@code birhts} and
 * {@code setBirhts} are preserved to match the requested integration API.
 */
public abstract class FillBlanksMethod
{
    private static final int POPULATION_ANCHOR_YEAR = 1896;

    private static final int DEFAULT_EDGE_WINDOW = 5;
    private static final int DEFAULT_ANCHOR_SEARCH_RADIUS = 250;
    private static final int DEFAULT_MAX_ITERATIONS = 10_000;
    private static final double DEFAULT_DAMPING = 0.60;
    private static final double DEFAULT_RELATIVE_TOLERANCE = 1.0e-10;
    private static final int MAX_SUPPORTED_SPAN = 1_000;

    /*
     * Values written by this instance remain model estimates, not new source
     * observations. This lets a later call revise them instead of treating
     * them as fixed interpolation anchors.
     */
    private final Set<Integer> imputedBirthYears = new HashSet<>();
    private final Set<Integer> imputedDeathYears = new HashSet<>();

    /** Returns the observed number of deaths, or null if it is missing. */
    protected abstract Long deaths(int year);

    /** Returns the observed number of births, or null if it is missing. */
    protected abstract Long birhts(int year);

    /** Returns net migration during the year. */
    protected abstract long migration(int year);

    /** Returns population at the beginning of 1896. */
    protected abstract long population1896();

    /** Stores an imputed number of births. */
    protected abstract void setBirhts(int year, long births);

    /** Stores an imputed number of deaths. */
    protected abstract void setDeaths(int year, long deaths);

    /** Fills missing births and/or deaths for one year. */
    public final void fillBlanks(int year)
    {
        fillBlanks(year, year);
    }

    /**
     * Fills missing births and/or deaths in the inclusive interval.
     * Existing observed values are preserved.
     */
    public final void fillBlanks(int fromYear, int toYear)
    {
        fillRequestedInterval(fromYear, toYear);
    }

    /**
     * Detects every missing birth/death value in the inclusive interval and
     * fills all of them in one joint solution.
     */
    public final void fillAllBlanks(int fromYear, int toYear)
    {
        fillRequestedInterval(fromYear, toYear);
    }

    /** Number of observed edge years used for robust extrapolation. */
    protected int edgeWindow()
    {
        return DEFAULT_EDGE_WINDOW;
    }

    /** Maximum number of years searched for observations outside the request. */
    protected int anchorSearchRadius()
    {
        return DEFAULT_ANCHOR_SEARCH_RADIUS;
    }

    /** Maximum fixed-point iterations. */
    protected int maxIterations()
    {
        return DEFAULT_MAX_ITERATIONS;
    }

    /** Damping applied to every fixed-point update; must be in (0, 1]. */
    protected double damping()
    {
        return DEFAULT_DAMPING;
    }

    /** Relative convergence tolerance. */
    protected double relativeTolerance()
    {
        return DEFAULT_RELATIVE_TOLERANCE;
    }

    /** Hook for non-fatal diagnostics. */
    protected void warning(String message)
    {
        System.err.println("FillBlanks: " + message);
    }

    private void fillRequestedInterval(int fromYear, int toYear)
    {
        validateRequestedInterval(fromYear, toYear);

        boolean hasMissingValue = false;

        for (int year = fromYear; year <= toYear; year++)
        {
            if (observedBirths(year) == null || observedDeaths(year) == null)
            {
                hasMissingValue = true;
                break;
            }
        }

        if (!hasMissingValue)
            return;

        Span span = chooseCalculationSpan(fromYear, toYear);
        Solution solution = solve(span);
        commitRequestedAndPreviouslyImputed(fromYear, toYear, solution);
        reportDiagnostics(solution);
    }

    private void validateRequestedInterval(int fromYear, int toYear)
    {
        if (fromYear > toYear)
            throw new IllegalArgumentException("fromYear must not be greater than toYear: " + fromYear + " > " + toYear);

        if ((long) toYear - fromYear + 1L > MAX_SUPPORTED_SPAN)
            throw new IllegalArgumentException("Requested interval is too large: " + fromYear + ".." + toYear);

        if (population1896() <= 0L)
            throw new IllegalStateException("population1896() must return a positive population");

        if (edgeWindow() < 1)
            throw new IllegalStateException("edgeWindow() must be at least 1");

        if (anchorSearchRadius() < 0)
            throw new IllegalStateException("anchorSearchRadius() must not be negative");

        if (maxIterations() < 1)
            throw new IllegalStateException("maxIterations() must be at least 1");

        if (!(damping() > 0.0 && damping() <= 1.0))
            throw new IllegalStateException("damping() must be in (0, 1]");

        if (!(relativeTolerance() > 0.0))
            throw new IllegalStateException("relativeTolerance() must be positive");
    }

    private Span chooseCalculationSpan(int fromYear, int toYear)
    {
        int baseStart = Math.min(fromYear, POPULATION_ANCHOR_YEAR);
        int baseEnd = Math.max(toYear, POPULATION_ANCHOR_YEAR);

        int start = baseStart;
        int end = baseEnd;

        for (Series series : Series.values())
        {
            Integer earlier = findNthObservedBefore(series, baseStart, edgeWindow(), anchorSearchRadius());
            Integer later = findNthObservedAfter(series, baseEnd, edgeWindow(), anchorSearchRadius());

            if (earlier != null)
                start = Math.min(start, earlier);

            if (later != null)
                end = Math.max(end, later);
        }

        long length = (long) end - start + 1L;

        if (length > MAX_SUPPORTED_SPAN)
            throw new IllegalStateException("Calculation span became too large: " + start + ".." + end);

        return new Span(start, end);
    }

    private Integer findNthObservedBefore(
            Series series, int beforeYear, int wantedCount, int searchRadius)
    {
        int found = 0;

        for (int distance = 1; distance <= searchRadius; distance++)
        {
            int year = beforeYear - distance;
            if (observedValue(series, year) != null)
            {
                found++;
                if (found == wantedCount)
                    return year;
            }
        }

        // Even fewer than wantedCount observations are useful. Return the
        // farthest one that exists inside the search radius.
        Integer farthest = null;
        for (int distance = 1; distance <= searchRadius; distance++)
        {
            int year = beforeYear - distance;
            if (observedValue(series, year) != null)
                farthest = year;
        }

        return farthest;
    }

    private Integer findNthObservedAfter(
            Series series, int afterYear, int wantedCount, int searchRadius)
    {
        int found = 0;
        for (int distance = 1; distance <= searchRadius; distance++)
        {
            int year = afterYear + distance;

            if (observedValue(series, year) != null)
            {
                found++;
                if (found == wantedCount)
                    return year;
            }
        }

        Integer farthest = null;
        for (int distance = 1; distance <= searchRadius; distance++)
        {
            int year = afterYear + distance;
            if (observedValue(series, year) != null)
                farthest = year;
        }

        return farthest;
    }

    private Solution solve(Span span)
    {
        int n = span.length();
        long[] observedBirthValues = new long[n];
        long[] observedDeathValues = new long[n];
        boolean[] observedBirth = new boolean[n];
        boolean[] observedDeath = new boolean[n];
        double[] births = new double[n];
        double[] deaths = new double[n];
        long[] migrations = new long[n];

        for (int i = 0; i < n; i++)
        {
            int year = span.start + i;
            Long b = observedBirths(year);
            Long d = observedDeaths(year);

            if (b != null)
            {
                requireNonNegative("births", year, b);
                observedBirth[i] = true;
                observedBirthValues[i] = b;
                births[i] = b.doubleValue();
            }

            if (d != null)
            {
                requireNonNegative("deaths", year, d);
                observedDeath[i] = true;
                observedDeathValues[i] = d;
                deaths[i] = d.doubleValue();
            }

            migrations[i] = migration(year);
        }

        ensureAtLeastOneObservation("births", observedBirth, span);
        ensureAtLeastOneObservation("deaths", observedDeath, span);

        initialiseMissingCounts(births, observedBirth, span);
        initialiseMissingCounts(deaths, observedDeath, span);

        double[] population = new double[n + 1];
        double[] exposure = new double[n];
        double[] birthRates = new double[n];
        double[] deathRates = new double[n];
        double[] nextBirths = new double[n];
        double[] nextDeaths = new double[n];

        boolean converged = false;
        int iterationsUsed = 0;

        for (int iteration = 1; iteration <= maxIterations(); iteration++)
        {
            iterationsUsed = iteration;
            reconstructPopulation(span, births, deaths, migrations, population, exposure);

            calculateObservedRates(births, observedBirth, exposure, birthRates, "births", span);
            calculateObservedRates(deaths, observedDeath, exposure, deathRates, "deaths", span);

            double maxRelativeChange = 0.0;

            for (int i = 0; i < n; i++)
            {
                if (observedBirth[i])
                {
                    nextBirths[i] = births[i];
                }
                else
                {
                    double rate = estimateMissingRate(i, birthRates, observedBirth);
                    double candidate = requireFiniteNonNegative(
                                                                "estimated births", span.start + i, rate * exposure[i]);
                    nextBirths[i] = damped(births[i], candidate);
                    maxRelativeChange = Math.max(maxRelativeChange, relativeChange(births[i], nextBirths[i]));
                }

                if (observedDeath[i])
                {
                    nextDeaths[i] = deaths[i];
                }
                else
                {
                    double rate = estimateMissingRate(i, deathRates, observedDeath);
                    double candidate = requireFiniteNonNegative(
                                                                "estimated deaths", span.start + i, rate * exposure[i]);
                    nextDeaths[i] = damped(deaths[i], candidate);
                    maxRelativeChange = Math.max(maxRelativeChange, relativeChange(deaths[i], nextDeaths[i]));
                }
            }

            copy(nextBirths, births);
            copy(nextDeaths, deaths);

            if (maxRelativeChange < relativeTolerance())
            {
                converged = true;
                break;
            }
        }

        if (!converged)
        {
            throw new IllegalStateException("Interpolation did not converge after " + maxIterations()
                                            + " iterations for " + span.start + ".." + span.end);
        }

        reconstructPopulation(span, births, deaths, migrations, population, exposure);
        calculateObservedRates(births, observedBirth, exposure, birthRates, "births", span);
        calculateObservedRates(deaths, observedDeath, exposure, deathRates, "deaths", span);

        // Store the final interpolated/extrapolated rates too, for diagnostics.
        for (int i = 0; i < n; i++)
        {
            if (!observedBirth[i])
                birthRates[i] = estimateMissingRate(i, birthRates, observedBirth);

            if (!observedDeath[i])
                deathRates[i] = estimateMissingRate(i, deathRates, observedDeath);
        }

        return new Solution(span,
                            births,
                            deaths,
                            population,
                            exposure,
                            birthRates,
                            deathRates,
                            observedBirth,
                            observedDeath,
                            iterationsUsed);
    }

    private void initialiseMissingCounts(
            double[] values, boolean[] observed, Span span)
    {
        for (int i = 0; i < values.length; i++)
        {
            if (!observed[i])
            {
                values[i] = estimateInitialCount(i, values, observed);
                requireFiniteNonNegative("initial estimate", span.start + i, values[i]);
            }
        }
    }

    private double estimateInitialCount(
            int index, double[] observedValues, boolean[] observed)
    {
        int previous = previousObservedIndex(index, observed);
        int next = nextObservedIndex(index, observed);

        if (previous >= 0 && next >= 0)
        {
            double fraction = (double) (index - previous) / (next - previous);
            return interpolateNonNegative(observedValues[previous], observedValues[next], fraction);
        }

        if (previous >= 0)
            return medianNearestObserved(index, observedValues, observed, -1, edgeWindow());

        if (next >= 0)
            return medianNearestObserved(index, observedValues, observed, +1, edgeWindow());

        throw new IllegalStateException("No observed values available for initialization");
    }

    private void reconstructPopulation(
            Span span,
            double[] births,
            double[] deaths,
            long[] migrations,
            double[] population,
            double[] exposure)
    {
        Arrays.fill(population, Double.NaN);
        int anchorIndex = POPULATION_ANCHOR_YEAR - span.start;
        population[anchorIndex] = population1896();

        // Move backward from the beginning of 1896.
        for (int year = POPULATION_ANCHOR_YEAR - 1; year >= span.start; year--)
        {
            int i = year - span.start;
            population[i] = population[i + 1] - births[i] + deaths[i] - migrations[i];
            requirePositivePopulation(year, population[i]);
        }

        // Move forward from the beginning of 1896.
        for (int year = POPULATION_ANCHOR_YEAR; year <= span.end; year++)
        {
            int i = year - span.start;
            population[i + 1] = population[i]
                                + births[i] - deaths[i] + migrations[i];
            requirePositivePopulation(year + 1, population[i + 1]);
        }

        for (int i = 0; i < exposure.length; i++)
        {
            exposure[i] = 0.5 * (population[i] + population[i + 1]);
            if (!(exposure[i] > 0.0) || !Double.isFinite(exposure[i]))
            {
                throw new IllegalStateException("Non-positive or non-finite exposure for year "
                                                + (span.start + i) + ": " + exposure[i]);
            }
        }
    }

    private void calculateObservedRates(
            double[] counts,
            boolean[] observed,
            double[] exposure,
            double[] rates,
            String label,
            Span span)
    {
        Arrays.fill(rates, Double.NaN);

        for (int i = 0; i < counts.length; i++)
        {
            if (observed[i])
            {
                rates[i] = counts[i] / exposure[i];
                requireFiniteNonNegative(label + " rate", span.start + i, rates[i]);
            }
        }
    }

    private double estimateMissingRate(
            int index, double[] observedRates, boolean[] observed)
    {
        int previous = previousObservedIndex(index, observed);
        int next = nextObservedIndex(index, observed);

        if (previous >= 0 && next >= 0)
        {
            double fraction = (double) (index - previous) / (next - previous);
            return interpolateNonNegative(observedRates[previous], observedRates[next], fraction);
        }

        if (previous >= 0)
            return medianNearestObserved(index, observedRates, observed, -1, edgeWindow());

        if (next >= 0)
            return medianNearestObserved(index, observedRates, observed, +1, edgeWindow());

        throw new IllegalStateException("No observed rates available for interpolation");
    }

    /**
     * Uses geometric interpolation when both endpoints are positive; otherwise
     * falls back to ordinary linear interpolation, which also supports zeros.
     */
    private static double interpolateNonNegative(
            double left, double right, double fraction)
    {
        if (left > 0.0 && right > 0.0)
            return Math.exp(Math.log(left) + fraction * (Math.log(right) - Math.log(left)));

        return Math.max(0.0, left + fraction * (right - left));
    }

    private static int previousObservedIndex(int index, boolean[] observed)
    {
        for (int i = index - 1; i >= 0; i--)
        {
            if (observed[i])
                return i;
        }

        return -1;
    }

    private static int nextObservedIndex(int index, boolean[] observed)
    {
        for (int i = index + 1; i < observed.length; i++)
        {
            if (observed[i])
                return i;
        }

        return -1;
    }

    private static double medianNearestObserved(
            int index,
            double[] values,
            boolean[] observed,
            int direction,
            int maximumCount)
    {
        List<Double> selected = new ArrayList<>(maximumCount);
        for (int i = index + direction; i >= 0 && i < values.length && selected.size() < maximumCount; i += direction)
        {
            if (observed[i])
            {
                double value = values[i];

                if (!Double.isFinite(value) || value < 0.0)
                    throw new IllegalStateException("Invalid observed value used for edge estimation: " + value);

                selected.add(value);
            }
        }
        if (selected.isEmpty())
            throw new IllegalStateException("No edge observations available");

        Collections.sort(selected);
        int middle = selected.size() / 2;
        if ((selected.size() & 1) == 1)
            return selected.get(middle);

        return 0.5 * (selected.get(middle - 1) + selected.get(middle));
    }

    private double damped(double oldValue, double candidate)
    {
        return oldValue + damping() * (candidate - oldValue);
    }

    private static double relativeChange(double oldValue, double newValue)
    {
        return Math.abs(newValue - oldValue) / Math.max(1.0, Math.abs(oldValue));
    }

    private static void copy(double[] source, double[] destination)
    {
        System.arraycopy(source, 0, destination, 0, source.length);
    }

    private void commitRequestedAndPreviouslyImputed(
            int fromYear, int toYear, Solution solution)
    {
        for (int year = solution.span.start; year <= solution.span.end; year++)
        {
            int i = year - solution.span.start;
            boolean inRequestedInterval = year >= fromYear && year <= toYear;

            boolean writeBirths = imputedBirthYears.contains(year) || (inRequestedInterval && observedBirths(year) == null);
            boolean writeDeaths = imputedDeathYears.contains(year) || (inRequestedInterval && observedDeaths(year) == null);

            if (writeBirths)
            {
                long rounded = roundCount("births", year, solution.births[i]);
                setBirhts(year, rounded);
                imputedBirthYears.add(year);
            }

            if (writeDeaths)
            {
                long rounded = roundCount("deaths", year, solution.deaths[i]);
                setDeaths(year, rounded);
                imputedDeathYears.add(year);
            }
        }
    }

    private void reportDiagnostics(Solution solution)
    {
        for (int i = 0; i < solution.span.length(); i++)
        {
            int year = solution.span.start + i;
            double birthRatePerThousand = 1000.0 * solution.birthRates[i];
            double deathRatePerThousand = 1000.0 * solution.deathRates[i];

            if (!solution.observedBirth[i]
                && (birthRatePerThousand < 1.0 || birthRatePerThousand > 100.0))
            {
                warning("unusual imputed birth rate in " + year + ": "
                        + birthRatePerThousand + " per 1,000");
            }

            if (!solution.observedDeath[i]
                && (deathRatePerThousand < 1.0 || deathRatePerThousand > 150.0))
            {
                warning("unusual imputed death rate in " + year + ": "
                        + deathRatePerThousand + " per 1,000");
            }
        }
    }

    private Long observedBirths(int year)
    {
        return imputedBirthYears.contains(year) ? null : birhts(year);
    }

    private Long observedDeaths(int year)
    {
        return imputedDeathYears.contains(year) ? null : deaths(year);
    }

    private Long observedValue(Series series, int year)
    {
        return series == Series.BIRTHS ? observedBirths(year) : observedDeaths(year);
    }

    private static void ensureAtLeastOneObservation(
            String label, boolean[] observed, Span span)
    {
        for (boolean value : observed)
        {
            if (value)
                return;
        }

        throw new IllegalStateException("No observed " + label + " in calculation span "
                                        + span.start + ".." + span.end);
    }

    private static void requireNonNegative(String label, int year, long value)
    {
        if (value < 0L)
            throw new IllegalStateException("Negative " + label + " in " + year + ": " + value);
    }

    private static double requireFiniteNonNegative(
            String label, int year, double value)
    {
        if (!Double.isFinite(value) || value < 0.0)
            throw new IllegalStateException("Invalid " + label + " in " + year + ": " + value);

        return value;
    }

    private static void requirePositivePopulation(int year, double value)
    {
        if (!Double.isFinite(value) || value <= 0.0)
        {
            throw new IllegalStateException("Reconstructed beginning-of-year population is invalid in "
                                            + year + ": " + value
                                            + ". Check population1896(), migration, and source data.");
        }
    }

    private static long roundCount(String label, int year, double value)
    {
        requireFiniteNonNegative(label, year, value);
        if (value > Long.MAX_VALUE)
        {
            throw new IllegalStateException("Estimated " + label + " exceeds long range in " + year);
        }
        return Math.round(value);
    }

    private enum Series
    {
        BIRTHS, DEATHS
    }

    private static final class Span
    {
        final int start;
        final int end;

        Span(int start, int end)
        {
            this.start = start;
            this.end = end;
        }

        int length()
        {
            return end - start + 1;
        }
    }

    private static final class Solution
    {
        final Span span;
        final double[] births;
        final double[] deaths;
        final double[] population;
        final double[] exposure;
        final double[] birthRates;
        final double[] deathRates;
        final boolean[] observedBirth;
        final boolean[] observedDeath;
        final int iterationsUsed;

        Solution(
                Span span,
                double[] births,
                double[] deaths,
                double[] population,
                double[] exposure,
                double[] birthRates,
                double[] deathRates,
                boolean[] observedBirth,
                boolean[] observedDeath,
                int iterationsUsed)
        {
            this.span = span;
            this.births = births;
            this.deaths = deaths;
            this.population = population;
            this.exposure = exposure;
            this.birthRates = birthRates;
            this.deathRates = deathRates;
            this.observedBirth = observedBirth;
            this.observedDeath = observedDeath;
            this.iterationsUsed = iterationsUsed;
        }
    }
}
