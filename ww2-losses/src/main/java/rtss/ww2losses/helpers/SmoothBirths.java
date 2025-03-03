package rtss.ww2losses.helpers;

import java.util.ArrayList;
import java.util.List;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.curves.InterpolatePopulationAsMeanPreservingCurve;
import rtss.data.curves.TargetResolution;
import rtss.data.curves.InterpolatePopulationAsMeanPreservingCurve.InterpolationOptions;
import rtss.data.selectors.Gender;
import rtss.util.plot.ChartXY;
import rtss.ww2losses.HalfYearEntries;
import rtss.ww2losses.HalfYearEntry;
import rtss.ww2losses.params.AreaParameters;

import static rtss.data.population.projection.ForwardPopulation.years2days;

public class SmoothBirths
{
    private final int ndays = years2days(0.5);
    private final double PROMILLE = 1000;

    private AreaParameters ap;
    private List<Bin> bins;

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

            double births = ap.CBR_1940_MIDYEAR * pavg / PROMILLE;

            bins.add(new Bin(nd, nd + ndays - 1, births / ndays));
            nd += ndays;
        }

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

        return this;
        
    }

    public void calc() throws Exception
    {
        InterpolationOptions options = new InterpolationOptions().usePrimaryCSASRA(true).usePrimarySPLINE(false).useSecondaryRefineYearlyAges(false);
        double[] births = InterpolatePopulationAsMeanPreservingCurve.curve(Bins.bins(bins), "wartime births", TargetResolution.DAILY, 1942, Gender.MALE, options);
        ChartXY.display("Рождения " + ap.area, births);
        // ###
    }
}
