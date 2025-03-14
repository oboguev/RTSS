package rtss.ww2losses.population1941;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import rtss.ww2losses.model.Automation;
import rtss.ww2losses.params.AreaParameters;
// import rtss.ww2losses.population1941.math.RefineSeries;
import rtss.ww2losses.population1941.math.RefineSeries;

/* 
 * Перераспределить население на начало 1941 года внутри 5-летних групп аггреграции
 * для устаренения артефактов отрицательной величины потерь в 1941-1945 гг.
 * для некоторых возрастных линий.
 */
public class AdjustPopulation1941vs1946
{
    final private AreaParameters ap;
    final PeacetimeMortalityTables peacetimeMortalityTables;
    final WarAttritionModel wam;
    final PopulationContext p1946_actual;
    final double min_margin;

    final int DAYS_PER_YEAR = 365;

    public AdjustPopulation1941vs1946(
            AreaParameters ap, 
            PeacetimeMortalityTables peacetimeMortalityTables, 
            WarAttritionModel wam,
            final PopulationContext p1946_actual,
            final double min_margin)
    {
        this.ap = ap;
        this.peacetimeMortalityTables = peacetimeMortalityTables;
        this.wam = wam;
        this.p1946_actual = p1946_actual;
        this.min_margin = min_margin; 
    }

    /*
     * Если требуется перераспределение, возвращает перераспределённое население на начало 1941 года.
     * Если перераспределения не требуется, возвращает null.
     */
    public PopulationContext refine(PopulationContext p_start1941) throws Exception
    {
        return refine(p_start1941, false);
    }

    public PopulationContext refine(PopulationContext p_start1941, boolean recur) throws Exception
    {
        /* требуется учёт миграции */
        if (ap.area == Area.RSFSR)
            return null;

        BacktrackPopulation backtrack = new BacktrackPopulation(ap, peacetimeMortalityTables, wam, p1946_actual);

        /* 
         * Для каждой возрастной линии вычислить требуемое минимальное население @pmin, 
         * обеспечивающее достижение населения в начале 1946 года при заданном 
         * (в данном случае нулевом) уровне военных потерь.
         */
        PopulationContext pmin = backtrack.population_1946_to_early1941(null);

        // population excess over minimum
        PopulationContext p_excess = p_start1941.sub(pmin, ValueConstraint.NONE);

        // check if we need to adjust it
        Set<Gender> do_adjust = new HashSet<>();
        final int cutoff_age = 80;
        for (Gender gender : Gender.TwoGenders)
        {
            double[] minValues = pmin.asArray(Locality.TOTAL, gender);
            minValues = Util.multiply(minValues, min_margin);

            // population excess over minimum
            double[] a_excess = p_excess.asArray(Locality.TOTAL, gender);
            if (checkNegativeRegions(a_excess, minValues, gender, cutoff_age, recur, true))
                do_adjust.add(gender);
        }

        if (do_adjust.isEmpty())
            return null;

        // will adjust and collect the adjusted excess here 
        PopulationContext p_excess2 = p_excess.clone();

        for (Gender gender : Gender.TwoGenders)
        {
            if (!do_adjust.contains(gender))
                continue;

            // population excess over minimum
            double[] a_excess = p_excess.asArray(Locality.TOTAL, gender);

            double[] minValues = pmin.asArray(Locality.TOTAL, gender);
            minValues = Util.multiply(minValues, min_margin);

            // redistribute the excess
            boolean strict = !Automation.isAutomated();
            RefineSeries rs = new RefineSeries();
            rs.sigma = 10.0;
            rs.gaussianKernelWindow = 50;
            rs.minRelativeLevel = 0.3;
            
            if (Automation.isAutomated())
            {
                for (;;)
                {
                    if (checkCanAdjust(rs, gender, a_excess, minValues, strict, Bins.forWidths(PopulationADH.AgeBinWidthsDays())))
                        break;
                    rs.minRelativeLevel -= 0.05;
                    if (rs.minRelativeLevel < 0.01)
                        return null;
                }
            }
            else
            {
                if (!checkCanAdjust(rs, gender, a_excess, minValues, strict, Bins.forWidths(PopulationADH.AgeBinWidthsDays())))
                    return null;
            }
            
            double[] a_excess2 = rs.modifySeries(a_excess, minValues, PopulationADH.AgeBinWidthsDays(), a_excess.length - 1);
            p_excess2.fromArray(Locality.TOTAL, gender, a_excess2);

            // проверки
            Util.checkSame(Util.sum(a_excess), Util.sum(a_excess2));
            for (int nd = 0; nd < a_excess.length; nd++)
                Util.assertion(a_excess2[nd] >= minValues[nd]);
            if (checkNegativeRegions(a_excess2, minValues, gender, cutoff_age, recur, false))
                throw new Exception("внутренний сбой");

            if (Util.False)
                ChartXYSplineAdvanced.display2("Refinement " + gender.name(), a_excess, a_excess2);
        }

        PopulationContext p_new1941 = pmin.add(p_excess2);

        /* self-check */
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

        // showDifferences(p_new1941, p_start1941);

        if (Util.True)
        {
            for (Gender gender : Gender.TwoGenders)
            {
                double[] minValues = pmin.asArray(Locality.TOTAL, gender);
                minValues = Util.multiply(minValues, min_margin);

                // population excess over minimum
                double[] a_excess2 = p_excess2.asArray(Locality.TOTAL, gender);
                if (checkNegativeRegions(a_excess2, minValues, gender, cutoff_age, recur, false))
                    throw new Exception("внутренний сбой");
            }
        }

        if (Util.False)
            refine(p_new1941, true);
        
        Util.err("Отрицательные районы перераспределены внутри 5-летних групп");
        
        return p_new1941;
    }

