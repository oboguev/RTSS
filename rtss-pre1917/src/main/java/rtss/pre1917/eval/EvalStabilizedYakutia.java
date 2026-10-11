package rtss.pre1917.eval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.util.Util;

/**
 * Reconstructs Yakutia births and deaths in 1881-1885 using the algorithm of
 * EvalStabilizedV2, with fixed reconstruction and reference intervals.
 *
 * <p>The target birth rate is the population-weighted mean for 1886-1911.
 * The target death rate uses the same interval but excludes 1889 from both
 * the event sum and the population sum because of the smallpox epidemic.</p>
 *
 * <p>Only births and deaths in 1881-1885 are reconstructed. All other event
 * counts, including those in 1889, 1891 and 1912-1914, remain as supplied.
 * Progressive population is recalculated using the 1897 census after each
 * iteration.</p>
 *
 * <p>For each series, the original rates supply relative deviations around
 * a centered moving-median trend. A population-weighted normalization over
 * 1881-1885 makes the reconstructed mean rate equal to the reference rate,
 * subject to integer rounding. The trend and residuals are recomputed from
 * the original counts and the current population at every iteration.</p>
 *
 * <p>Known gaps and other defective observations must be repaired before
 * calling this class. In particular, it does not fill the missing source
 * observations for 1882. The input territory is never modified.</p>
 *
 * <p>lambda = 0 gives a flat rate; lambda = 0.5 retains part of the residual
 * variation; lambda = 1 retains the relative residuals fully.</p>
 */
public class EvalStabilizedYakutia
{
    private static final int MAX_ITERATIONS = 200;
    private static final int FROM_YEAR = 1881;
    private static final int TO_YEAR = 1914;
    private static final int RECONSTRUCT_FROM_YEAR = 1881;
    private static final int RECONSTRUCT_TO_YEAR = 1885;
    private static final int REFERENCE_FROM_YEAR = 1886;
    private static final int REFERENCE_TO_YEAR = 1911;
    private static final int EXCLUDED_DEATH_YEAR = 1889;

    private final TerritoryDataSet tdsCensus1897;
    private final int windowWidth;
    private final double lambda;

    /** Uses a five-year median window and preserves relative residuals fully. */
    public EvalStabilizedYakutia(TerritoryDataSet tdsCensus1897)
    {
        this(tdsCensus1897, 5, 1.0);
    }

    public EvalStabilizedYakutia(TerritoryDataSet tdsCensus1897,
            int windowWidth,
            double lambda)
    {
        if (windowWidth < 3 || windowWidth % 2 == 0)
            throw constructorError("EvalStabilizedYakutia: windowWidth must be an odd integer >= 3");

        if (!Double.isFinite(lambda) || lambda < 0.0 || lambda > 1.0)
            throw constructorError("EvalStabilizedYakutia: lambda must be between 0 and 1");

        this.tdsCensus1897 = tdsCensus1897;
        this.windowWidth = windowWidth;
        this.lambda = lambda;
    }

    /**
     * @param t source Yakutia territory, including any previously accepted
     *          source patches and gap filling; it is treated as immutable
     */
    public Territory evalTerritory(Territory t) throws Exception
    {
        if (t == null)
            throw evaluationError("EvalStabilizedYakutia: territory is null");

        if (tdsCensus1897 == null)
            throw constructorError("EvalStabilizedYakutia: tdsCensus1897 is null");

        Territory censusTerritory = tdsCensus1897.get(t.name);
        if (censusTerritory == null)
            throw evaluationError("EvalStabilizedYakutia: no 1897 census territory for " + t.name);

        return evalTerritory(t, censusTerritory);
    }

    /** Allows the caller to supply the census territory directly. */
    public Territory evalTerritory(Territory t, Territory censusTerritory) throws Exception
    {
        if (t == null)
            throw evaluationError("EvalStabilizedYakutia: territory is null");

        if (censusTerritory == null)
            throw evaluationError("EvalStabilizedYakutia: census territory is null for " + t.name);

        Territory result = t.dup();
        List<Integer> years = new ArrayList<Integer>();
        for (int year : result.years())
        {
            if (year >= FROM_YEAR && year <= TO_YEAR)
                years.add(year);
        }
        Collections.sort(years);

        validateInterval(years, RECONSTRUCT_FROM_YEAR, RECONSTRUCT_TO_YEAR, "reconstruction", t.name);
        validateInterval(years, REFERENCE_FROM_YEAR, REFERENCE_TO_YEAR, "reference", t.name);
        validateTerritoryData(t, years);

        Map<Integer, Long> sourceBirths = sourceCounts(t, years, Series.BIRTHS);
        Map<Integer, Long> sourceDeaths = sourceCounts(t, years, Series.DEATHS);

        // Establish a population series consistent with the unmodified clone.
        EvalProgressive.evalProgressive(result, censusTerritory);

        long[][] twoIterationsAgo = null;

        for (int iteration = 1; ; iteration++)
        {
            long[][] before = snapshot(result, years);

            long[] reconstructedBirths = reconstruct(result, years, sourceBirths, Series.BIRTHS);
            long[] reconstructedDeaths = reconstruct(result, years, sourceDeaths, Series.DEATHS);

            apply(result, years, reconstructedBirths, Series.BIRTHS);
            apply(result, years, reconstructedDeaths, Series.DEATHS);

            EvalProgressive.evalProgressive(result, censusTerritory);

            long[][] after = snapshot(result, years);

            if (sameState(before, after))
                return result;

            /*
             * Integer counts may settle into adjacent states rather than an
             * exact fixed point. As in EvalStabilizedV2, accept two successive
             * transitions with no more than one event per series and year.
             */
            if (twoIterationsAgo != null &&
                differsOnlyByRounding(twoIterationsAgo, before) &&
                differsOnlyByRounding(before, after))
            {
                return result;
            }

            if (twoIterationsAgo != null && sameState(twoIterationsAgo, after))
                throw evaluationError("EvalStabilizedYakutia: entered a non-trivial two-state cycle for " + t.name);

            twoIterationsAgo = before;

            if (iteration == MAX_ITERATIONS)
                throw evaluationError("EvalStabilizedYakutia: failed to converge after " + MAX_ITERATIONS + " iterations for " + t.name);
        }
    }

