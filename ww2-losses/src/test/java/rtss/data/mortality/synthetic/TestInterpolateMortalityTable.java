package rtss.data.mortality.synthetic;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.PopulationByLocality;
import rtss.data.population.forward.PopulationContext;
import rtss.data.selectors.Area;
import rtss.util.Util;
import rtss.ww2losses.params.AreaParameters;
import rtss.ww2losses.population_194x.Population_In_Early_1940;

public class TestInterpolateMortalityTable
{
    public static void main(String[] args)
    {
        try
        {
            CombinedMortalityTable mt1 = CombinedMortalityTable.load("mortality_tables/USSR/1938-1939");
            mt1.comment("ГКС-СССР-1938");

            CombinedMortalityTable mt2 = MortalityTableADH.getMortalityTable(Area.RSFSR, 1940);
            mt2.comment("АДХ-РСФСР-1940");
            
            AreaParameters ap = AreaParameters.forArea(Area.USSR);
            PopulationContext fctx = new PopulationContext();
            PopulationByLocality p1940 = new Population_In_Early_1940(ap).evaluate(fctx);

            CombinedMortalityTable cmt = InterpolateMortalityTable.forTargetRates(
                    mt1,
                    mt2,
                    p1940,
                    fctx,
                    ap.CBR_1940,
                    ap.CDR_1940,
                    4,
                    null);
            
            Util.unused(cmt);
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
        }
    }
}
