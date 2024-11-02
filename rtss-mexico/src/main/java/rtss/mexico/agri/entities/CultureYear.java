package rtss.mexico.agri.entities;

/*
 * Годовые даные для чсельскохозяйственной культуры
 */
public class CultureYear
{
    public final Culture culture;
    public final int year;
    public final String comment;
    public RiceKind rice_kind;

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

    private CultureYear(Culture culture, int year, String comment)
    {
        this.culture = culture;
        this.year = year;
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

    public void copyValues(CultureYear cy) throws Exception
    {
        this.rice_kind = cy.rice_kind;
        this.surface = cy.surface;
        this.yield = cy.yield;
        this.production = cy.production;
        this.production_raw = cy.production_raw;
        this.importAmount = cy.importAmount;
        this.exportAmount = cy.exportAmount;
        this.consumption = cy.consumption;
        this.perCapita = cy.perCapita;
        this.alcohol = cy.alcohol;
    }

    public void addValues(CultureYear cy) throws Exception
    {
        this.surface = add(this.surface, cy.surface);
        this.production = add(this.production, cy.production);
        this.production_raw = add(this.production_raw, cy.production_raw);
        this.importAmount = add(this.importAmount, cy.importAmount);
        this.exportAmount = add(this.exportAmount, cy.exportAmount);
        this.consumption = add(this.consumption, cy.consumption);
        this.alcohol = add(this.alcohol, cy.alcohol);

        this.yield = null;
        this.perCapita = null;
    }

    private Double add(Double v1, Double v2) throws Exception
    {
        if (v1 == null && v2 == null)
            return null;
        else if (v1 != null && v2 != null)
            return v1 + v2;
        else
            throw new Exception("Unable to add year values for the culture");
    }
    
    public CultureYear dup(Culture culture)
    {
        CultureYear cy = new CultureYear(culture, this.year, this.comment);

        cy.rice_kind = this.rice_kind;
        cy.surface = this.surface;
        cy.yield = this.yield;
        cy.production = this.production;
        cy.production_raw = this.production_raw;
        cy.importAmount = this.importAmount;
        cy.exportAmount = this.exportAmount;
        cy.consumption = this.consumption;
        cy.perCapita = this.perCapita;
        cy.alcohol = this.alcohol;

        return cy;
    }
}
