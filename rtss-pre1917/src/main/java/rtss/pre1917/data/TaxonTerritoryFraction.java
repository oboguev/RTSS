package rtss.pre1917.data;

public class TaxonTerritoryFraction
{
    TaxonTerritoryFraction[] ttfs = null;

    private double fraction; 
    
    @SuppressWarnings("unused")
    private TaxonTerritoryFraction()
    {
    }

    public TaxonTerritoryFraction(double fraction)
    {
        this.fraction = fraction;
    }
    
    public double fraction(int year)
    {
        if (ttfs != null)
        {
            double result = 1.0;
            for (TaxonTerritoryFraction ttf : ttfs)
                result *= ttf.fraction(year);
            return result;
        }
        
        return fraction;
    }

    public TaxonTerritoryFraction(TaxonTerritoryFraction... ttfs)
    {
        if (ttfs.length == 0)
            throw new IllegalArgumentException("Empty TaxonTerritoryFraction list");
        this.ttfs = ttfs;
    }
}
