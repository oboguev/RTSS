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
 * Reconstructs births and deaths for the Fergana oblast.
 *
 * <p>This class deliberately keeps the Fergana-specific rules out of
 * {@link EvalStabilizedV2}:</p>
 *
 * <ul>
 *   <li>1881-1895: flat CBR and CDR based on the stabilized interval;</li>
 *   <li>1896-1911: relative deviations around a robust local trend are
 *       retained, with their strength controlled by {@code lambda};</li>
 *   <li>1902-1904: the defective source block is replaced, for purposes of
 *       both trend estimation and reconstruction, by geometric interpolation
 *       between 1901 and 1905;</li>
 *   <li>1902 deaths: 4,652 victims of the Andijan earthquake are added to the
 *       reconstructed background mortality;</li>
 *   <li>1912-1913: the stabilized interval is preserved exactly;</li>
 *   <li>1914: the 1913 CBR and CDR, rather than the 1913 counts, are carried
 *       forward.</li>
 * </ul>
 *
 * <p>No special adjustment is made for 1892. The method never modifies its
 * input territory.</p>
 *
 * <p>lambda = 0 gives a flat reconstruction apart from the explicit Fergana
 * corrections; lambda = 0.5 partially retains the cleaned shape; lambda = 1
 * retains it fully.</p>
 */
public class EvalStabilizedFergana
{
    private static final int MAX_ITERATIONS = 200;

    private static final int fromYear = 1881;
    private static final int flatToYear = 1895;
    private static final int preserveFromYear = 1896;

    private static final int interpolationLeftYear = 1901;
    private static final int interpolationFirstYear = 1902;
    private static final int interpolationLastYear = 1904;
    private static final int interpolationRightYear = 1905;

    private static final int stableYear1 = 1912;
    private static final int stableYear2 = 1913;
    private static final int carriedRateYear = 1914;
    private static final int toYear = 1914;

    private static final long earthquakeDeaths1902 = 4_652L;

    private final TerritoryDataSet tdsCensus1897;
    private final int windowWidth;
    private final double lambda;

    public EvalStabilizedFergana(TerritoryDataSet tdsCensus1897,
            int windowWidth,
            double lambda)
    {
        if (tdsCensus1897 == null)
            throw constructorError("EvalStabilizedFergana: tdsCensus1897 is null");

        if (windowWidth < 3 || windowWidth % 2 == 0)
            throw constructorError("EvalStabilizedFergana: windowWidth must be an odd integer >= 3");

        if (!Double.isFinite(lambda) || lambda < 0.0 || lambda > 1.0)
            throw constructorError("EvalStabilizedFergana: lambda must be between 0 and 1");

        this.tdsCensus1897 = tdsCensus1897;
        this.windowWidth = windowWidth;
        this.lambda = lambda;
    }

    /**
     * Reconstructs the Fergana series for 1881-1914.
     *
     * @param t source territory; it is treated as immutable
     */
    public Territory evalTerritory(Territory t) throws Exception
    {
        if (t == null)
            throw evaluationError("EvalStabilizedFergana: territory is null");

        Territory censusTerritory = tdsCensus1897.get(t.name);
        if (censusTerritory == null)
            throw evaluationError("EvalStabilizedFergana: no 1897 census territory for " + t.name);

        Territory result = t.dup();
        List<Integer> years = reconstructionYears(result);

        validateRequiredYears(years, t.name);
        validateTerritoryData(t, years);

        Map<Integer, Long> sourceBirths = sourceCounts(t, years, Series.BIRTHS);
        Map<Integer, Long> sourceDeaths = sourceCounts(t, years, Series.DEATHS);

        // Establish a population series consistent with the unmodified clone.
        EvalProgressive.evalProgressive(result, censusTerritory);

        long[][] twoIterationsAgo = null;

        for (int iteration = 1; iteration <= MAX_ITERATIONS; iteration++)
        {
            long[][] before = snapshot(result, years);

            long[] reconstructedBirths = reconstruct(result,
                                                     years,
                                                     sourceBirths,
                                                     Series.BIRTHS);

            long[] reconstructedDeaths = reconstruct(result,
                                                     years,
                                                     sourceDeaths,
                                                     Series.DEATHS);

            apply(result, years, reconstructedBirths, Series.BIRTHS);
            apply(result, years, reconstructedDeaths, Series.DEATHS);

            EvalProgressive.evalProgressive(result, censusTerritory);

            long[][] after = snapshot(result, years);

            if (sameState(before, after))
                return result;

            /*
             * Integer rounding may produce a two-state cycle in which a few
             * annual counts alternate by one. Either state represents the
             * same continuous solution to the available precision.
             */
            if (twoIterationsAgo != null && sameState(twoIterationsAgo, after))
            {
                if (differsOnlyByRounding(before, after))
                    return result;

                throw evaluationError("EvalStabilizedFergana: entered a non-trivial two-state cycle for " + t.name);
            }

            twoIterationsAgo = before;
        }

        throw evaluationError("EvalStabilizedFergana: failed to converge after " + MAX_ITERATIONS + " iterations for " + t.name);
    }