    private long[] reconstruct(Territory current,
            List<Integer> years,
            Map<Integer, Long> source,
            Series series) throws Exception
    {
        int n = years.size();
        double[] population = new double[n];
        double[] sourceRatesForTrend = new double[n];

        for (int k = 0; k < n; k++)
        {
            int year = years.get(k);
            TerritoryYear ty = current.territoryYearOrNull(year);
            long p = requirePopulation(ty, year, current.name);
            population[k] = p;
            sourceRatesForTrend[k] = source.get(year) / (double) p;
        }

        double[] trend = centeredMovingMedian(sourceRatesForTrend);
        double referenceRate = weightedReferenceRate(years, source, population, series);

        double weightedFactorSum = 0.0;
        double weightSum = 0.0;

        // Normalize over the early block only, not every non-reference year.
        for (int k = 0; k < n; k++)
        {
            int year = years.get(k);
            if (!isReconstructionYear(year))
                continue;

            double factor = residualFactor(sourceRatesForTrend[k], trend[k], year, series);
            weightedFactorSum += population[k] * factor;
            weightSum += population[k];
        }

        double normalization = weightedFactorSum / weightSum;

        if (!Double.isFinite(normalization) || normalization <= 0.0)
            throw evaluationError("EvalStabilizedYakutia: invalid " + seriesName(series) + " residual normalization for " + current.name);

        long[] reconstructed = new long[n];

        for (int k = 0; k < n; k++)
        {
            int year = years.get(k);

            if (!isReconstructionYear(year))
            {
                reconstructed[k] = source.get(year);
                continue;
            }

            double factor = residualFactor(sourceRatesForTrend[k], trend[k], year, series);
            double reconstructedRate = referenceRate * factor / normalization;

            reconstructed[k] = roundedCount(reconstructedRate * population[k],
                                            year,
                                            seriesName(series),
                                            current.name);
        }

        return reconstructed;
    }

    private double residualFactor(double sourceRate,
            double trend,
            int year,
            Series series) throws Exception
    {
        if (!Double.isFinite(sourceRate) || sourceRate <= 0.0)
            throw evaluationError("EvalStabilizedYakutia: invalid source " + seriesName(series) + " rate in " + year);

        if (!Double.isFinite(trend) || trend <= 0.0)
            throw evaluationError("EvalStabilizedYakutia: invalid " + seriesName(series) + " trend in " + year);

        return Math.pow(sourceRate / trend, lambda);
    }

    private double[] centeredMovingMedian(double[] values)
    {
        int n = values.length;
        int halfWindow = windowWidth / 2;
        double[] trend = new double[n];

        for (int k = 0; k < n; k++)
        {
            int from = Math.max(0, k - halfWindow);
            int to = Math.min(n - 1, k + halfWindow);
            double[] window = Arrays.copyOfRange(values, from, to + 1);
            Arrays.sort(window);

            int middle = window.length / 2;
            if (window.length % 2 == 1)
                trend[k] = window[middle];
            else
                trend[k] = (window[middle - 1] + window[middle]) / 2.0;
        }

        return trend;
    }

    private static double weightedReferenceRate(List<Integer> years,
            Map<Integer, Long> source,
            double[] population,
            Series series) throws Exception
    {
        double countSum = 0.0;
        double populationSum = 0.0;

        for (int k = 0; k < years.size(); k++)
        {
            if (isReferenceYear(years.get(k), series))
            {
                countSum += source.get(years.get(k));
                populationSum += population[k];
            }
        }

        double rate = countSum / populationSum;
        if (!Double.isFinite(rate) || rate <= 0.0)
            throw evaluationError("EvalStabilizedYakutia: invalid reference " + seriesName(series) + " rate");

        return rate;
    }

