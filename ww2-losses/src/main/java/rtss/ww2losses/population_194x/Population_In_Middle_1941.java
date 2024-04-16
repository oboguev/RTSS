package rtss.ww2losses.population_194x;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.PopulationByLocality;
import rtss.data.population.RescalePopulation;
import rtss.data.population.forward.ForwardPopulationT;
import rtss.data.population.forward.PopulationForwardingContext;
import rtss.data.population.synthetic.PopulationADH;
import rtss.data.selectors.Area;
import rtss.ww2losses.params.AreaParameters;

/**
 * Вычислить возрастную структуру населения СССР или РСФСР на середину 1941 года
 */
public class Population_In_Middle_1941 extends UtilBase_194x
{
    private AreaParameters ap;
    
    public Population_In_Middle_1941(AreaParameters ap)
    {
        this.ap = ap;
    }

    public PopulationByLocality evaluate() throws Exception
    {
        PopulationForwardingContext fctx = new PopulationForwardingContext();
        PopulationByLocality p = evaluate(fctx);
        return fctx.end(p);
    }
    
    /*
     * Оставляет контекст незакрытым, позволяя дальнейшую продвижку
     */
    public PopulationByLocality evaluate(PopulationForwardingContext fctx) throws Exception
    {
        ForwardPopulationT fw = new ForwardPopulationT();
        CombinedMortalityTable mt1940 = new MortalityTable_1940(ap).evaluate();
        PopulationByLocality p;
        
        if (useADH)
        {
            p = PopulationADH.getPopulationByLocality(ap.area, 1941);
            p = fctx.begin(p);
        }
        else
        {
            p = new Population_In_Early_1940(ap).evaluate(fctx);
            
            /*
             * Продвижка с начала 1940 до начала 1941 года
             */
            fw.setBirthRateTotal(ap.CBR_1940);
            p = fw.forward(p, fctx, mt1940, 1.0);
            show_struct("начало 1941", p, fctx);
        }
        
        /*
         * Продвижка с начала 1941 до середины 1941 года
         */
        fw.setBirthRateTotal(ap.CBR_1940);
        p = fw.forward(p, fctx, mt1940, 0.5);
        
        /*
         * Перемасштабировать для точного совпадения общей численности полов с расчётом АДХ
         */
        if (ap.area == Area.USSR)
        {
            final double USSR_1941_START = 195_392_000; // АДХ, "Население Советского Союза", стр. 77, 118, 126
            final double USSR_1941_MID = forward_6mo(USSR_1941_START, AreaParameters.forArea(Area.USSR).growth_1940());

            /* АДХ, "Население Советского Союза", стр. 56, 74 */
            final double males_jun21 = 94_338_000; 
            final double females_jun21 = 102_378_000;
            final double total_jun21 = males_jun21 + females_jun21;
            
            final double females_mid1941 = females_jun21 * USSR_1941_MID / total_jun21; 
            final double males_mid1941 = males_jun21 * USSR_1941_MID / total_jun21; 
            
            p = RescalePopulation.scaleTotal(p, fctx, males_mid1941, females_mid1941);
        }

        p.validate();

        return p;
    }
}
