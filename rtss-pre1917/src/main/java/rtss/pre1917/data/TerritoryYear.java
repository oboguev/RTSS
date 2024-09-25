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
    public URValue population = new URValue();
    public URValue midyear_population = new URValue();
    public URValue births = new URValue();
    public URValue deaths = new URValue();;

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
        case "еп":
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

        switch (what.trim())
        {
        case "MYY-чж":
            if (midyear_population.total.both != null)
                duplicateValue(what);
            midyear_population.total.both = v;
            break;

        case "чж":
        case "чж-о":
        case "чж-всего-о":
            if (population.total.both != null)
                duplicateValue(what);
            population.total.both = v;
            break;

        case "чж-м":
        case "чж-всего-м":
            if (population.total.male != null)
                duplicateValue(what);
            population.total.male = v;
            break;

        case "чж-ж":
        case "чж-всего-ж":
            if (population.total.female != null)
                duplicateValue(what);
            population.total.female = v;
            break;

        case "чр":
        case "чр-о":
            if (births.total.both != null)
                duplicateValue(what);
            births.total.both = v;
            break;
            
        case "чр-м":
            if (births.total.male!= null)
                duplicateValue(what);
            births.total.male = v;
            break;

        case "чр-ж":
            if (births.total.female != null)
                duplicateValue(what);
            births.total.female = v;
            break;

        case "чу":
        case "чс-о":
            if (deaths.total.both != null)
                duplicateValue(what);
            deaths.total.both = v;
            break;

        case "чс-м":
            if (deaths.total.male!= null)
                duplicateValue(what);
            deaths.total.male = v;
            break;

        case "чс-ж":
            if (deaths.total.female != null)
                duplicateValue(what);
            deaths.total.female = v;
            break;

        // -----------------

        case "чж-гор-м":
            if (population.urban.male != null)
                duplicateValue(what);
            population.urban.male = v;
            break;

        case "чж-гор-ж":
            if (population.urban.female != null)
                duplicateValue(what);
            population.urban.female = v;
            break;

        case "чж-гор-о":
            if (population.urban.both != null)
                duplicateValue(what);
            population.urban.both = v;
            break;

        // -----------------

        case "чж-уез-м":
            if (population.rural.male != null)
                duplicateValue(what);
            population.rural.male = v;
            break;

        case "чж-уез-ж":
            if (population.rural.female != null)
                duplicateValue(what);
            population.rural.female = v;
            break;

        case "чж-уез-о":
            if (population.rural.both != null)
                duplicateValue(what);
            population.rural.both = v;
            break;

        // -----------------

        case "чр-гор-м":
            if (births.urban.male != null)
                duplicateValue(what);
            births.urban.male = v;
            break;

        case "чр-гор-ж":
            if (births.urban.female != null)
                duplicateValue(what);
            births.urban.female = v;
            break;

        case "чр-гор-о":
            if (births.urban.both != null)
                duplicateValue(what);
            births.urban.both = v;
            break;

        // -----------------

        case "чр-уез-м":
            if (births.rural.male != null)
                duplicateValue(what);
            births.rural.male = v;
            break;

        case "чр-уез-ж":
            if (births.rural.female != null)
                duplicateValue(what);
            births.rural.female = v;
            break;

        case "чр-уез-о":
            if (births.rural.both != null)
                duplicateValue(what);
            births.rural.both = v;
            break;

        // -----------------

        case "чс-гор-м":
            if (deaths.urban.male != null)
                duplicateValue(what);
            deaths.urban.male = v;
            break;

        case "чс-гор-ж":
            if (deaths.urban.female != null)
                duplicateValue(what);
            deaths.urban.female = v;
            break;

        case "чс-гор-о":
            if (deaths.urban.both != null)
                duplicateValue(what);
            deaths.urban.both = v;
            break;

        // -----------------

        case "чс-уез-м":
            if (deaths.rural.male != null)
                duplicateValue(what);
            deaths.rural.male = v;
            break;

        case "чс-уез-ж":
            if (deaths.rural.female != null)
                duplicateValue(what);
            deaths.rural.female = v;
            break;

        case "чс-уез-о":
            if (deaths.rural.both != null)
                duplicateValue(what);
            deaths.rural.both = v;
            break;

        default:
            throw new Exception("Invalid selector");
        }
    }

    private void duplicateValue(String what) throws Exception
    {
        String msg = String.format("Duplicate value %s for %s %d", what, territory.name, year);
        Util.err(msg);
        // throw new Exception(msg);
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
        ty.midyear_population = this.midyear_population.dup();
        ty.population = this.population.dup();
        ty.births = this.births.dup();
        ty.deaths = this.deaths.dup();
        return ty;
    }
    
    public void merge(TerritoryYear ty) throws Exception
    {
        Long p1 = population.total.both;
        if (p1 == null)
            p1 = midyear_population.total.both;
        if (p1 != null && p1 == 0)
            p1 = null;

        Long p2 = ty.population.total.both;
        if (p2 == null)
            p2 = ty.midyear_population.total.both;
        if (p2 != null && p2 == 0)
            p2 = null;
        
        if (p1 == null && p2 == null)
        {
            // do nothing
        }
        else if (p1 != null && p2 != null)
        {
            cbr = rate(p1, cbr, p2, ty.cbr);
            cdr = rate(p1, cdr, p2, ty.cdr);
        }
        else if (p1 == null)
        {
            cbr = ty.cbr;
            cdr = ty.cdr;
        }
        else // if (p2 == null) 
        {
            // do nothing
        }
        
        if (ngr != null)
        {
            if (cbr != null && cdr != null)
                ngr = cbr - cdr;
            else
                ngr = null;
        }
        
        /* --------------------------------------------- */

        population.merge(ty.population);
        midyear_population.merge(ty.midyear_population);
        births.merge(ty.births);
        deaths.merge(ty.deaths);
    }
    
    private Double rate(long p1, Double r1, long p2, Double r2) throws Exception
    {
        if (r1 == null && r2 == null)
        {
            return null;
        }
        else if (r1 != null && r2 != null)
        {
            return (r1 * p1 + r2 * p2) / (p1 + p2);
        }
        else
        {
            return null;
            // throw new Exception("Unable to merge CBR/CDR");
        }
    }

    public String toString()
    {
        return territory.toString() + " " + year;
    }
}
