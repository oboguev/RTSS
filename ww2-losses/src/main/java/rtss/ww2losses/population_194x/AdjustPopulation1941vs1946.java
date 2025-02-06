package rtss.ww2losses.population_194x;

import rtss.data.population.struct.PopulationContext;
import rtss.ww2losses.ageline.warmodel.WarAttritionModelParameters;
import rtss.ww2losses.helpers.PeacetimeMortalityTables;

public class AdjustPopulation1941vs1946
{
    private final PopulationContext p1946_actual;
    private final PeacetimeMortalityTables peacetimeMortalityTables;
    private final WarAttritionModelParameters wamp;

    public AdjustPopulation1941vs1946(final PopulationContext p_start1941,
            final PopulationContext p1946_actual,
            final PeacetimeMortalityTables peacetimeMortalityTables,
            final WarAttritionModelParameters wamp)
    {
        this.p1946_actual = p1946_actual;
        this.peacetimeMortalityTables = peacetimeMortalityTables;
        this.wamp = wamp;
    }

    public static PopulationContext adjust(final PopulationContext p_start1941) throws Exception
    {

        // ### передвижка до середины 1941
        // ### WarAttritionModel wam =
        // ### BacktrackPopulation

        // ### for 5-year groups redistribute via diffusion?
        // ### must_reduce
        // ### may_increase

        // ### repeat

        return null;
    }
}
