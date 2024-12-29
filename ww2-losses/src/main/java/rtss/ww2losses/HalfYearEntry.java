package rtss.ww2losses;

import rtss.data.population.PopulationByLocality;

public class HalfYearEntry
{
    public HalfYearEntry prev;
    public HalfYearEntry next;
    
    final public int year;
    final public int halfyear;
    final public PopulationByLocality p_nowar_with_births;
    final public PopulationByLocality p_nowar_without_births;

    public HalfYearEntry(
            int year,
            int halfyear,
            PopulationByLocality p_nowar_with_births,
            PopulationByLocality p_nowar_without_births)
    {
        this.year = year;
        this.halfyear = halfyear;
        this.p_nowar_with_births = p_nowar_with_births;
        this.p_nowar_without_births = p_nowar_without_births;
    }
}
