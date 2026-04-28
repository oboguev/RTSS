package rtss.pre1917.data;

import rtss.pre1917.data.URValue.URValueWhich;

/*
 * Содержит данные раздельно для городского и сельского населения, и их сумму.
 */
public class RateURValue
{
    public RateValueByGender total = new RateValueByGender(this, URValueWhich.TOTAL);
    public RateValueByGender rural = new RateValueByGender(this, URValueWhich.RURAL);
    public RateValueByGender urban = new RateValueByGender(this, URValueWhich.URBAN);

    public final TerritoryYear territoryYear;

    public RateURValue(TerritoryYear territoryYear)
    {
        this.territoryYear = territoryYear;
    }

    public Double all()
    {
        return total.both;
    }

    public RateURValue dup(TerritoryYear territoryYear)
    {
        RateURValue x = new RateURValue(territoryYear);

        x.total = this.total.dup(x);
        x.rural = this.rural.dup(x);
        x.urban = this.urban.dup(x);

        return x;
    }

    public void leaveOnlyTotalBoth()
    {
        rural.clear();
        urban.clear();
        total.leaveOnlyBoth();
    }

    public void leaveOnlyTotal(Gender gender) throws Exception
    {
        urban.clear(gender);
        rural.clear(gender);
    }
    
    public String toString()
    {
        return String.format("%s: U = (%s), R = (%s), T = (%s)",
                             territoryYear == null ? "anonymous" : territoryYear.toString(),
                             urban.toString(),
                             rural.toString(),
                             total.toString());
    }
    
    public boolean hasOnlyTotal(Gender gender) throws Exception
    {
        return urban.get(gender) == null && rural.get(gender) == null && total.get(gender) != null;
    }
}