    private static boolean isReferenceYear(int year, Series series)
    {
        return year >= REFERENCE_FROM_YEAR && year <= REFERENCE_TO_YEAR &&
               !(series == Series.DEATHS && year == EXCLUDED_DEATH_YEAR);
    }

    private static boolean isReconstructionYear(int year)
    {
        return year >= RECONSTRUCT_FROM_YEAR && year <= RECONSTRUCT_TO_YEAR;
    }

    private static Map<Integer, Long> sourceCounts(Territory sourceTerritory,
            List<Integer> years,
            Series series)
    {
        Map<Integer, Long> counts = new HashMap<Integer, Long>();

        for (int year : years)
        {
            TerritoryYear ty = sourceTerritory.territoryYearOrNull(year);
            counts.put(year, series == Series.BIRTHS ? ty.births.total.both
                                                     : ty.deaths.total.both);
        }

        return counts;
    }

    private static void apply(Territory territory,
            List<Integer> years,
            long[] values,
            Series series)
    {
        for (int k = 0; k < years.size(); k++)
        {
            int year = years.get(k);
            if (!isReconstructionYear(year))
                continue;

            TerritoryYear ty = territory.territoryYearOrNull(year);
            if (series == Series.BIRTHS)
                ty.births.total.both = values[k];
            else
                ty.deaths.total.both = values[k];
        }
    }

    private static long[][] snapshot(Territory territory,
            List<Integer> years) throws Exception
    {
        long[][] state = new long[years.size()][3];

        for (int k = 0; k < years.size(); k++)
        {
            int year = years.get(k);
            TerritoryYear ty = territory.territoryYearOrNull(year);
            state[k][0] = ty.births.total.both;
            state[k][1] = ty.deaths.total.both;
            state[k][2] = requirePopulation(ty, year, territory.name);
        }

        return state;
    }

    private static boolean sameState(long[][] a, long[][] b)
    {
        if (a.length != b.length)
            return false;

        for (int k = 0; k < a.length; k++)
        {
            if (!Arrays.equals(a[k], b[k]))
                return false;
        }

        return true;
    }

    private static boolean differsOnlyByRounding(long[][] a, long[][] b)
    {
        if (a.length != b.length)
            return false;

        for (int k = 0; k < a.length; k++)
        {
            // Population differences accumulate from the +/-1 event choices.
            if (Math.abs(a[k][0] - b[k][0]) > 1 ||
                Math.abs(a[k][1] - b[k][1]) > 1)
            {
                return false;
            }
        }

        return true;
    }

    private static void validateTerritoryData(Territory territory,
            List<Integer> years) throws Exception
    {
        for (int year : years)
        {
            TerritoryYear ty = territory.territoryYearOrNull(year);
            if (ty == null)
                throw evaluationError("EvalStabilizedYakutia: missing year " + year + " for " + territory.name);

            if (ty.births == null || ty.births.total == null ||
                ty.births.total.both == null ||
                ty.deaths == null || ty.deaths.total == null ||
                ty.deaths.total.both == null)
            {
                throw evaluationError("EvalStabilizedYakutia: incomplete data in " + year + " for " + territory.name);
            }

            if (ty.births.total.both <= 0 || ty.deaths.total.both <= 0)
                throw evaluationError("EvalStabilizedYakutia: non-positive births/deaths in " + year + " for " + territory.name);

            requirePopulation(ty, year, territory.name);
        }
    }

    private static void validateInterval(List<Integer> years,
            int year1,
            int year2,
            String intervalName,
            String territoryName) throws Exception
    {
        for (int year = year1; year <= year2; year++)
        {
            if (!years.contains(year))
            {
                throw evaluationError("EvalStabilizedYakutia: " + intervalName + " interval contains missing year " + year + " for "
                                      + territoryName);
            }
        }
    }

    private static long requirePopulation(TerritoryYear ty,
            int year,
            String territoryName) throws Exception
    {
        if (ty == null ||
            ty.progressive_population == null ||
            ty.progressive_population.total == null ||
            ty.progressive_population.total.both == null ||
            ty.progressive_population.total.both <= 0)
        {
            throw evaluationError("EvalStabilizedYakutia: invalid progressive population in " + year + " for " + territoryName);
        }

        return ty.progressive_population.total.both;
    }

    private static long roundedCount(double value,
            int year,
            String seriesName,
            String territoryName) throws Exception
    {
        if (!Double.isFinite(value) || value < 0.0 || value > Long.MAX_VALUE)
            throw evaluationError("EvalStabilizedYakutia: invalid reconstructed " + seriesName + " count in " + year + " for " + territoryName);

        return Math.round(value);
    }

    private static String seriesName(Series series)
    {
        return series == Series.BIRTHS ? "births" : "deaths";
    }

    private static IllegalArgumentException constructorError(String message)
    {
        Util.err(message);
        return new IllegalArgumentException(message);
    }

    private static Exception evaluationError(String message)
    {
        Util.err(message);
        return new Exception(message);
    }

    private enum Series
    {
        BIRTHS, DEATHS
    }
}