    private long[] reconstruct(Territory current,
            List<Integer> years,
            Map<Integer, Long> source,
            Series series) throws Exception
    {
        int n = years.size();
        double[] population = new double[n];
        double[] cleanedRates = new double[n];

        for (int k = 0; k < n; k++)
        {
            int year = years.get(k);
            TerritoryYear ty = current.territoryYearOrNull(year);
            long p = requirePopulation(ty, year, current.name);

            population[k] = p;
            cleanedRates[k] = source.get(year) / (double) p;
        }

        repairInterpolationBlock(years, cleanedRates, series, current.name);

        /*
         * The raw 1914 decline is not allowed to affect even the nearby
         * moving-median trend. For trend purposes 1914 has the 1913 rate.
         */
        cleanedRates[indexOf(years, carriedRateYear, current.name)] = cleanedRates[indexOf(years, stableYear2, current.name)];

        double[] trend = centeredMovingMedian(years, cleanedRates);
        double stableRate = weightedStableRate(years, source, population);

        /*
         * Normalize only the years in which residual fluctuations are
         * actually retained. Thus the 1881-1895 segment remains exactly flat,
         * and the explicit earthquake increment does not lower other years.
         */
        double weightedFactorSum = 0.0;
        double weightSum = 0.0;

        for (int k = 0; k < n; k++)
        {
            int year = years.get(k);
            if (!preservesShape(year))
                continue;

            double factor = residualFactor(cleanedRates[k],
                                           trend[k],
                                           year,
                                           series,
                                           current.name);

            weightedFactorSum += population[k] * factor;
            weightSum += population[k];
        }

        double normalization = weightSum == 0.0 ? 1.0
                                                : weightedFactorSum / weightSum;

        if (!Double.isFinite(normalization) || normalization <= 0.0)
            throw evaluationError("EvalStabilizedFergana: invalid " + series.displayName + " residual normalization for " + current.name);

        long[] reconstructed = new long[n];
        int index1913 = indexOf(years, stableYear2, current.name);
        double rate1913 = source.get(stableYear2) / population[index1913];

        for (int k = 0; k < n; k++)
        {
            int year = years.get(k);

            if (inside(year, stableYear1, stableYear2))
            {
                // Preserve the stabilized interval exactly as supplied.
                reconstructed[k] = source.get(year);
                continue;
            }

            double reconstructedRate;

            if (year <= flatToYear)
            {
                reconstructedRate = stableRate;
            }
            else if (year == carriedRateYear)
            {
                reconstructedRate = rate1913;
            }
            else
            {
                double factor = residualFactor(cleanedRates[k],
                                               trend[k],
                                               year,
                                               series,
                                               current.name);

                reconstructedRate = stableRate * factor / normalization;
            }

            long count = roundedCount(reconstructedRate * population[k],
                                      year,
                                      series.displayName,
                                      current.name);

            if (series == Series.DEATHS && year == 1902)
                count = addExact(count, earthquakeDeaths1902, year, current.name);

            reconstructed[k] = count;
        }

        return reconstructed;
    }

    private static void repairInterpolationBlock(List<Integer> years,
            double[] rates,
            Series series,
            String territoryName) throws Exception
    {
        int leftIndex = indexOf(years, interpolationLeftYear, territoryName);
        int rightIndex = indexOf(years, interpolationRightYear, territoryName);
        double leftRate = rates[leftIndex];
        double rightRate = rates[rightIndex];

        if (!Double.isFinite(leftRate) || leftRate <= 0.0 ||
            !Double.isFinite(rightRate) || rightRate <= 0.0)
        {
            throw evaluationError("EvalStabilizedFergana: invalid " + series.displayName + " interpolation anchors for " + territoryName);
        }

        double logLeft = Math.log(leftRate);
        double logRight = Math.log(rightRate);
        double yearSpan = interpolationRightYear - interpolationLeftYear;

        for (int year = interpolationFirstYear; year <= interpolationLastYear; year++)
        {
            double fraction = (year - interpolationLeftYear) / yearSpan;
            double rate = Math.exp(logLeft + fraction * (logRight - logLeft));
            rates[indexOf(years, year, territoryName)] = rate;
        }
    }

