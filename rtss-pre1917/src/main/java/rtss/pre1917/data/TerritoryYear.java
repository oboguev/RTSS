package rtss.pre1917.data;

import rtss.util.Util;

public class TerritoryYear
{
    public TerritoryYear(Territory territory, int year)
    {
        this.territory = territory;
        this.year = year;
    }

    public Territory territory;
    public final int year;

    /*
     * Рождаемость, смертность и темп естественного роста.
     * null = no data
     */
    public Double cbr;
    public Double cdr;
    public Double ngr;

    /*
     * Население, число рождений и число смертей 
     * null = no data
     */
    public Long population;
    public Long births;
    public Long deaths;

    public Long population_urban_male;
    public Long population_urban_female;
    public Long population_rural_male;
    public Long population_rural_female;

    public Long births_urban_male;
    public Long births_urban_female;
    public Long births_rural_male;
    public Long births_rural_female;

    public Long deaths_urban_male;
    public Long deaths_urban_female;
    public Long deaths_rural_male;
    public Long deaths_rural_female;

    public void setValue(String what, Double v) throws Exception
    {
        interceptSetValue(what);

        switch (what)
        {
        case "р":
            if (cbr != null)
                throw new Exception("Duplicate value");
            cbr = v;
            break;

        case "с":
            if (cdr != null)
                throw new Exception("Duplicate value");
            cdr = v;
            break;

        case "п":
            if (ngr != null)
                throw new Exception("Duplicate value");
            ngr = v;
            break;

        default:
            throw new Exception("Invalid selector");
        }
    }

    public void setValue(String what, Long v) throws Exception
    {
        interceptSetValue(what);

        switch (what)
        {
        case "чж":
            if (population != null)
                duplicateValue(what);
            population = v;
            break;

        case "чр":
            if (births != null)
                duplicateValue(what);
            births = v;
            break;

        case "чу":
            if (deaths != null)
                duplicateValue(what);
            deaths = v;
            break;

        case "чж-гор-м ":
            if (population_urban_male != null)
                duplicateValue(what);
            population_urban_male = v;
            break;

        case "чж-гор-ж ":
            if (population_urban_female != null)
                duplicateValue(what);
            population_urban_female = v;
            break;

        case "чж-уез-м ":
            if (population_rural_male != null)
                duplicateValue(what);
            population_rural_male = v;
            break;

        case "чж-уез-ж ":
            if (population_rural_female != null)
                duplicateValue(what);
            population_rural_female = v;
            break;

        case "чр-гор-м ":
            if (births_urban_male != null)
                duplicateValue(what);
            births_urban_male = v;
            break;

        case "чр-гор-ж ":
            if (births_urban_female != null)
                duplicateValue(what);
            births_urban_female = v;
            break;

        case "чр-уез-м ":
            if (births_rural_male != null)
                duplicateValue(what);
            births_rural_male = v;
            break;

        case "чр-уез-ж ":
            if (births_rural_female != null)
                duplicateValue(what);
            births_rural_female = v;
            break;

        case "чс-гор-м ":
            if (deaths_urban_male != null)
                duplicateValue(what);
            deaths_urban_male = v;
            break;

        case "чс-гор-ж ":
            if (deaths_urban_female != null)
                duplicateValue(what);
            deaths_urban_female = v;
            break;

        case "чс-уез-м ":
            if (deaths_rural_male != null)
                duplicateValue(what);
            deaths_rural_male = v;
            break;

        case "чс-уез-ж ":
            if (deaths_rural_female != null)
                duplicateValue(what);
            deaths_rural_female = v;
            break;

        default:
            throw new Exception("Invalid selector");
        }
    }

    private void duplicateValue(String what) throws Exception
    {
        throw new Exception(String.format("Duplicate value %s for %s %d", what, territory.name, year));
    }

    private void interceptSetValue(String what)
    {
        if (Util.True)
        {
            if (year == 1897 && what.equals("чж") && territory.name.equals("Область войска Донского"))
                Util.noop();
        }
    }

    public TerritoryYear dup()
    {
        TerritoryYear ty = new TerritoryYear(this.territory, this.year);
        ty.cbr = this.cbr;
        ty.cdr = this.cdr;
        ty.ngr = this.ngr;
        ty.population = this.population;
        ty.births = this.births;
        ty.deaths = this.deaths;
        return ty;
    }
}
