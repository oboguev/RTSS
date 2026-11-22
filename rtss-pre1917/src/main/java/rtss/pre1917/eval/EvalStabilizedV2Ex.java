package rtss.pre1917.eval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtss.data.selectors.BirthDeathBoth;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.util.Util;

/**
 * Reconstructs a selected births/deaths series in an explicitly specified
 * interval, using a reliable reference interval to calibrate its mean rate.
 *
 * <p>The mean CBR/CDR is population-weighted, i.e. total events divided by
 * total progressive population over the interval (times 1,000, if expressed
 * per thousand). The corrected interval has the same aggregate rate as the
 * reference interval, within the unavoidable rounding of event counts.</p>
 *
 * <p>Year-to-year deviations are taken from the source series relative to a
 * centered moving median. {@code lambda} controls their retention: 0 gives
 * a flat rate, 1 retains the full relative residual. When {@code adjust1892}
 * is enabled, the special 1892 anomaly is retained in the shape of the
 * reconstruction, but is included in normalization so that the requested
 * equality of mean rates still holds.</p>
 *
 * <p>The reference and correction intervals must be nonoverlapping. Only
 * selected events inside the correction interval are changed; all other
 * births and deaths are preserved. Progressive population is recalculated
 * iteratively. Neither source territory nor census territory is modified.</p>
 *
 * <p>Known gaps, administrative breaks, and other defective source intervals
 * should be repaired before this class is used.</p>
 */
public class EvalStabilizedV2Ex
{
    private static final int MAX_ITERATIONS = 200;
    private static final int fromYear = 1881;
    private static final int toYear = 1914;

    private final TerritoryDataSet tdsCensus1897;
    private final int windowWidth;
    private final double lambda;
    private final boolean adjust1892;

    public EvalStabilizedV2Ex(TerritoryDataSet tdsCensus1897,
            int windowWidth,
            double lambda,
            boolean adjust1892)
    {
        if (windowWidth < 3 || windowWidth % 2 == 0)
            throw constructorError("EvalStabilizedV2Ex: windowWidth must be an odd integer >= 3");

        if (!Double.isFinite(lambda) || lambda < 0.0 || lambda > 1.0)
            throw constructorError("EvalStabilizedV2Ex: lambda must be between 0 and 1");

        this.tdsCensus1897 = tdsCensus1897;
        this.windowWidth = windowWidth;
        this.lambda = lambda;
        this.adjust1892 = adjust1892;
    }

    /**
     * @param t source territory; it is treated as immutable
     * @param bd BIRTH, DEATH, or BIRTH_DEATH
     * @param in_y1 first reliable reference year (inclusive)
     * @param in_y2 last reliable reference year (inclusive)
     * @param out_y1 first year to correct (inclusive)
     * @param out_y2 last year to correct (inclusive)
     */
    public Territory evalTerritory(Territory t,
            BirthDeathBoth bd,
            int in_y1,
            int in_y2,
            int out_y1,
            int out_y2) throws Exception
    {
        if (t == null)
            throw evaluationError("EvalStabilizedV2Ex: territory is null");

        if (tdsCensus1897 == null)
            throw constructorError("EvalStabilizedV2Ex: tdsCensus1897 is null");

        Territory censusTerritory = tdsCensus1897.get(t.name);
        if (censusTerritory == null)
            throw evaluationError("EvalStabilizedV2Ex: no 1897 census territory for " + t.name);

        return evalTerritory(t, censusTerritory, bd, in_y1, in_y2, out_y1, out_y2);
    }

