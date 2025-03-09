package rtss.ww2losses.helpers;

import java.util.ArrayList;
import java.util.List;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.curves.CurveVerifier;
import rtss.math.interpolate.disaggregate.csasra.DisaggregateVariableWidthSeriesWithStartValues;
import rtss.util.Util;
import rtss.util.plot.ChartXY;
import rtss.ww2losses.params.AreaParameters;
import rtss.ww2losses.struct.HalfYearEntries;
import rtss.ww2losses.struct.HalfYearEntry;
import rtss.ww2losses.struct.HalfYearEntries.HalfYearSelector;

import static rtss.data.population.projection.ForwardPopulation.years2days;

public class SmoothBirths
{
    private final int ndays = years2days(0.5);
    private final double PROMILLE = 1000;

    private AreaParameters ap;
    private List<Bin> bins;
    private double[] births_1941_1st_halfyear;

    public SmoothBirths init_nonwar(AreaParameters ap, HalfYearEntries<HalfYearEntry> halves) throws Exception
    {
        this.ap = ap;
        this.bins = new ArrayList<>();
        int nd = 0;

        for (HalfYearEntry he : halves)
        {
            if (he.next == null)
                break;

            double p1 = he.p_nonwar_with_births.sum();
            double p2 = he.next.p_nonwar_with_births.sum();
            double pavg = (p1 + p2) / 2;

            double births = ap.CBR_1940_MIDYEAR * pavg * 0.5 / PROMILLE;

            bins.add(new Bin(nd, nd + ndays - 1, births / ndays));
            nd += ndays;
        }

        births_1941_1st_halfyear = halves.get(1941, HalfYearSelector.FirstHalfYear).expected_nonwar_births_byday;

        return this;
    }

    public SmoothBirths init_actual(AreaParameters ap, HalfYearEntries<HalfYearEntry> halves) throws Exception
    {
        this.ap = ap;
        this.bins = new ArrayList<>();
        int nd = 0;

        for (HalfYearEntry he : halves)
        {
            if (he.next == null)
                break;

            bins.add(new Bin(nd, nd + ndays - 1, he.actual_births / ndays));
            nd += ndays;
        }

        births_1941_1st_halfyear = halves.get(1941, HalfYearSelector.FirstHalfYear).expected_nonwar_births_byday;

        return this;
    }

    public void calc() throws Exception
    {
        Bin[] abins = Bins.bins(bins);
        double[] averages = Bins.midpoint_y(abins);

        int[] intervalWidths = Bins.widths(abins);
        int maxIterations = 5000;
        double positivityThreshold = 1e-6;
        double maxConvergenceDifference = 1e-3;
        double smoothingSigma = 10.0;

        abins[0].avg = averages[0] = Util.average(births_1941_1st_halfyear);

        double[] births = DisaggregateVariableWidthSeriesWithStartValues.disaggregate(averages,
                                                                              intervalWidths,
                                                                              maxIterations,
                                                                              smoothingSigma,
                                                                              positivityThreshold,
                                                                              maxConvergenceDifference,
                                                                              births_1941_1st_halfyear);

        if (!Util.isNonNegative(births))
            throw new Exception("Error calculating curve (negative value)");

        CurveVerifier.validate_means(births, abins);

        new ChartXY("Рождения " + ap.area, "x", "y")
                .addSeries("b1", births)
                .display();
    }
}
