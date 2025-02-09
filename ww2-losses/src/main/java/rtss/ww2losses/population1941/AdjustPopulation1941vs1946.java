package rtss.ww2losses.population1941;

import java.util.ArrayList;
import java.util.List;

import rtss.data.ValueConstraint;
import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.population.struct.PopulationContext;
import rtss.data.population.synthetic.PopulationADH;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;
import rtss.util.plot.ChartXYSplineAdvanced;
import rtss.ww2losses.ageline.BacktrackPopulation;
import rtss.ww2losses.ageline.warmodel.WarAttritionModel;
import rtss.ww2losses.helpers.PeacetimeMortalityTables;
import rtss.ww2losses.params.AreaParameters;
import rtss.ww2losses.population1941.math.RefineSeries;

/* 
 * Перераспределить население внутри 5-летних групп аггреграции
 * для устаренения артефактов отрицательной величины потерь в 1941-1945 гг.
 * для некоторых возрастных линий.
 */
public class AdjustPopulation1941vs1946
{
    final private AreaParameters ap;
    final PeacetimeMortalityTables peacetimeMortalityTables;
    final WarAttritionModel wam;
    final PopulationContext p1946_actual;
    
    final int DAYS_PER_YEAR = 365;

    public AdjustPopulation1941vs1946(AreaParameters ap, PeacetimeMortalityTables peacetimeMortalityTables, WarAttritionModel wam,
            final PopulationContext p1946_actual)
    {
        this.ap = ap;
        this.peacetimeMortalityTables = peacetimeMortalityTables;
        this.wam = wam;
        this.p1946_actual = p1946_actual;
    }

    /*
     * Если требуется перераспределение, возвращает перераспределённое население.
     * Если перераспределения не требуется, возвращает null.
     */
    public PopulationContext refine(PopulationContext p_start1941) throws Exception
    {
        /* требуется учёт миграции */
        if (ap.area == Area.RSFSR)
            return null;

        BacktrackPopulation backtrack = new BacktrackPopulation(peacetimeMortalityTables, wam, p1946_actual);

        /* 
         * Для каждой возрастной линии вычислить требуемое минимальное население @pmin, 
         * обеспечивающее достижение населения в начале 1946 года при заданном 
         * (в данном случае нулевом) уровне военных потерь.
         */
        PopulationContext pmin = backtrack.population_1946_to_early1941(null);

        // population excess over minimum
        PopulationContext p_excess = p_start1941.sub(pmin, ValueConstraint.NONE);

        // check if need to adjust it
        boolean do_adjust = false;
        final double min_margin = 0.005;
        final int cutoff_age = 80;
        for (Gender gender : Gender.TwoGenders)
        {
            // population excess over minimum
            double[] a_excess = p_excess.asArray(Locality.TOTAL, gender);
            if (checkNegativeRegions(a_excess, pmin, gender, min_margin, cutoff_age, true))
                do_adjust = true;
        }

        if (!do_adjust)
            return null;

        // will adjust and collect the adjusted excess here 
        PopulationContext p_excess2 = PopulationContext.newTotalPopulationContext(ValueConstraint.NONE);

        for (Gender gender : Gender.TwoGenders)
        {
            // population excess over minimum
            double[] a_excess = p_excess.asArray(Locality.TOTAL, gender);

            // ### must_reduce
            // ### may_increase

            // redistribute the excess
            RefineSeries rs = new RefineSeries();
            rs.minRelativeLevel = 0.3;
            rs.sigma = 10.0;
            rs.gaussianKernelWindow = 50;
            double[] a_excess2 = rs.modifySeries(a_excess, PopulationADH.AgeBinWidthsDays(), a_excess.length - 1);
            p_excess2.fromArray(Locality.TOTAL, gender, a_excess2);
            ChartXYSplineAdvanced.display2("Refinement " + gender.name(), a_excess, a_excess2);
            Util.noop();
        }

        PopulationContext p_new1941 = pmin.add(p_excess2);

        for (Gender gender : Gender.TwoGenders)
        {
            Util.assertion(Util.same(p_new1941.sum(gender), p_start1941.sum(gender)));

            for (Bin bin : Bins.forWidths(PopulationADH.AgeBinWidthsDays()))
            {
                double v1 = p_new1941.sumDays(gender, bin.age_x1, bin.age_x2);
                double v2 = p_start1941.sumDays(gender, bin.age_x1, bin.age_x2);
                Util.assertion(Util.same(v1, v2));
            }
        }

        return p_new1941;
    }

    /* =============================================================================================== */

    private boolean checkNegativeRegions(double[] a, PopulationContext p_start1941, Gender gender, double amin, int cutoff_age, boolean print)
            throws Exception
    {
        List<Region> list = negativeRegions(a, p_start1941, gender, amin);

        if (print)
            Util.err("Отрицательные районы в населении 1941 года " + gender.name());

        if (list.size() == 0)
        {
            if (print)
                Util.err("    нет");

            return false;
        }
        
        boolean hasNegativeRegions = false;

        for (Region r : list)
        {
            if (print)
            {
                Util.err(String.format("    %5d - %5d  [%5d] =  %7.3f - %7.3f",
                                       r.nd1, r.nd2, r.nd2 - r.nd1 + 1,
                                       day2year(r.nd1), day2year(r.nd2)));
            }
            
            if (r.nd1 < cutoff_age * DAYS_PER_YEAR)
                hasNegativeRegions = true;
        }

        return hasNegativeRegions;
    }

    public static class Region
    {
        public int nd1;
        public int nd2;
    }

    public List<Region> negativeRegions(double[] a, PopulationContext p_start1941, Gender gender, double amin) throws Exception
    {
        List<Region> list = new ArrayList<>();

        for (int nd = 0; nd < a.length;)
        {
            double vmin = amin * p_start1941.getDay(Locality.TOTAL, gender, nd);

            // find first negative point
            while (nd < a.length && a[nd] >= vmin)
                nd++;
            if (nd == a.length)
                break;

            Region r = new Region();
            r.nd1 = nd;

            // find first positive point
            while (nd < a.length && a[nd] < vmin)
                nd++;
            r.nd2 = nd - 1;
            list.add(r);
        }

        return list;
    }

    private double day2year(int nd)
    {
        return nd / 365.0;
    }
}
