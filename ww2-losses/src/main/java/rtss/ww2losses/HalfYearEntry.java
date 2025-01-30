package rtss.ww2losses;

import rtss.data.ValueConstraint;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.struct.PopulationContext;
import rtss.ww2losses.HalfYearEntries.HalfYearSelector;

public class HalfYearEntry
{
    /* предыдущий и следующий периоды */
    public HalfYearEntry prev;
    public HalfYearEntry next;
    
    /* обозначение периода (год и полугодие) */
    final public int year;
    final public HalfYearSelector halfyear;
    
    /* таблица смертности для этого полугодия в условиях мира */
    CombinedMortalityTable peace_mt;
    /* кривые l(x) с разрешением 1 день возраста для условий мира */
    public double[] peace_lx_male;
    public double[] peace_lx_female;
    
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
    
    /* действительное число смертей (по возрастам и полам) в этом полугодии */
    public PopulationContext actual_deaths = newPopulationContext();
    public PopulationContext actual_peace_deaths = newPopulationContext();
    public PopulationContext actual_excess_wartime_deaths = newPopulationContext();

    /* действительное население (по возрастам и полам) в начале полугодия */
    public PopulationContext actual_population = newPopulationContext();
    
    /* действительное число рождений в полугодии */
    public double actual_births; 
    
    /* 
     * число смертей в данном полугодии от фактического рождений во время войны
     * ожидаемое при смертности мирного времени
     */
    public double actual_warborn_deaths_baseline; 

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
        PopulationContext p = new PopulationContext(PopulationContext.ALL_AGES);
        p.setValueConstraint(ValueConstraint.NONE);
        p.beginTotal();
        return p;
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
    
    // заменить пробелы на неразбивающий пробел, 
    // для удобства импорта распечатанной таблицы в Excel 
    private String nbsp(String s)
    {
        final char nbsp = (char) 0xA0;
        return s.replace(' ', nbsp);
    }
}
