package rtss.ww2losses.struct;

import rtss.data.ValueConstraint;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.struct.PopulationContext;
import rtss.ww2losses.struct.HalfYearEntries.HalfYearSelector;

public class HalfYearEntry
{
    /* предыдущий и следующий периоды */
    public HalfYearEntry prev;
    public HalfYearEntry next;

    /* обозначение периода (год и полугодие) */
    final public int year;
    final public HalfYearSelector halfyear;

    /* таблица смертности для этого полугодия в условиях мира */
    public CombinedMortalityTable peace_mt;

    /* кривые l(x) с разрешением 1 день возраста для условий мира */
    public double[] peace_lx_male;
    public double[] peace_lx_female;

    /** ============================ Ожидаемое движение в условияaх мира ============================ **/

    /* ожидаемое в условиях мира население на начало периода, с учётом рождений после середины 1941 */
    public PopulationContext p_nonwar_with_births;

    /* ожидаемое в условиях мира население на начало периода, без учёта рождений после середины 1941 */
    public PopulationContext p_nonwar_without_births;

    /* 
     * ожидаемое в условиях мира число смертей за период (от начала до конца периода) 
     * в наличном на начало войны населении
     */
    public double expected_nonwar_deaths;

    /* ожидаемое в условиях мира число рождений за период (от начала до конца периода) */
    public double expected_nonwar_births;
    
    /* ожидаемое в условиях мира ежедневное число рождений за период (от начала до конца периода) */
    public double[] expected_nonwar_births_byday;

    /** ============================ Иммиграция (в РСФСР) ============================ **/

    /* число переселенцев в РСФСР (по возрастам и полам) в этом полугодии, для СССР всегда нуль */
    public PopulationContext immigration = newPopulationContext();

    /** ============================ Фактические смерти ============================ **/

    /* действительное общее число смертей (по возрастам и полам) в этом полугодии */
    public PopulationContext actual_deaths = newPopulationContext();

    /* 
     * часть действительного числа смертей (по возрастам и полам) в этом полугодии, 
     * состоявшихся бы в действительном военном населении при смертности мирного времени
     */
    public PopulationContext actual_peace_deaths = newPopulationContext();

    /* 
     * смерти состоявшееся бы от рождённых в военное время если бы они умирали 
     * по смертности мирного времени; промежуточный элемент, в конце расчёта
     * уже прибавлен к actual_peace_deaths 
     */
    public PopulationContext actual_peace_deaths_from_newborn = newPopulationContext();

    /* 
     * часть действительного числа смертей (по возрастам и полам) в этом полугодии, 
     * состоявшихся в действительном военном населении избыточно к смертности мирного времени
     */
    public PopulationContext actual_excess_wartime_deaths = newPopulationContext();

    /** ============================ Фактическое население ============================ **/

    /* 
     * действительное население (по возрастам и полам) в начале полугодия
     * включая начальное на середину 1941 и рождённое после
     * 
     * Для начала 1941 -- население на начало 1941 по АДХ
     * Для середины 1941 -- население передвинутое от начала 1941
     */
    public PopulationContext actual_population = newPopulationContext();

    /* 
     * действительное население (по возрастам и полам) в начале полугодия
     * включая только начальное на середину 1941 года
     */
    public PopulationContext actual_population_without_births = newPopulationContext();
    
    /*
     * остаток на начало полугодия от родившихся с середины 1941 года
     * при фактических условиях военной смертности
     */
    public PopulationContext wartime_born_remainder_UnderActualWartimeChildMortality;

    /*
     * остаток на начало полугодия от родившихся с середины 1941 года
     * при условии, что детская смертность имела бы значения мирного времени
     */
    public PopulationContext wartime_born_remainder_UnderPeacetimeChildMortality;

    /** ============================ Фактические рождения ============================ **/

    /* действительное число рождений в полугодии */
    public double actual_births;

    /* 
     * число смертей в данном полугодии от фактического рождений во время войны
     * ожидаемое при смертности мирного времени
     */
    public double actual_warborn_deaths_baseline;

    /* 
     * фактическое число смертей в данном полугодии от фактических рождений во время войны
     * (рождений состоявшихся как в текущем, так и в предыдуших военных полугодиях)
     * при фактической смертности военного времени
     */
    public double actual_warborn_deaths;

    /** ============================================================================== **/

    public HalfYearEntry(
            int year,
            HalfYearSelector halfyear,
            PopulationContext p_nonwar_with_births,
            PopulationContext p_nonwar_without_births)
    {
        this.year = year;
        this.halfyear = halfyear;
        this.p_nonwar_with_births = p_nonwar_with_births;
        this.p_nonwar_without_births = p_nonwar_without_births;
    }

    private PopulationContext newPopulationContext()
    {
        return PopulationContext.newTotalPopulationContext(ValueConstraint.NONE);
    }

    public String toString()
    {
        switch (halfyear)
        {
        case FirstHalfYear:
            return nbsp(year + " первое полугодие");

        case SecondHalfYear:
            return nbsp(year + " второе полугодие");

        default:
            return nbsp("неопределённая дата");
        }
    }
    
    public String id()
    {
        return id(year, halfyear);
    }

    public static String id(int year, HalfYearSelector halfyear)
    {
        return year + "." + halfyear;
    }

    // заменить пробелы на неразбивающий пробел, 
    // для удобства импорта распечатанной таблицы в Excel 
    private String nbsp(String s)
    {
        final char nbsp = (char) 0xA0;
        return s.replace(' ', nbsp);
    }
    
    /* смещение относительно начала начала 1941, в годах */  
    public double offset_start1941() throws Exception
    {
        double v = (year - 1941) * 2 + halfyear.seq(0);
        return v / 2;
    }
    
    /* индекс в массиве полугодий 1941.1 = 0*/
    public int index() throws Exception
    {
        return index(year, halfyear);
    }

    /* индекс в массиве полугодий 1941.1 = 0*/
    public static int index(int year, HalfYearSelector halfyear) throws Exception
    {
        return (year - 1941) * 2 + halfyear.seq(0);
    }
}
