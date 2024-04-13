package rtss.ww2losses.population_194x;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.PopulationByLocality;
import rtss.data.population.forward.ForwardPopulationT;
import rtss.data.population.forward.PopulationForwardingContext;

/**
 * Вычислить возрастную структуру и численность гипотетического населения СССР на начало 1946 года
 * в том случае, если бы рождаемость и смертность в 1941-1945 гг. оставались такими же, как в 1940 году 
 */
public class USSR_Expected_Population_In_Early_1946 extends UtilBase_194x
{
    private CombinedMortalityTable mt_ussr_1938 = new CombinedMortalityTable("mortality_tables/USSR/1938-1939");
    private CombinedMortalityTable mt_rsfsr_1940 = CombinedMortalityTable.loadTotal("mortality_tables/RSFSR/1940");
    
    public USSR_Expected_Population_In_Early_1946() throws Exception
    {
    }
    
    /*
     * Вычислить гипотетическое ожидаемое население СССР в начале 1946 г. 
     * используя госкомстатовкую таблицу смертности для СССР 1938-1939 гг.
     * 
     * @cbr указывает рождаемость
     * При значении CBR_1940 (36.1) число рождающихся добавляется в соответствии с уровнем рождаемости 1940 г.
     * При значении 0 рождения не добавляются. 
     */
    public PopulationByLocality with_mt_USSR_1938(double cbr) throws Exception
    {
        return with_mt(mt_ussr_1938, cbr);
    }

    /*
     * Вычислить гипотетическое ожидаемое население СССР в начале 1946 г. 
     * используя таблицу смертности соответствующую возрастным уровням расчитанным АДХ для РСФСР 1940 г. 
     * 
     * @cbr указывает рождаемость
     * При значении CBR_1940 (36.1) число рождающихся добавляется в соответствии с уровнем рождаемости 1940 г.
     * При значении 0 рождения не добавляются. 
     */
    public PopulationByLocality with_mt_RSFSR_1940(double cbr) throws Exception
    {
        return with_mt(mt_rsfsr_1940, cbr);
    }

    public PopulationByLocality with_mt(CombinedMortalityTable mt, double cbr) throws Exception
    {
        PopulationForwardingContext fctx = new PopulationForwardingContext();
        PopulationByLocality p = new USSR_Population_In_Middle_1941().evaluate(fctx);
        
        /* продвижка до начала 1942 года */
        p = forward(p, fctx, mt, cbr, 0.5);
        
        /* продвижка до начала 1943 года */
        p = forward(p, fctx, mt, cbr, 1.0);
        
        /* продвижка до начала 1944 года */
        p = forward(p, fctx, mt, cbr, 1.0);
        
        /* продвижка до начала 1945 года */
        p = forward(p, fctx, mt, cbr, 1.0);
        
        /* продвижка до начала 1946 года */
        p = forward(p, fctx, mt, cbr, 1.0);
        
        return fctx.end(p);
    }

    private PopulationByLocality forward(PopulationByLocality p, PopulationForwardingContext fctx, CombinedMortalityTable mt, double cbr, double yfraction) throws Exception
    {
        ForwardPopulationT fw = new ForwardPopulationT().setBirthRateTotal(cbr);
        return fw.forward(p, fctx, mt, yfraction);
    }
}
