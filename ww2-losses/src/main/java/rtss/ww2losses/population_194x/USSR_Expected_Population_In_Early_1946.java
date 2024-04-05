package rtss.ww2losses.population_194x;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.PopulationByLocality;

/**
 * Вычислить возрастную структуру и численность гипотетического населения СССР на начало 1946 года
 * в том случае, если бы рождаемость и смертность в 1941-1945 гг. оставались такими же, как в 1940 году 
 */
public class USSR_Expected_Population_In_Early_1946
{
    /*
     * AДХ, "Население Советского Союза", стр. 120
     */
    public static final double CBR_1940 = 36.1;
    
    private static final int MAX_AGE = PopulationByLocality.MAX_AGE;
    private CombinedMortalityTable mt_ussr_1938 = new CombinedMortalityTable("mortality_tables/USSR/1938-1939");
    private CombinedMortalityTable mt_rsfsr_1940 = new CombinedMortalityTable("mortality_tables/RSFSR/1940");
    
    public USSR_Expected_Population_In_Early_1946() throws Exception
    {
    }
    
    /*
     * Вычислить население СССР в начале 1946 г. 
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
     * Вычислить население СССР в начале 1946 г. 
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

    /*
     * Вычислить население СССР в начале 1946 г. 
     * используя интерполируемую таблицу смертности. 
     * 
     * @cbr указывает рождаемость
     * При значении CBR_1940 (36.1) число рождающихся добавляется в соответствии с уровнем рождаемости 1940 г.
     * При значении 0 рождения не добавляются. 
     */
    public PopulationByLocality with_mt_interpolated(double cbr) throws Exception
    {
        return with_mt(null, cbr);
    }
    
    private PopulationByLocality with_mt(CombinedMortalityTable mt, double cbr) throws Exception
    {
        // ###
        return null;
    }
}