    /**
     * @param t source territory; it is treated as immutable
     * @param censusTerritory census anchor used by EvalProgressive
     * @param bd BIRTH, DEATH, or BIRTH_DEATH
     * @param in_y1 first reliable reference year (inclusive)
     * @param in_y2 last reliable reference year (inclusive)
     * @param out_y1 first year to correct (inclusive)
     * @param out_y2 last year to correct (inclusive)
     */
    public Territory evalTerritory(Territory t, Territory censusTerritory,
            BirthDeathBoth bd,
            int in_y1,
            int in_y2,
            int out_y1,
            int out_y2) throws Exception
    {
        if (t == null)
            throw evaluationError("EvalStabilizedV2Ex: territory is null");

        if (censusTerritory == null)
            throw evaluationError("EvalStabilizedV2Ex: census territory is null for " + t.name);

        if (bd == null)
            throw evaluationError("EvalStabilizedV2Ex: BirthDeathBoth is null");

        if (in_y1 > in_y2)
            throw evaluationError("EvalStabilizedV2Ex: invalid reference interval " + in_y1 + "-" + in_y2);

        if (out_y1 > out_y2)
            throw evaluationError("EvalStabilizedV2Ex: invalid correction interval " + out_y1 + "-" + out_y2);

        if (in_y1 <= out_y2 && out_y1 <= in_y2)
            throw evaluationError("EvalStabilizedV2Ex: reference and correction intervals overlap for " + t.name);

        if (in_y1 < fromYear || in_y2 > toYear || out_y1 < fromYear || out_y2 > toYear)
            throw evaluationError("EvalStabilizedV2Ex: intervals must lie within " + fromYear + "-" + toYear);

        Territory result = t.dup();
        List<Integer> years = new ArrayList<Integer>();
        for (int year : result.years())
        {
            if (year >= fromYear && year <= toYear)
                years.add(year);
        }
        Collections.sort(years);

        if (years.isEmpty())
            throw evaluationError("EvalStabilizedV2Ex: no data in " + fromYear + "-" + toYear + " for " + t.name);

        validateInterval(years, in_y1, in_y2, "reference", t.name);
        validateInterval(years, out_y1, out_y2, "correction", t.name);
        validateTerritoryData(t, years);

        boolean doBirths = bd == BirthDeathBoth.BIRTH || bd == BirthDeathBoth.BIRTH_DEATH;
        boolean doDeaths = bd == BirthDeathBoth.DEATH || bd == BirthDeathBoth.BIRTH_DEATH;

        Map<Integer, Long> sourceBirths = doBirths ? sourceCounts(t, years, Series.BIRTHS) : null;
        Map<Integer, Long> sourceDeaths = doDeaths ? sourceCounts(t, years, Series.DEATHS) : null;

        Special1892 births1892 = doBirths && adjust1892 ? Special1892.forBirths(sourceBirths)
                                                         : Special1892.inactive();
        Special1892 deaths1892 = doDeaths && adjust1892 ? Special1892.forDeaths(sourceDeaths)
                                                         : Special1892.inactive();

        // Establish a population series consistent with the unmodified clone.
        EvalProgressive.evalProgressive(result, censusTerritory);

        long[][] twoIterationsAgo = null;

        for (int iteration = 1; ; iteration++)
        {
            long[][] before = snapshot(result, years);

            if (doBirths)
            {
                long[] reconstructedBirths = reconstruct(result, years, sourceBirths,
                        in_y1, in_y2, out_y1, out_y2, births1892, "births");
                apply(result, years, reconstructedBirths, Series.BIRTHS);
            }

            if (doDeaths)
            {
                long[] reconstructedDeaths = reconstruct(result, years, sourceDeaths,
                        in_y1, in_y2, out_y1, out_y2, deaths1892, "deaths");
                apply(result, years, reconstructedDeaths, Series.DEATHS);
            }

            EvalProgressive.evalProgressive(result, censusTerritory);

            long[][] after = snapshot(result, years);

            if (sameState(before, after))
                return result;

            /*
             * Integer event totals may prevent a strict fixed point. As in V2,
             * allow small successive differences caused by rounding.
             */
            if (twoIterationsAgo != null &&
                differsOnlyByRounding(twoIterationsAgo, before) &&
                differsOnlyByRounding(before, after))
            {
                return result;
            }

            if (twoIterationsAgo != null && sameState(twoIterationsAgo, after))
                throw evaluationError("EvalStabilizedV2Ex: entered a non-trivial two-state cycle for " + t.name);

            twoIterationsAgo = before;

            if (iteration == MAX_ITERATIONS)
                throw evaluationError("EvalStabilizedV2Ex: failed to converge after " + MAX_ITERATIONS
                                      + " iterations for " + t.name);
        }
    }

