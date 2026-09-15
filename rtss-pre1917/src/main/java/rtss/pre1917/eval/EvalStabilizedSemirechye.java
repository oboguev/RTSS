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
 * Reconstructs births and deaths for the Semirechye oblast.
 *
 * <p>This class keeps the Semirechye-specific source corrections out of
 * {@link EvalStabilizedV2}. All explicit repairs are performed in rate space
 * and are therefore recomputed after every progressive-population update.</p>
 *
 * <ul>
 *   <li>1886-1887 are interpolated between 1885 and 1888;</li>
 *   <li>1895-1896 are interpolated between 1894 and 1897;</li>
 *   <li>1899-1900 are interpolated between 1898 and 1901;</li>
 *   <li>1902 is interpolated between 1901 and 1903;</li>
 *   <li>1904 is interpolated between 1903 and 1905;</li>
 *   <li>through 1905, the common log-residual of births and deaths is treated
 *       as variation in registration coverage and removed; only the
 *       differential residual is eligible for retention;</li>
 *   <li>1906-1911 retain the ordinary cleaned residuals;</li>
 *   <li>1912-1914 form the stabilized interval and are preserved exactly.</li>
 * </ul>
 *
 * <p>{@code lambda} controls the retained residuals: 0 gives a flat-rate
 * reconstruction outside the stabilized interval, while 1 retains the full
 * eligible cleaned shape. There is no special 1892 adjustment. The method
 * never modifies its input territory.</p>
 */
public class EvalStabilizedSemirechye
{
    private static final int MAX_ITERATIONS = 200;

    private static final int fromYear = 1881;
    private static final int commonModeToYear = 1905;
    private static final int stableYear1 = 1912;
    private static final int stableYear2 = 1914;
    private static final int toYear = 1914;

    private static final InterpolationBlock[] interpolationBlocks = { /*new InterpolationBlock(1885, 1886, 1887, 1888), */
                                                                      new InterpolationBlock(1894, 1895, 1896, 1897),
                                                                      new InterpolationBlock(1898, 1899, 1900, 1901),
                                                                      new InterpolationBlock(1901, 1902, 1902, 1903),
                                                                      new InterpolationBlock(1903, 1904, 1904, 1905)
    };

    private final TerritoryDataSet tdsCensus1897;
    private final int windowWidth;
    private final double lambda;

    public EvalStabilizedSemirechye(TerritoryDataSet tdsCensus1897,
            int windowWidth,
            double lambda)
    {
        if (tdsCensus1897 == null)
            throw constructorError("EvalStabilizedSemirechye: tdsCensus1897 is null");

        if (windowWidth < 3 || windowWidth % 2 == 0)
            throw constructorError("EvalStabilizedSemirechye: windowWidth must be an odd integer >= 3");

        if (!Double.isFinite(lambda) || lambda < 0.0 || lambda > 1.0)
            throw constructorError("EvalStabilizedSemirechye: lambda must be between 0 and 1");

        this.tdsCensus1897 = tdsCensus1897;
        this.windowWidth = windowWidth;
        this.lambda = lambda;
    }

    /**
     * Reconstructs the Semirechye series for 1881-1914.
     *
     * @param t source territory; it is treated as immutable
     */
    public Territory evalTerritory(Territory t) throws Exception
    {
        if (t == null)
            throw evaluationError("EvalStabilizedSemirechye: territory is null");

        Territory censusTerritory = tdsCensus1897.get(t.name);
        if (censusTerritory == null)
            throw evaluationError("EvalStabilizedSemirechye: no 1897 census territory for " + t.name);

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

            Reconstruction reconstructed = reconstruct(result,
                                                       years,
                                                       sourceBirths,
                                                       sourceDeaths);

            apply(result, years, reconstructed.births, Series.BIRTHS);
            apply(result, years, reconstructed.deaths, Series.DEATHS);

            EvalProgressive.evalProgressive(result, censusTerritory);

            long[][] after = snapshot(result, years);

            if (sameState(before, after))
                return result;

            /*
             * Integer rounding may produce A -> B -> A, with a few annual
             * counts alternating by one. Either state represents the same
             * continuous solution to the available precision.
             */
            if (twoIterationsAgo != null && sameState(twoIterationsAgo, after))
            {
                if (differsOnlyByRounding(before, after))
                    return result;

                throw evaluationError("EvalStabilizedSemirechye: entered a non-trivial two-state cycle for " + t.name);
            }

            twoIterationsAgo = before;
        }

