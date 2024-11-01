package rtss.mexico.agri.entities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtss.mexico.agri.loader.LoadCultureDefinitions;
import rtss.util.Util;

/*
 * Сельскохозяйственная культура
 */
public class Culture
{
    public final String name;
    public final String category;

    private final Map<Integer, CultureYear> y2cy = new HashMap<>();

    public final Map<String, CultureYear> averageCultureYears = new HashMap<>();

    public Culture(String name, String category) throws Exception
    {
        CultureDefinition cd = LoadCultureDefinitions.load().get(name);
        if (cd == null)
            throw new Exception("Неопределённая культура: " + name);
        this.name = cd.name;
        this.category = category;
    }

    public List<Integer> years()
    {
        return Util.sort(y2cy.keySet());
    }

    public CultureYear cultureYear(int year)
    {
        return y2cy.get(year);
    }

    public CultureYear makeCultureYear(int year) throws Exception
    {
        if (y2cy.containsKey(year))
            throw new Exception("Duplicate year " + year + " for " + name);
        CultureYear cy = new CultureYear(this, year);
        y2cy.put(year, cy);
        return cy;
    }

    public CultureYear makeAverageCultureYear(String comment) throws Exception
    {
        if (averageCultureYears.containsKey(comment))
            throw new Exception("Duplicate average year " + comment + " for " + name);
        CultureYear cy = new CultureYear(this, comment);
        averageCultureYears.put(comment, cy);
        return cy;
    }

    public void deleteYear(CultureYear cy)
    {
        y2cy.remove(cy.year);
        averageCultureYears.remove(cy.comment);
    }
    
    public String id()
    {
        if (category != null)
            return category + "/" + name;
        else
            return name;
    }
}