    /* =============================================================================================== */

    private boolean checkNegativeRegions(double[] a, double[] minValues, Gender gender, int cutoff_age, boolean recur, boolean print)
            throws Exception
    {
        List<Region> list = negativeRegions(a, minValues, recur);

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
                Util.err(String.format("    %5d - %5d  [%5d] =  %7.3f - %7.3f  [нехватка %.0f]",
                                       r.nd1, r.nd2, r.nd2 - r.nd1 + 1,
                                       day2year(r.nd1), day2year(r.nd2), (double) Math.round(r.sum)));
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
        double sum;
    }

    public List<Region> negativeRegions(double[] a, double[] minValues, boolean recur) throws Exception
    {
        List<Region> list = new ArrayList<>();

        double margin = recur ? 0.999 : 1.0;

        for (int nd = 0; nd < a.length;)
        {
            // find first negative point
            while (nd < a.length && a[nd] >= margin * minValues[nd])
                nd++;
            if (nd == a.length)
                break;

            Region r = new Region();
            r.nd1 = nd;

            // find first positive point
            while (nd < a.length && a[nd] < margin * minValues[nd])
                nd++;
            r.nd2 = nd - 1;
            list.add(r);

            r.sum = Util.sum(Util.splice(a, r.nd1, r.nd2));
        }

        return list;
    }

    private double day2year(int nd)
    {
        return nd / 365.0;
    }

    /* =============================================================================================== */

    private boolean checkCanAdjust(RefineSeries rs, Gender gender, double[] a, double[] minValues, boolean strict, Bin[] bins)
    {
        for (Bin bin : bins)
        {
            if (!checkCanAdjust(rs, gender, a, minValues, strict, bin))
                return false;
        }
        
        return true;
    }

    private boolean checkCanAdjust(RefineSeries rs, Gender gender, double[] a, double[] minValues, boolean strict, Bin bin)
    {
        int nd1 = bin.age_x1;
        int nd2 = bin.age_x2;
        double avg = Util.average(Util.splice(a, nd1, nd2));

        double can_distribute = 0;
        double must_distribute = 0;

        for (int nd = nd1; nd <= nd2; nd++)
        {
            double minv = minValues[nd];
            minv = Math.max(minv, rs.minRelativeLevel * avg);
            if (a[nd] >= minv)
            {
                can_distribute += a[nd] - minv;
            }
            else
            {
                must_distribute += minv - a[nd];
            }
        }
        
        if (Util.False)
        {

            if (must_distribute == 0)
            {
                Util.out(String.format("%s %d-%d no-distr", gender.name(), nd1 / 365, nd2 / 365));
            }
            else
            {
                Util.out(String.format("%s %d-%d %f", gender.name(), nd1 / 365, nd2 / 365, can_distribute / must_distribute));
            }
        }

        if (strict)
        {
            Util.assertion(can_distribute >= must_distribute);
            return true;
        }
        else
        {
            return can_distribute >= must_distribute;
        }
    }

    /* =============================================================================================== */

    @SuppressWarnings("unused")
    private void showDifferences(PopulationContext p_new, PopulationContext p_start) throws Exception
    {
        Gender gender = Gender.MALE;
        for (int nd = 0; nd <= p_new.MAX_DAY; nd++)
        {
            double vn = p_new.getDay(Locality.TOTAL, gender, nd);
            double vs = p_start.getDay(Locality.TOTAL, gender, nd);
            if (Util.same(vn, vs))
                continue;
            double vdiff = vn - vs;

            Util.out(String.format("%s.%d.%d %f -> %f (diff: %f)", gender.name(), nd / 365, nd % 365, vs, vn, vdiff));
        }
    }
}
