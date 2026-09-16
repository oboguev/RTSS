package rtss.pre1917.eval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.util.Util;

/**
 * Reconstructs births and deaths for the combined Primorsk-Kamchatka
 * territory.
 *
 * <p>The ordinary reconstruction is first performed by
 * {@link EvalStabilizedV2}. Its result for 1886-1914 is then kept unchanged.
 * The unreliable source shape in 1881-1885 is discarded, and the 1886 CBR
 * and CDR are carried backward as constant rates. Births, deaths, and the
 * progressive population for the early segment are recalculated iteratively
 * until they reach the available integer precision.</p>
 *
 * <p>The early correction is deliberately performed after the V2
 * reconstruction. It is not fed back into V2 residual normalization, because
 * doing so would also rescale the already satisfactory result from 1886
 * onward. The method never modifies its input territory.</p>
 *
 * <p>No special 1892 adjustment is used.</p>
 */
public class EvalStabilizedPrimKamchatka
{
    private static final int MAX_ITERATIONS = 200;

    private static final int fromYear = 1881;
    private static final int flatToYear = 1885;
    private static final int referenceYear = 1886;
    private static final int defaultStableYear1 = 1899;
    private static final int defaultStableYear2 = 1903;
    private static final int toYear = 1914;

    private final TerritoryDataSet tdsCensus1897;
    private final EvalStabilizedV2 baseEvaluator;

    public EvalStabilizedPrimKamchatka(TerritoryDataSet tdsCensus1897,
            int windowWidth,
            double lambda)
    {
        if (tdsCensus1897 == null)
            throw constructorError("EvalStabilizedPrimKamchatka: tdsCensus1897 is null");

        if (windowWidth < 3 || windowWidth % 2 == 0)
            throw constructorError("EvalStabilizedPrimKamchatka: windowWidth must be an odd integer >= 3");

        if (!Double.isFinite(lambda) || lambda < 0.0 || lambda > 1.0)
            throw constructorError("EvalStabilizedPrimKamchatka: lambda must be between 0 and 1");

        this.tdsCensus1897 = tdsCensus1897;
        this.baseEvaluator = new EvalStabilizedV2(tdsCensus1897,
                                                  windowWidth,
                                                  lambda,
                                                  false);
    }

    /**
     * Reconstructs the series using 1899-1903 as the stabilized interval for
     * both births and deaths.
     *
     * @param t source territory; it is treated as immutable
     */
    public Territory evalTerritory(Territory t) throws Exception
    {
        return evalTerritory(t,
                             defaultStableYear1,
                             defaultStableYear2,
                             defaultStableYear1,
                             defaultStableYear2);
    }

    /**
     * Reconstructs the series with explicitly supplied stabilized intervals.
     * The intervals affect the ordinary V2 reconstruction only; 1881-1885
     * are subsequently flattened to the reconstructed 1886 rates.
     *
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
            throw evaluationError("EvalStabilizedPrimKamchatka: territory is null");

        Territory censusTerritory = tdsCensus1897.get(t.name);
        if (censusTerritory == null)
            throw evaluationError("EvalStabilizedPrimKamchatka: no 1897 census territory for " + t.name);

        Territory result = baseEvaluator.evalTerritory(t,
                                                       by1,
                                                       by2,
                                                       dy1,
                                                       dy2);

        validateRequiredYears(result);

        TerritoryYear reference = result.territoryYearOrNull(referenceYear);
        long referencePopulation = requirePopulation(reference,
                                                     referenceYear,
                                                     result.name);

        double birthRate = requireRate(reference.births.total.both,
                                       referencePopulation,
                                       "births",
                                       result.name);
        double deathRate = requireRate(reference.deaths.total.both,
                                       referencePopulation,
                                       "deaths",
                                       result.name);

        List<Integer> earlyYears = years(fromYear, flatToYear);
        List<Integer> preservedYears = years(referenceYear, toYear);
        long[][] preservedState = snapshot(result, preservedYears);
        long[][] twoIterationsAgo = null;

        for (int iteration = 1; iteration <= MAX_ITERATIONS; iteration++)
        {
            long[][] before = snapshot(result, earlyYears);

            applyFlatRates(result, earlyYears, birthRate, deathRate);
            EvalProgressive.evalProgressive(result, censusTerritory);

            long[][] after = snapshot(result, earlyYears);
            long[][] currentPreservedState = snapshot(result, preservedYears);

            if (!sameState(preservedState, currentPreservedState))
            {
                throw evaluationError("EvalStabilizedPrimKamchatka: early-period correction changed 1886-1914 for "
                                      + result.name);
            }

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

                throw evaluationError("EvalStabilizedPrimKamchatka: entered a non-trivial two-state cycle for "
                                      + result.name);
            }

            twoIterationsAgo = before;
        }

        throw evaluationError("EvalStabilizedPrimKamchatka: failed to converge after "
                              + MAX_ITERATIONS + " iterations for " + result.name);
    }

    private static void applyFlatRates(Territory territory,
            List<Integer> years,
            double birthRate,
            double deathRate) throws Exception
    {
        for (int year : years)
        {
            TerritoryYear ty = territory.territoryYearOrNull(year);
            long population = requirePopulation(ty, year, territory.name);

            ty.births.total.both = roundedCount(birthRate * population,
                                                year,
                                                "births",
                                                territory.name);
            ty.deaths.total.both = roundedCount(deathRate * population,
                                                year,
                                                "deaths",
                                                territory.name);
        }
    }

    private static void validateRequiredYears(Territory territory) throws Exception
    {
        for (int year = fromYear; year <= toYear; year++)
        {
            TerritoryYear ty = territory.territoryYearOrNull(year);
            if (ty == null)
                throw evaluationError("EvalStabilizedPrimKamchatka: missing year " + year + " for " + territory.name);

            if (ty.births == null || ty.births.total == null ||
                ty.births.total.both == null ||
                ty.deaths == null || ty.deaths.total == null ||
                ty.deaths.total.both == null)
            {
                throw evaluationError("EvalStabilizedPrimKamchatka: incomplete births/deaths in " + year + " for "
                                      + territory.name);
            }

            if (ty.births.total.both <= 0 || ty.deaths.total.both <= 0)
                throw evaluationError("EvalStabilizedPrimKamchatka: non-positive births/deaths in " + year + " for "
                                      + territory.name);

            requirePopulation(ty, year, territory.name);
        }
    }

    private static List<Integer> years(int year1, int year2)
    {
        List<Integer> years = new ArrayList<Integer>();
        for (int year = year1; year <= year2; year++)
            years.add(year);
        return years;
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

    private static double requireRate(long count,
            long population,
            String seriesName,
            String territoryName) throws Exception
    {
        double rate = count / (double) population;
        if (!Double.isFinite(rate) || rate <= 0.0)
        {
            throw evaluationError("EvalStabilizedPrimKamchatka: invalid reconstructed " + seriesName
                                  + " rate in " + referenceYear + " for " + territoryName);
        }

        return rate;
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
            throw evaluationError("EvalStabilizedPrimKamchatka: invalid progressive population in " + year + " for "
                                  + territoryName);
        }

        return ty.progressive_population.total.both;
    }

    private static long roundedCount(double value,
            int year,
            String seriesName,
            String territoryName) throws Exception
    {
        if (!Double.isFinite(value) || value < 0.0 || value > Long.MAX_VALUE)
        {
            throw evaluationError("EvalStabilizedPrimKamchatka: invalid reconstructed " + seriesName
                                  + " count in " + year + " for " + territoryName);
        }

        return Math.round(value);
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
}
