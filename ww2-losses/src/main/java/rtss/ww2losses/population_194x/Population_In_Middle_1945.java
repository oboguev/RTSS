package rtss.ww2losses.population_194x;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.Population;
import rtss.data.population.PopulationByLocality;
import rtss.data.population.forward.BackwardPopulationT;
import rtss.data.population.forward.PopulationForwardingContext;
import rtss.data.population.synthetic.PopulationADH;
import rtss.data.selectors.Locality;
import rtss.ww2losses.params.AreaParameters;

/**
 * Вычислить половозрастную структуру населения СССР или РСФСР на середину 1945 года
 */
public class Population_In_Middle_1945 extends UtilBase_194x
{
    private AreaParameters ap;

    public PopulationByLocality p_start_1946;
    public double observed_deaths_1945_2nd_halfyear;

    public Population_In_Middle_1945(AreaParameters ap)
    {
        this.ap = ap;
    }

    public PopulationByLocality evaluate(CombinedMortalityTable mt1945) throws Exception
    {
        PopulationForwardingContext fctx = new PopulationForwardingContext();
        PopulationByLocality p = evaluate(fctx, mt1945);
        return fctx.end(p);
    }

    public Population evaluateAsPopulation(CombinedMortalityTable mt1945) throws Exception
    {
        return evaluate(mt1945).forLocality(Locality.TOTAL);
    }

    /*
     * Оставляет контекст незакрытым, позволяя дальнейшую передвижку
     */
    public PopulationByLocality evaluate(PopulationForwardingContext fctx, CombinedMortalityTable mt1945) throws Exception
    {
        PopulationByLocality p;

        p = PopulationADH.getPopulationByLocality(ap.area, 1941);
        p_start_1946 = p.clone();
        p = fctx.begin(p);

        /*
         * Передвижка с начала 1941 до середины 1941 года
         */
        BackwardPopulationT bw = new BackwardPopulationT();
        p = bw.backward(p, fctx, mt1945, 0.5);
        observed_deaths_1945_2nd_halfyear = bw.getObservedDeaths();

        p.validate();

        return p;
    }

    public Population evaluateAsPopulation(PopulationForwardingContext fctx, CombinedMortalityTable mt1945) throws Exception
    {
        return evaluate(fctx, mt1945).forLocality(Locality.TOTAL);
    }
}