    /**
     * Builds a centered moving median only inside 1896-1914. Source data from
     * the flat 1881-1895 segment therefore cannot distort the retained shape
     * at the 1896 boundary.
     */
    private double[] centeredMovingMedian(List<Integer> years,
            double[] values) throws Exception
    {
        int n = values.length;
        int halfWindow = windowWidth / 2;
        int segmentFrom = indexOf(years, preserveFromYear, "Fergana");
        int segmentTo = indexOf(years, toYear, "Fergana");
        double[] trend = new double[n];
        Arrays.fill(trend, Double.NaN);

        for (int k = segmentFrom; k <= segmentTo; k++)
        {
            int from = Math.max(segmentFrom, k - halfWindow);
            int to = Math.min(segmentTo, k + halfWindow);
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

    private double residualFactor(double sourceRate,
            double trend,
            int year,
            Series series,
            String territoryName) throws Exception
    {
        if (!Double.isFinite(sourceRate) || sourceRate <= 0.0)
            throw evaluationError("EvalStabilizedFergana: invalid source " + series.displayName + " rate in " + year + " for " + territoryName);

        if (!Double.isFinite(trend) || trend <= 0.0)
            throw evaluationError("EvalStabilizedFergana: invalid " + series.displayName + " trend in " + year + " for " + territoryName);

        return Math.pow(sourceRate / trend, lambda);
    }

    private static double weightedStableRate(List<Integer> years,
            Map<Integer, Long> source,
            double[] population) throws Exception
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
            throw evaluationError("EvalStabilizedFergana: invalid stabilized rate");

        return rate;
    }

    private static List<Integer> reconstructionYears(Territory territory)
    {
        List<Integer> years = new ArrayList<Integer>();

        for (int year : territory.years())
        {
            if (year >= fromYear && year <= toYear)
                years.add(year);
        }

        Collections.sort(years);
        return years;
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

    private static void validateRequiredYears(List<Integer> years,
            String territoryName) throws Exception
    {
        for (int year = fromYear; year <= toYear; year++)
        {
            if (!years.contains(year))
                throw evaluationError("EvalStabilizedFergana: missing year " + year + " for " + territoryName);
        }
    }

    private static void validateTerritoryData(Territory territory,
            List<Integer> years) throws Exception
    {
        for (int year : years)
        {
            TerritoryYear ty = territory.territoryYearOrNull(year);
            if (ty == null)
                throw evaluationError("EvalStabilizedFergana: missing year " + year + " for " + territory.name);

            if (ty.births == null || ty.births.total == null ||
                ty.births.total.both == null ||
                ty.deaths == null || ty.deaths.total == null ||
                ty.deaths.total.both == null ||
                ty.progressive_population == null ||
                ty.progressive_population.total == null ||
                ty.progressive_population.total.both == null)
            {
                throw evaluationError("EvalStabilizedFergana: incomplete data in " + year + " for " + territory.name);
            }

            if (ty.births.total.both <= 0 || ty.deaths.total.both <= 0)
                throw evaluationError("EvalStabilizedFergana: non-positive births/deaths in " + year + " for " + territory.name);

            requirePopulation(ty, year, territory.name);
        }
    }

    private static int indexOf(List<Integer> years,
            int year,
            String territoryName) throws Exception
    {
        int index = years.indexOf(year);
        if (index < 0)
            throw evaluationError("EvalStabilizedFergana: missing year " + year + " for " + territoryName);

        return index;
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
            throw evaluationError("EvalStabilizedFergana: invalid progressive population in " + year + " for " + territoryName);
        }

        return ty.progressive_population.total.both;
    }

    private static long roundedCount(double value,
            int year,
            String seriesName,
            String territoryName) throws Exception
    {
        if (!Double.isFinite(value) || value < 0.0 || value > Long.MAX_VALUE)
            throw evaluationError("EvalStabilizedFergana: invalid reconstructed " + seriesName + " count in " + year + " for " + territoryName);

        return Math.round(value);
    }

    private static long addExact(long background,
            long increment,
            int year,
            String territoryName) throws Exception
    {
        try
        {
            return Math.addExact(background, increment);
        }
        catch (ArithmeticException ex)
        {
            throw evaluationError("EvalStabilizedFergana: reconstructed deaths overflow in " + year + " for " + territoryName);
        }
    }

    private static boolean preservesShape(int year)
    {
        return year >= preserveFromYear && year < stableYear1;
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
        BIRTHS("births"), DEATHS("deaths");

        final String displayName;

        Series(String displayName)
        {
            this.displayName = displayName;
        }
    }
}
