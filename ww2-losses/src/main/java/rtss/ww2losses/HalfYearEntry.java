package rtss.ww2losses;

import rtss.data.population.PopulationByLocality;
import rtss.ww2losses.util.HalfYearEntries.HalfYearSelector;

public class HalfYearEntry
{
    /* предыдущий и следующий периоды */
    public HalfYearEntry prev;
    public HalfYearEntry next;
    
    /* обозначение периода (год и полугодие) */
    final public int year;
    final public HalfYearSelector halfyear;
    
    /* ожидаемое в условиях мира население на начало периода, с учётом рождений после середины 1941 */
    final public PopulationByLocality p_nonwar_with_births;

    /* ожидаемое в условиях мира население на начало периода, без учёта рождений после середины 1941 */
    final public PopulationByLocality p_nonwar_without_births;
    
    /* ожидаемое в условиях мира число смертей за период (от начала до конца периода) */
    public double expected_nonwar_deaths;

    /* ожидаемое в условиях мира число рождений за период (от начала до конца периода) */
    public double expected_nonwar_births;

    public HalfYearEntry(
            int year,
            HalfYearSelector halfyear,
            PopulationByLocality p_nonwar_with_births,
            PopulationByLocality p_nonwar_without_births)
    {
        this.year = year;
        this.halfyear = halfyear;
        this.p_nonwar_with_births = p_nonwar_with_births;
        this.p_nonwar_without_births = p_nonwar_without_births;
    }
    
    public String toString()
    {
        switch (halfyear)
        {
        case FirstHalfYear:
            return year + ", первое полугодие";
            
        case SecondHalfYear:
            return year + ", второе полугодие";
            
        default:
            return "неопределённая дата";
        }
    }
}
