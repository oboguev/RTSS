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
 * Reconstructs births and deaths outside reliable (stabilized) intervals.
 *
 * <p>The stabilized interval supplies the population-weighted mean rate. The
 * original series supplies relative year-to-year deviations around a robust
 * centered trend. {@code lambda} controls how much of those deviations is
 * retained: 0 gives a flat rate, 1 retains them fully.</p>
 *
 * <p>Known gaps, administrative breaks, and other defective source intervals
 * must be repaired before this class is used. The method never modifies its
 * input territory.</p>
 * 
 * λ = 0 — плоская реконструкция;
 * λ = 0.5 — частичное сохранение формы;
 * λ = 1 — полное сохранение остаточных колебаний;
 */
public class EvalStabilizedV2
{
    private static final int MAX_ITERATIONS = 200;
    private static final int fromYear = 1881;
    private static final int toYear = 1914;

    private final TerritoryDataSet tdsCensus1897;
    private final int windowWidth;
    private final double lambda;
    private final boolean adjust1892;

    public EvalStabilizedV2(TerritoryDataSet tdsCensus1897,
            int windowWidth,
            double lambda,
            boolean adjust1892)
    {
        if (tdsCensus1897 == null)
            throw constructorError("EvalStabilizedV2: tdsCensus1897 is null");

        if (windowWidth < 3 || windowWidth % 2 == 0)
            throw constructorError("EvalStabilizedV2: windowWidth must be an odd integer >= 3");

        if (!Double.isFinite(lambda) || lambda < 0.0 || lambda > 1.0)
            throw constructorError("EvalStabilizedV2: lambda must be between 0 and 1");

        this.tdsCensus1897 = tdsCensus1897;
        this.windowWidth = windowWidth;
        this.lambda = lambda;
        this.adjust1892 = adjust1892;
    }

    /**
     * @param t   source territory; it is treated as immutable
     * @param by1 first reliable year for births
     * @param by2 last reliable year for births
     * @param dy1 first reliable year for deaths
     * @param dy2 last reliable year for deaths
     */
    public Territory evalTerritory(Territory t,
            int by1,
            int by2,
            int dy1,
            int dy2) throws Exception
    {
        if (t == null)
            throw evaluationError("EvalStabilizedV2: territory is null");

        if (by1 > by2)
            throw evaluationError("EvalStabilizedV2: invalid births interval " + by1 + "-" + by2);

        if (dy1 > dy2)
            throw evaluationError("EvalStabilizedV2: invalid deaths interval " + dy1 + "-" + dy2);

        Territory censusTerritory = tdsCensus1897.get(t.name);
        if (censusTerritory == null)
            throw evaluationError("EvalStabilizedV2: no 1897 census territory for " + t.name);

        Territory result = t.dup();
        List<Integer> years = new ArrayList<Integer>();
        for (int year : result.years())
        {
            if (year >= fromYear && year <= toYear)
                years.add(year);
        }
        Collections.sort(years);

        if (years.isEmpty())
            throw evaluationError("EvalStabilizedV2: no data in " + fromYear + "-" + toYear + " for " + t.name);

        validateStableInterval(years, by1, by2, "births", t.name);
        validateStableInterval(years, dy1, dy2, "deaths", t.name);
        validateTerritoryData(t, years);

        Map<Integer, Long> sourceBirths = sourceCounts(t, years, Series.BIRTHS);
        Map<Integer, Long> sourceDeaths = sourceCounts(t, years, Series.DEATHS);

        Special1892 births1892 = Special1892.forBirths(sourceBirths);
        Special1892 deaths1892 = Special1892.forDeaths(sourceDeaths);

        // Establish a population series consistent with the unmodified clone.
        EvalProgressive.evalProgressive(result, censusTerritory);

        long[][] twoIterationsAgo = null;

        for (int iteration = 1; iteration <= MAX_ITERATIONS; iteration++)
        {
            long[][] before = snapshot(result, years);

            long[] reconstructedBirths = reconstruct(result, years, sourceBirths, by1, by2, births1892, "births");

            long[] reconstructedDeaths = reconstruct(result, years, sourceDeaths, dy1, dy2, deaths1892, "deaths");

            apply(result, years, reconstructedBirths, Series.BIRTHS);
            apply(result, years, reconstructedDeaths, Series.DEATHS);

            EvalProgressive.evalProgressive(result, censusTerritory);

            long[][] after = snapshot(result, years);

            if (sameState(before, after))
                return result;

            /*
             * With integer births/deaths an exact fixed point need not exist:
             * rounding can produce A -> B -> A, with A and B differing by one
             * event in a few years. Both states represent the same continuous
             * solution to the available integer precision.
             */
            if (twoIterationsAgo != null && sameState(twoIterationsAgo, after))
            {
                if (differsOnlyByRounding(before, after))
                    return result;

                throw evaluationError("EvalStabilizedV2: entered a non-trivial two-state cycle for " + t.name);
            }

            twoIterationsAgo = before;
        }

        throw evaluationError("EvalStabilizedV2: failed to converge after " + MAX_ITERATIONS + " iterations for " + t.name);
    }

    private long[] reconstruct(Territory current,
            List<Integer> years,
            Map<Integer, Long> source,
            int stableYear1,
            int stableYear2,
            Special1892 special1892,
            String seriesName) throws Exception
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

            double count = source.get(year);
            if (year == 1892 && special1892.active)
                count = special1892.replacementCount;

