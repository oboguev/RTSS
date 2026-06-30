package rtss.pre1917.data;

public class TaxonTerritoryFraction
{
    private TaxonTerritoryFraction[] ttfs = null;
    
    private Integer year1;
    private Integer year2;
    private Double fraction1;
    private Double fraction2;

    private double fraction; 
    
    @SuppressWarnings("unused")
    private TaxonTerritoryFraction()
    {
    }
    
    /* =========================================================== */

    public TaxonTerritoryFraction(TaxonTerritoryFraction... ttfs)
    {
        if (ttfs.length == 0)
            throw new IllegalArgumentException("Empty TaxonTerritoryFraction list");
        this.ttfs = ttfs;
    }

    public TaxonTerritoryFraction(int year1, double fraction1, int year2, double fraction2)
    {
        if (fraction1 <= 0 || fraction1 >= 1)
            throw new IllegalArgumentException();
        
        if (fraction2 <= 0 || fraction2 >= 1)
            throw new IllegalArgumentException();

        this.year1 = year1;
        this.year2 = year2;
        
        this.fraction1 = fraction1;
        this.fraction2 = fraction2;
    }

    public TaxonTerritoryFraction(double fraction)
    {
        if (fraction <= 0 || fraction > 1)
            throw new IllegalArgumentException();
        this.fraction = fraction;
    }
    
    /* =========================================================== */

    static public TaxonTerritoryFraction percent(double pct)
    {
        return new TaxonTerritoryFraction(pct / 100.0);
    }

    static public TaxonTerritoryFraction percent(int year1, double fraction1, int year2, double fraction2)
    {
        return new TaxonTerritoryFraction(year1, fraction1 / 100.0, year2, fraction2 / 100.0);
    }

    /* =========================================================== */

    public double fraction(int year)
    {
        if (ttfs != null)
        {
            double result = 1.0;
            for (TaxonTerritoryFraction ttf : ttfs)
                result *= ttf.fraction(year);
            return result;
        }
        else if (year1 != null)
        {
            
            if (year <= year1)
                return fraction1;
            else if (year >= year2)
                return fraction2;
            else
                // use linear interpolation (year1, fraction1) -> (year2, fraction2)
                return fraction1 + (fraction2 - fraction1) * (year - year1) / (double)(year2 - year1);
        }
        else
        {
            return fraction;
        }
    }

    /* =========================================================== */
    
    public String describe() throws Exception
    {
        if (ttfs != null)
        {
            throw new Exception("Nested description not implemented");
        }
        else if (year1 != null)
        {
            return String.format("%.1f%% в %d → %.1f%% в %d", fraction1 * 100.0, year1, fraction2 * 100.0, year2);
        }
        else
        {
            return String.format("%.1f%%", fraction * 100.0);
        }
        
    }
}
