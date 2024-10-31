package rtss.mexico.agri.entities;

/*
 * Годовые даные для чсельскохозяйственной культуры
 */
public class CultureYear
{
    public final Culture culture;
    public final int year;
    public final String comment;
    public Rice rice_kind;

    // уборочнная площадь, га
    public Double surface;

    // урожайность, кг/га
    public Double yield;

    // урожай, тонн
    public Double production;
    public Double production_raw;

    // импорт, тонн
    public Double importAmount;

    // экспорт, тонн
    public Double exportAmount;

    // национальное потребление, тонн
    public Double consumption;

    // душевой потребление за год, кг на душу (при численности населения по таблице SARH)
    public Double perCapita;

    // производство алкоголя, литров
    public Double alcohol;

    public CultureYear(Culture culture, int year)
    {
        this.culture = culture;
        this.year = year;
        this.comment = null;
    }

    public CultureYear(Culture culture, String comment)
    {
        this.culture = culture;
        this.year = -1;
        this.comment = comment;
    }

    public boolean isAllNull()
    {
        return surface == null && yield == null && production == null &&
               importAmount == null && exportAmount == null &&
               consumption == null && perCapita == null && alcohol == null;
    }

    public String idYear()
    {
        if (comment != null)
            return comment;
        else
            return String.format("%d", year);
    }

    public Double get(String what) throws Exception
    {
        switch (what)
        {
        case "площадь":
            return this.surface;
        case "урожай":
        case "производство":
            return this.production;
        case "урожайность":
            return this.yield;
        case "экспорт":
            return this.exportAmount;
        case "импорт":
            return this.importAmount;
        case "национальное потребление":
            return this.consumption;
        case "душевое потребление":
            return this.perCapita;
        case "алкоголь":
            return this.alcohol;
        default:
            throw new Exception("Incorrect selector");
        }
    }
}