    private long[] reconstruct(Territory current,
            List<Integer> years,
            Map<Integer, Long> source,
            int in_y1,
            int in_y2,
            int out_y1,
            int out_y2,
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
        double referenceRate = weightedStableRate(years, source, population, in_y1, in_y2);

        /*
         * Normalize only over the explicitly requested correction interval.
         * Include the special-1892 multiplier when it applies: the mean rate
         * must equal the reference mean even if there is an epidemic spike.
         */
        double weightedFactorSum = 0.0;
        double outPopulationSum = 0.0;
        double[] factors = new double[n];
        List<Integer> correctedIndices = new ArrayList<Integer>();

        for (int k = 0; k < n; k++)
        {
            int year = years.get(k);
            if (!inside(year, out_y1, out_y2))
                continue;

            double factor = residualFactor(sourceRatesForTrend[k], trend[k], year, seriesName);
            if (year == 1892 && special1892.active)
                factor *= special1892.multiplier;

            if (!Double.isFinite(factor) || factor <= 0.0)
                throw evaluationError("EvalStabilizedV2Ex: invalid " + seriesName + " factor in " + year);

            factors[k] = factor;
            weightedFactorSum += population[k] * factor;
            outPopulationSum += population[k];
            correctedIndices.add(k);
        }

        double normalization = weightedFactorSum / outPopulationSum;
        if (!Double.isFinite(normalization) || normalization <= 0.0)
            throw evaluationError("EvalStabilizedV2Ex: invalid " + seriesName + " residual normalization for " + current.name);

        // Years outside [out_y1, out_y2] are left EXACTLY as in the source.
        long[] reconstructed = new long[n];
        for (int k = 0; k < n; k++)
            reconstructed[k] = source.get(years.get(k));

        /*
         * Balanced rounding preserves the aggregate mean rate to the nearest
         * whole event, avoiding the cumulative rounding drift of Math.round()
         * on every year independently.
         */
        long targetTotal = roundedCount(referenceRate * outPopulationSum,
                                        out_y1, seriesName + " total", current.name);
        long floorSum = 0;
        double[] fractions = new double[n];

        for (int k : correctedIndices)
        {
            int year = years.get(k);
            double expected = referenceRate * population[k] * factors[k] / normalization;
            if (!Double.isFinite(expected) || expected < 0.0 || expected >= Long.MAX_VALUE)
                throw evaluationError("EvalStabilizedV2Ex: invalid reconstructed " + seriesName + " count in " + year);

            long floor = (long) Math.floor(expected);
            if (floorSum > Long.MAX_VALUE - floor)
                throw evaluationError("EvalStabilizedV2Ex: reconstructed " + seriesName + " total overflow");

            reconstructed[k] = floor;
            floorSum += floor;
            fractions[k] = expected - floor;
        }

        long difference = targetTotal - floorSum;
        if (difference < 0 || difference > correctedIndices.size())
            throw evaluationError("EvalStabilizedV2Ex: unexpected rounding difference for " + seriesName
                                  + " in " + current.name + ": " + difference);

        Collections.sort(correctedIndices, new Comparator<Integer>()
        {
            @Override
            public int compare(Integer a, Integer b)
            {
                int cmp = Double.compare(fractions[b], fractions[a]);
                return cmp != 0 ? cmp : Integer.compare(years.get(a), years.get(b));
            }
        });

        for (int j = 0; j < difference; j++)
            reconstructed[correctedIndices.get(j)]++;

        return reconstructed;
    }

    private double residualFactor(double sourceRate,
            double trend,
            int year,
            String seriesName) throws Exception
    {
        if (!Double.isFinite(sourceRate) || sourceRate <= 0.0)
            throw evaluationError("EvalStabilizedV2Ex: invalid source " + seriesName + " rate in " + year);

        if (!Double.isFinite(trend) || trend <= 0.0)
            throw evaluationError("EvalStabilizedV2Ex: invalid " + seriesName + " trend in " + year);

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
            throw evaluationError("EvalStabilizedV2Ex: invalid reference rate");

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
                throw evaluationError("EvalStabilizedV2Ex: missing year " + year + " for " + territory.name);

            if (ty.births == null || ty.births.total == null ||
                ty.births.total.both == null ||
                ty.deaths == null || ty.deaths.total == null ||
                ty.deaths.total.both == null ||
                ty.progressive_population == null ||
                ty.progressive_population.total == null ||
                ty.progressive_population.total.both == null)
            {
                throw evaluationError("EvalStabilizedV2Ex: incomplete data in " + year + " for " + territory.name);
            }

            if (ty.births.total.both <= 0 || ty.deaths.total.both <= 0)
                throw evaluationError("EvalStabilizedV2Ex: non-positive births/deaths in " + year + " for " + territory.name);

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
                throw evaluationError("EvalStabilizedV2Ex: " + intervalName + " interval contains missing year "
                                      + year + " for " + territoryName);
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
            throw evaluationError("EvalStabilizedV2Ex: invalid progressive population in " + year + " for " + territoryName);
        }

        return ty.progressive_population.total.both;
    }

    private static long roundedCount(double value,
            int year,
            String seriesName,
            String territoryName) throws Exception
    {
        if (!Double.isFinite(value) || value < 0.0 || value > Long.MAX_VALUE)
            throw evaluationError("EvalStabilizedV2Ex: invalid reconstructed " + seriesName
                                  + " count in " + year + " for " + territoryName);

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