        throw evaluationError("EvalStabilizedSemirechye: failed to converge after " + MAX_ITERATIONS + " iterations for " + t.name);
    }

    private Reconstruction reconstruct(Territory current,
            List<Integer> years,
            Map<Integer, Long> sourceBirths,
            Map<Integer, Long> sourceDeaths) throws Exception
    {
        int n = years.size();
        double[] population = new double[n];
        double[] cleanedBirthRates = new double[n];
        double[] cleanedDeathRates = new double[n];

        for (int k = 0; k < n; k++)
        {
            int year = years.get(k);
            TerritoryYear ty = current.territoryYearOrNull(year);
            long p = requirePopulation(ty, year, current.name);

            population[k] = p;
            cleanedBirthRates[k] = sourceBirths.get(year) / (double) p;
            cleanedDeathRates[k] = sourceDeaths.get(year) / (double) p;
        }

        repairInterpolationBlocks(years,
                                  cleanedBirthRates,
                                  Series.BIRTHS,
                                  current.name);
        repairInterpolationBlocks(years,
                                  cleanedDeathRates,
                                  Series.DEATHS,
                                  current.name);

        double[] birthTrend = centeredMovingMedian(cleanedBirthRates);
        double[] deathTrend = centeredMovingMedian(cleanedDeathRates);

        double stableBirthRate = weightedStableRate(years,
                                                    sourceBirths,
                                                    population,
                                                    Series.BIRTHS,
                                                    current.name);
        double stableDeathRate = weightedStableRate(years,
                                                    sourceDeaths,
                                                    population,
                                                    Series.DEATHS,
                                                    current.name);

        double[] birthFactors = new double[n];
        double[] deathFactors = new double[n];

        double weightedBirthFactorSum = 0.0;
        double weightedDeathFactorSum = 0.0;
        double weightSum = 0.0;

        for (int k = 0; k < n; k++)
        {
            int year = years.get(k);
            if (inside(year, stableYear1, stableYear2))
                continue;

            double birthResidual = logResidual(cleanedBirthRates[k],
                                               birthTrend[k],
                                               year,
                                               Series.BIRTHS,
                                               current.name);
            double deathResidual = logResidual(cleanedDeathRates[k],
                                               deathTrend[k],
                                               year,
                                               Series.DEATHS,
                                               current.name);

            if (year <= commonModeToYear)
            {
                /*
                 * A synchronous multiplicative movement in births and deaths
                 * is interpreted as changing registration coverage. Removing
                 * their mean log-residual retains only the opposing,
                 * series-specific part of the movement.
                 */
                double commonResidual = (birthResidual + deathResidual) / 2.0;
                birthResidual -= commonResidual;
                deathResidual -= commonResidual;
            }
            birthFactors[k] = retainedFactor(birthResidual,
                                             year,
                                             Series.BIRTHS,
                                             current.name);
            deathFactors[k] = retainedFactor(deathResidual,
                                             year,
                                             Series.DEATHS,
                                             current.name);

            weightedBirthFactorSum += population[k] * birthFactors[k];
            weightedDeathFactorSum += population[k] * deathFactors[k];
            weightSum += population[k];
        }

        double birthNormalization = normalizedFactor(weightedBirthFactorSum,
                                                     weightSum,
                                                     Series.BIRTHS,
                                                     current.name);
        double deathNormalization = normalizedFactor(weightedDeathFactorSum,
                                                     weightSum,
                                                     Series.DEATHS,
                                                     current.name);

        long[] reconstructedBirths = new long[n];
        long[] reconstructedDeaths = new long[n];

        for (int k = 0; k < n; k++)
        {
            int year = years.get(k);

            if (inside(year, stableYear1, stableYear2))
            {
                reconstructedBirths[k] = sourceBirths.get(year);
                reconstructedDeaths[k] = sourceDeaths.get(year);
                continue;
            }

            reconstructedBirths[k] = roundedCount(stableBirthRate
                                                  * birthFactors[k]
                                                  / birthNormalization
                                                  * population[k],
                                                  year,
                                                  Series.BIRTHS,
                                                  current.name);

            reconstructedDeaths[k] = roundedCount(stableDeathRate
                                                  * deathFactors[k]
                                                  / deathNormalization
                                                  * population[k],
                                                  year,
                                                  Series.DEATHS,
                                                  current.name);
        }

        return new Reconstruction(reconstructedBirths, reconstructedDeaths);
    }

    private static void repairInterpolationBlocks(List<Integer> years,
            double[] rates,
            Series series,
            String territoryName) throws Exception
    {
        for (InterpolationBlock block : interpolationBlocks)
            block.apply(years, rates, series, territoryName);
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

    private static double logResidual(double sourceRate,
            double trend,
            int year,
            Series series,
            String territoryName) throws Exception
    {
        if (!Double.isFinite(sourceRate) || sourceRate <= 0.0)
            throw evaluationError("EvalStabilizedSemirechye: invalid source " + series.displayName + " rate in " + year + " for " + territoryName);

        if (!Double.isFinite(trend) || trend <= 0.0)
            throw evaluationError("EvalStabilizedSemirechye: invalid " + series.displayName + " trend in " + year + " for " + territoryName);

        double residual = Math.log(sourceRate / trend);
        if (!Double.isFinite(residual))
            throw evaluationError("EvalStabilizedSemirechye: invalid " + series.displayName + " residual in " + year + " for " + territoryName);

        return residual;
    }

    private double retainedFactor(double residual,
            int year,
            Series series,
            String territoryName) throws Exception
    {
        double factor = Math.exp(lambda * residual);

        if (!Double.isFinite(factor) || factor <= 0.0)
            throw evaluationError("EvalStabilizedSemirechye: invalid retained " + series.displayName + " factor in " + year + " for "
                                  + territoryName);

        return factor;
    }

    private static double normalizedFactor(double weightedFactorSum,
            double weightSum,
            Series series,
            String territoryName) throws Exception
    {
        double normalization = weightSum == 0.0 ? 1.0
                                                : weightedFactorSum / weightSum;

        if (!Double.isFinite(normalization) || normalization <= 0.0)
            throw evaluationError("EvalStabilizedSemirechye: invalid " + series.displayName + " residual normalization for " + territoryName);

        return normalization;
    }

    private static double weightedStableRate(List<Integer> years,
            Map<Integer, Long> source,
            double[] population,
            Series series,
            String territoryName) throws Exception
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
            throw evaluationError("EvalStabilizedSemirechye: invalid stabilized " + series.displayName + " rate for " + territoryName);

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
                throw evaluationError("EvalStabilizedSemirechye: missing year " + year + " for " + territoryName);
        }
    }

    private static void validateTerritoryData(Territory territory,
            List<Integer> years) throws Exception
    {
        for (int year : years)
        {
            TerritoryYear ty = territory.territoryYearOrNull(year);
            if (ty == null)
                throw evaluationError("EvalStabilizedSemirechye: missing year " + year + " for " + territory.name);

            if (ty.births == null || ty.births.total == null ||
                ty.births.total.both == null ||
                ty.deaths == null || ty.deaths.total == null ||
                ty.deaths.total.both == null ||
                ty.progressive_population == null ||
                ty.progressive_population.total == null ||
                ty.progressive_population.total.both == null)
            {
                throw evaluationError("EvalStabilizedSemirechye: incomplete data in " + year + " for " + territory.name);
            }

            if (ty.births.total.both <= 0 || ty.deaths.total.both <= 0)
                throw evaluationError("EvalStabilizedSemirechye: non-positive births/deaths in " + year + " for " + territory.name);

            requirePopulation(ty, year, territory.name);
        }
    }

    private static int indexOf(List<Integer> years,
            int year,
            String territoryName) throws Exception
    {
        int index = years.indexOf(year);
        if (index < 0)
            throw evaluationError("EvalStabilizedSemirechye: missing year " + year + " for " + territoryName);

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
            throw evaluationError("EvalStabilizedSemirechye: invalid progressive population in " + year + " for " + territoryName);
        }

        return ty.progressive_population.total.both;
    }

    private static long roundedCount(double value,
            int year,
            Series series,
            String territoryName) throws Exception
    {
        if (!Double.isFinite(value) || value < 0.0 || value > Long.MAX_VALUE)
            throw evaluationError("EvalStabilizedSemirechye: invalid reconstructed " + series.displayName + " count in " + year + " for "
                                  + territoryName);

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
        BIRTHS("births"), DEATHS("deaths");

        final String displayName;

        Series(String displayName)
        {
            this.displayName = displayName;
        }
    }

    private static class InterpolationBlock
    {
        final int leftYear;
        final int firstYear;
        final int lastYear;
        final int rightYear;

        InterpolationBlock(int leftYear,
                int firstYear,
                int lastYear,
                int rightYear)
        {
            this.leftYear = leftYear;
            this.firstYear = firstYear;
            this.lastYear = lastYear;
            this.rightYear = rightYear;
        }

        void apply(List<Integer> years,
                double[] rates,
                Series series,
                String territoryName) throws Exception
        {
            int leftIndex = indexOf(years, leftYear, territoryName);
            int rightIndex = indexOf(years, rightYear, territoryName);
            double leftRate = rates[leftIndex];
            double rightRate = rates[rightIndex];

            if (!Double.isFinite(leftRate) || leftRate <= 0.0 ||
                !Double.isFinite(rightRate) || rightRate <= 0.0)
            {
                throw evaluationError("EvalStabilizedSemirechye: invalid " + series.displayName + " interpolation anchors " + leftYear + "-"
                                      + rightYear + " for " + territoryName);
            }

            double logLeft = Math.log(leftRate);
            double logRight = Math.log(rightRate);
            double yearSpan = rightYear - leftYear;

            for (int year = firstYear; year <= lastYear; year++)
            {
                double fraction = (year - leftYear) / yearSpan;
                double rate = Math.exp(logLeft + fraction * (logRight - logLeft));
                rates[indexOf(years, year, territoryName)] = rate;
            }
        }
    }

    private static class Reconstruction
    {
        final long[] births;
        final long[] deaths;

        Reconstruction(long[] births, long[] deaths)
        {
            this.births = births;
            this.deaths = deaths;
        }
    }
}
