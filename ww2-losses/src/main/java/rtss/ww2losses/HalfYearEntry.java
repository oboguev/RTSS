package rtss.ww2losses;

import rtss.data.population.PopulationByLocality;
import rtss.data.population.forward.PopulationForwardingContext;
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
    public PopulationByLocality p_nonwar_with_births;

    /* ожидаемое в условиях мира население на начало периода, без учёта рождений после середины 1941 */
    public PopulationByLocality p_nonwar_without_births;
    
    /* 
     * ожидаемое в условиях мира число смертей за период (от начала до конца периода) 
     * в наличном на начало войны населении
     */
    public double expected_nonwar_deaths;

    /*ожидаемое в условиях мира число рождений за период (от начала до конца периода) */
    public double expected_nonwar_births;
    
    /* кумулятивные потери населения по сверхсмертности накопленные на начало периода, половозрастная структура */
    public PopulationByLocality accumulated_excess_deaths;
    
    /*
     * добавочное из-за войны количество смертей в этом полугодии (включает excess_deaths_fertile_f),
     * вычисляется независимо от accumulated_excess_deaths; разностное исчисление по accumulated_excess_deaths
     * даёт более точную величину  
     */
    public double excess_war_deaths = 0;

    /* 
     * добавочное из-за войны количество смертей в этом полугодии среди женщин фертильного возраста,
     * вычисляется независимо от accumulated_excess_deaths; разностное исчисление по accumulated_excess_deaths 
     * даёт более точную величину  
     */
    public double excess_war_deaths_fertile_f = 0;
    
    /* фактическое число состоявшихся рождений */
    public double actual_births; 
    
    /* временные данные для передвижки */
    PopulationByLocality fw_p_wb;
    PopulationForwardingContext fw_fctx_wb;
    PopulationByLocality fw_p_xb;
    PopulationForwardingContext fw_fctx_xb;

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
    
    public void save_fw(
            PopulationByLocality pwb, PopulationForwardingContext fctx, 
            PopulationByLocality pxb, PopulationForwardingContext fctx_xb)
    {
        this.fw_p_wb = pwb.clone();
        this.fw_fctx_wb = fctx.clone();

        this.fw_p_xb = pxb.clone();
        this.fw_fctx_xb = fctx_xb.clone();            
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
