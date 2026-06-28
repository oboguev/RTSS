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
        this.year1 = year1;
        this.year2 = year2;
        
        this.fraction1 = fraction1;
        this.fraction2 = fraction2;
    }

    public TaxonTerritoryFraction(double fraction)
    {
        this.fraction = fraction;
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
                // ### use linear interpolation
                return 0;
        }
        else
        {
            return fraction;
        }
    }
}