            sourceRatesForTrend[k] = count / p;
        }

        double[] trend = centeredMovingMedian(sourceRatesForTrend);
        double stableRate = weightedStableRate(years, source, population, stableYear1, stableYear2);

        /*
         * Normalize the ordinary residual factors over all reconstructed years.
         * The special 1892 multiplier is deliberately excluded: an epidemic
         * boost or a births dip must change the reconstructed total, rather than
         * being offset by lowering or raising all other years.
         */
        double weightedFactorSum = 0.0;
        double weightSum = 0.0;

        for (int k = 0; k < n; k++)
        {
            int year = years.get(k);
            if (inside(year, stableYear1, stableYear2))
                continue;

            double factor = residualFactor(sourceRatesForTrend[k], trend[k], year, seriesName);

            weightedFactorSum += population[k] * factor;
            weightSum += population[k];
        }

        double normalization = weightSum == 0.0 ? 1.0
                                                : weightedFactorSum / weightSum;

        if (!Double.isFinite(normalization) || normalization <= 0.0)
            throw evaluationError("EvalStabilizedV2: invalid " + seriesName + " residual normalization for " + current.name);

        long[] reconstructed = new long[n];

        for (int k = 0; k < n; k++)
        {
            int year = years.get(k);

            if (inside(year, stableYear1, stableYear2))
            {
                // Preserve the stabilized interval exactly as supplied.
                reconstructed[k] = source.get(year);
                continue;
            }

            double factor = residualFactor(sourceRatesForTrend[k], trend[k], year, seriesName);

            double reconstructedRate = stableRate * factor / normalization;

            if (year == 1892 && special1892.active)
                reconstructedRate *= special1892.multiplier;

            reconstructed[k] = roundedCount(reconstructedRate * population[k],
                                            year,
                                            seriesName,
                                            current.name);
        }

        return reconstructed;
    }

    private double residualFactor(double sourceRate,
            double trend,
            int year,
            String seriesName) throws Exception
    {
        if (!Double.isFinite(sourceRate) || sourceRate <= 0.0)
            throw evaluationError("EvalStabilizedV2: invalid source " + seriesName + " rate in " + year);

        if (!Double.isFinite(trend) || trend <= 0.0)
            throw evaluationError("EvalStabilizedV2: invalid " + seriesName + " trend in " + year);

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

    private static double weightedStableRate(List<Integer> years,
            Map<Integer, Long> source,
            double[] population,
            int stableYear1,
            int stableYear2) throws Exception
    {
        double countSum = 0.0;
        double populationSum = 0.0;

        for (int k = 0; k < years.size(); k++)
        {
            int year = years.get(k);
            if (inside(year, stableYear1, stableYear2))
            {
                countSum += source.get(year);
                populationSum += population[k];
            }
        }

        double rate = countSum / populationSum;
        if (!Double.isFinite(rate) || rate <= 0.0)
            throw evaluationError("EvalStabilizedV2: invalid stabilized rate");

        return rate;
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
            TerritoryYear ty = territory.territoryYearOrNull(years.get(k));
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
                throw evaluationError("EvalStabilizedV2: missing year " + year + " for " + territory.name);

            if (ty.births == null || ty.births.total == null ||
                ty.births.total.both == null ||
                ty.deaths == null || ty.deaths.total == null ||
                ty.deaths.total.both == null ||
                ty.progressive_population == null ||
                ty.progressive_population.total == null ||
                ty.progressive_population.total.both == null)
            {
                throw evaluationError("EvalStabilizedV2: incomplete data in " + year + " for " + territory.name);
            }

            if (ty.births.total.both <= 0 || ty.deaths.total.both <= 0)
                throw evaluationError("EvalStabilizedV2: non-positive births/deaths in " + year + " for " + territory.name);

            requirePopulation(ty, year, territory.name);
        }
    }

    private static void validateStableInterval(List<Integer> years,
            int year1,
            int year2,
            String seriesName,
            String territoryName) throws Exception
    {
        for (int year = year1; year <= year2; year++)
        {
            if (!years.contains(year))
            {
                throw evaluationError("EvalStabilizedV2: stabilized " + seriesName + " interval contains missing year " + year + " for "
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
            throw evaluationError("EvalStabilizedV2: invalid progressive population in " + year + " for " + territoryName);
        }

        return ty.progressive_population.total.both;
    }

    private static long roundedCount(double value,
            int year,
            String seriesName,
            String territoryName) throws Exception
    {
        if (!Double.isFinite(value) || value < 0.0 || value > Long.MAX_VALUE)
            throw evaluationError("EvalStabilizedV2: invalid reconstructed " + seriesName + " count in " + year + " for " + territoryName);

        return Math.round(value);
    }

    private static boolean inside(int year, int year1, int year2)
    {
        return year >= year1 && year <= year2;
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

    private static class Special1892
    {
        final boolean active;
        final double replacementCount;
        final double multiplier;

        private Special1892(boolean active,
                double replacementCount,
                double multiplier)
        {
            this.active = active;
            this.replacementCount = replacementCount;
            this.multiplier = multiplier;
        }

        static Special1892 forBirths(Map<Integer, Long> source)
        {
            return create(source, false);
        }

        static Special1892 forDeaths(Map<Integer, Long> source)
        {
            return create(source, true);
        }

        private static Special1892 create(Map<Integer, Long> source,
                boolean preserveRise)
        {
            Long value1891 = source.get(1891);
            Long value1892 = source.get(1892);
            Long value1893 = source.get(1893);

            if (value1891 == null || value1892 == null || value1893 == null)
                return inactive();

            double adjacentAverage = (value1891 + value1893) / 2.0;
            if (adjacentAverage <= 0.0)
                return inactive();

            boolean active = preserveRise ? value1892 > adjacentAverage
                                          : value1892 < adjacentAverage;

            if (!active)
                return inactive();

            return new Special1892(true,
                                   adjacentAverage,
                                   value1892 / adjacentAverage);
        }

        private static Special1892 inactive()
        {
            return new Special1892(false, 0.0, 1.0);
        }
    }
}
