package rtss.ww2losses.population1941;

import rtss.data.ValueConstraint;
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
import rtss.ww2losses.population1941.math.RefineSeriesV3;

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
        PopulationContext p = backtrack.population_1946_to_early1941(null);
        p = p_start1941.sub(p, ValueConstraint.NONE);
        // p.display("Разница 1941");
        Util.noop();

        for (Gender gender : Gender.TwoGenders)
        {
            double[] y = p.asArray(Locality.TOTAL, gender);
            double minRelativeLevel = 0.2;
            double smoohingSigma = 10.0;
            double[] y2 = RefineSeriesV3.modifySeries(y, PopulationADH.AgeBinWidthsDays(), y.length, minRelativeLevel, smoohingSigma);
            ChartXYSplineAdvanced.display2("Refinement " + gender.name(), y, y2);
            Util.noop();
        }

        // ###

        return null;
    }

}
