package rtss.ww2losses.population_194x;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.calc.RescalePopulation;
import rtss.data.population.forward.ForwardPopulationT;
import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.population.struct.PopulationContext;
import rtss.data.population.synthetic.PopulationADH;
import rtss.data.selectors.Area;
import rtss.data.selectors.Locality;
import rtss.ww2losses.params.AreaParameters;

/**
 * Вычислить возрастную структуру населения СССР или РСФСР на середину 1941 года
 */
public class Population_In_Middle_1941 extends UtilBase_194x
{
    private final AreaParameters ap;
    private final AdjustPopulation adjuster1941;
    
    public PopulationByLocality p_start_1941;
    public double observed_births_1941_1st_halfyear;
    public double observed_deaths_1941_1st_halfyear;
    public PopulationContext observed_deaths_1941_1st_halfyear_byGenderAge;
    
    public Population_In_Middle_1941(AreaParameters ap)
    {
        this(ap, null);
    }

    public Population_In_Middle_1941(AreaParameters ap, AdjustPopulation adjuster1941)
    {
        this.ap = ap;
        this.adjuster1941 = adjuster1941;
    }

    public PopulationByLocality evaluate() throws Exception
    {
        PopulationContext fctx = new PopulationContext();
        PopulationByLocality p = evaluate(fctx);
        return fctx.end(p);
    }
    
    public Population evaluateAsPopulation() throws Exception
    {
        return evaluate().forLocality(Locality.TOTAL);
    }

    /*
     * Оставляет контекст незакрытым, позволяя дальнейшую передвижку
     */
    public PopulationByLocality evaluate(PopulationContext fctx) throws Exception
    {
        CombinedMortalityTable mt1940 = new MortalityTable_1940(ap).evaluate();
        return evaluate(fctx, mt1940);
    }
    
    public Population evaluateAsPopulation(PopulationContext fctx) throws Exception
    {
        return evaluate(fctx).forLocality(Locality.TOTAL);
    }

    public PopulationByLocality evaluate(PopulationContext fctx, CombinedMortalityTable mt1940) throws Exception
    {
        ForwardPopulationT fw = new ForwardPopulationT();
        PopulationByLocality p;
        
        if (useADH)
        {
            p = PopulationADH.getPopulationByLocality(ap.area, 1941);
            if (adjuster1941 != null)
                p = adjuster1941.adjust(p);
            p_start_1941 = p.clone();
            p = fctx.begin(p);
        }
        else
        {
            p = new Population_In_Early_1940(ap).evaluate(fctx);
            
            /*
             * Передвижка с начала 1940 до начала 1941 года
             */
            fw.setBirthRateTotal(ap.CBR_1940);
            p = fw.forward(p, fctx, mt1940, 1.0);
            show_struct("начало 1941", p, fctx);
            p_start_1941 = fctx.end(p);
        }
        
        /*
         * Передвижка с начала 1941 до середины 1941 года
         */
        fw = new ForwardPopulationT();
        fw.setBirthRateTotal(ap.CBR_1940);
        p = fw.forward(p, fctx, mt1940, 0.5);
        
        observed_births_1941_1st_halfyear = fw.getObservedBirths();
        observed_deaths_1941_1st_halfyear = fw.getObservedDeaths();
        observed_deaths_1941_1st_halfyear_byGenderAge = fw.deathsByGenderAge();
        
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

    public Population evaluateAsPopulation(PopulationContext fctx, CombinedMortalityTable mt1940) throws Exception
    {
        return evaluate(fctx, mt1940).forLocality(Locality.TOTAL);
    }
}
