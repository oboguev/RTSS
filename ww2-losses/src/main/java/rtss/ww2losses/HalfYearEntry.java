package rtss.ww2losses;

import rtss.ww2losses.HalfYearEntries.HalfYearSelector;

public class HalfYearEntry
{
    /* предыдущий и следующий периоды */
    public HalfYearEntry prev;
    public HalfYearEntry next;
    
    /* обозначение периода (год и полугодие) */
    final public int year;
    final public HalfYearSelector halfyear;

    public HalfYearEntry(
            int year,
            HalfYearSelector halfyear)
    {
        this.year = year;
        this.halfyear = halfyear;
    }

    public String toString()
    {
        switch (halfyear)
        {
        case FirstHalfYear:
            return nbsp(year + ", первое полугодие");
            
        case SecondHalfYear:
            return nbsp(year + ", второе полугодие");
            
        default:
            return nbsp("неопределённая дата");
        }
    }
    
    // заменить пробелы на неразбивающий пробел, 
    // для удобства импорта таблицы в Excel 
    private String nbsp(String s)
    {
        final char nbsp = (char) 0xA0;
        return s.replace(' ', nbsp);
    }
}
