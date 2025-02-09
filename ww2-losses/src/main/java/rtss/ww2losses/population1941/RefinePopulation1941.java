package rtss.ww2losses.population1941;

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
import rtss.ww2losses.population1941.math.RefineSeriesV3;
import rtss.ww2losses.population1941.math.RefineSeriesV4;

/* 
 * Перераспределить население внутри 5-летних групп аггреграции
 * для устаренения артефактов отрицательной величины потерь в 1941-1945 гг.
 * для некоторых возрастных линий.
 */
public class RefinePopulation1941
{
    final private AreaParameters ap;
    final PeacetimeMortalityTables peacetimeMortalityTables;
    final WarAttritionModel wam;
    final PopulationContext p1946_actual;

    public RefinePopulation1941(AreaParameters ap, PeacetimeMortalityTables peacetimeMortalityTables, WarAttritionModel wam,
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
         * Для каждой возрастной линии -- требуемое минимальное население, обеспечивающее достижение
         * населения в начале 1946 года при заданном (в данном случае нулевом) уровне потерь.
         */
        PopulationContext pmin = backtrack.population_1946_to_early1941(null);

        // population excess over minimum
        PopulationContext p_excess = p_start1941.sub(pmin, ValueConstraint.NONE);

        // check if need to adjust it
        boolean do_adjust = false;
        for (Gender gender : Gender.TwoGenders)
        {
            // population excess over minimum
            double[] a_excess = p_excess.asArray(Locality.TOTAL, gender);
            if (Util.min(a_excess) < 50)
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

            // redistribute the excess
            RefineSeriesV4 rs = new RefineSeriesV4();
            rs.minRelativeLevel = 0.2;
            rs.sigma = 10.0;
            rs.gaussianKernelWindow = 50;
            rs.sigmoidTransitionSteepness = 0.1; // ###
            double[] a_excess2 = rs.modifySeries(a_excess, PopulationADH.AgeBinWidthsDays(),  a_excess.length - 1);
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

}
