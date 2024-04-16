package rtss.ww2losses.population_194x;

import org.apache.commons.lang3.mutable.MutableDouble;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.PopulationByLocality;
import rtss.data.population.forward.ForwardPopulationT;
import rtss.data.population.forward.PopulationForwardingContext;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.ww2losses.params.AreaParameters;

/**
 * Вычислить возрастную структуру и численность гипотетического населения СССР или РСФСР на начало 1946 года
 * в том случае, если бы рождаемость и смертность в 1941-1945 гг. оставались такими же, как в 1940 году 
 */
public class Expected_Population_In_Early_1946 extends UtilBase_194x
{
    private CombinedMortalityTable mt_ussr_1938 = new CombinedMortalityTable("mortality_tables/USSR/1938-1939");
    private CombinedMortalityTable mt_rsfsr_1940 = CombinedMortalityTable.loadTotal("mortality_tables/RSFSR/1940");
    private CombinedMortalityTable mt_ussr_1958 = new CombinedMortalityTable("mortality_tables/USSR/1958-1959");
    private AreaParameters ap;
    
    public Expected_Population_In_Early_1946(AreaParameters ap) throws Exception
    {
        this.ap = ap;
    }
    
    /*
     * Вычислить гипотетическое ожидаемое население СССР в начале 1946 г. 
     * используя госкомстатовкую таблицу смертности для СССР 1938-1939 гг.
     * 
     * @cbr указывает рождаемость
     * При значении CBR_1940 (36.1) число рождающихся добавляется в соответствии с уровнем рождаемости 1940 г.
     * При значении 0 рождения не добавляются. 
     */
    public PopulationByLocality with_mt_USSR_1938(double cbr, boolean interpolate_mt_to1958, MutableDouble births) throws Exception
    {
        return with_mt(mt_ussr_1938, cbr, interpolate_mt_to1958, births);
    }

    /*
     * Вычислить гипотетическое ожидаемое население СССР в начале 1946 г. 
     * используя таблицу смертности соответствующую возрастным уровням расчитанным АДХ для РСФСР 1940 г. 
     * 
     * @cbr указывает рождаемость
     * При значении CBR_1940 (36.1) число рождающихся добавляется в соответствии с уровнем рождаемости 1940 г.
     * При значении 0 рождения не добавляются. 
     */
    public PopulationByLocality with_mt_RSFSR_1940(double cbr, boolean interpolate_mt_to1958, MutableDouble births) throws Exception
    {
        return with_mt(mt_rsfsr_1940, cbr, interpolate_mt_to1958, births);
    }

    public PopulationByLocality with_mt(CombinedMortalityTable mt, double cbr, boolean interpolate_mt_to1958, MutableDouble births) throws Exception
    {
        PopulationForwardingContext fctx = new PopulationForwardingContext();
        PopulationByLocality p = new Population_In_Middle_1941(ap).evaluate(fctx);
        
        /* продвижка до начала 1942 года */
        p = forward(p, fctx, mt, cbr, 0.5);
        
        /* продвижка до начала 1943 года */
        p = forward(p, fctx, mt, cbr, 1.0);
        
        /* продвижка до начала 1944 года */
        CombinedMortalityTable xmt = tableForYear(mt, 1943, interpolate_mt_to1958);
        p = forward(p, fctx, xmt, cbr, 1.0);
        
        /* продвижка до начала 1945 года */
        xmt = tableForYear(mt, 1944, interpolate_mt_to1958);
        p = forward(p, fctx, xmt, cbr, 1.0);
        
        /* продвижка до начала 1946 года */
        xmt = tableForYear(mt, 1945, interpolate_mt_to1958);
        p = forward(p, fctx, xmt, cbr, 1.0);
        
        p = fctx.end(p);
        
        if (births != null)
            births.setValue(fctx.getTotalBirths(Locality.TOTAL, Gender.BOTH));
        
        return p;
    }

    private PopulationByLocality forward(PopulationByLocality p, PopulationForwardingContext fctx, CombinedMortalityTable mt, double cbr, double yfraction) throws Exception
    {
        ForwardPopulationT fw = new ForwardPopulationT().setBirthRateTotal(cbr);
        return fw.forward(p, fctx, mt, yfraction);
    }
    
    private CombinedMortalityTable tableForYear(CombinedMortalityTable mt1942, int year, boolean interpolate_mt_to1958) throws Exception
    {
        if (year <= 1942 || !interpolate_mt_to1958)
        {
            return mt1942;
        }
        else if (year >= 1958)
        {
            return mt_ussr_1958;
        }
        else
        {
            // interpolate between mt1942 in 1942 and mt1958 in 1958
            double weight = ((double)year - 1942) / (1958 - 1942);
            return CombinedMortalityTable.interpolate(mt1942, mt_ussr_1958, weight);
        }
    }
}
