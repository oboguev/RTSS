package rtss.pre1917.eval;

import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.util.Util;

public class ApplyWarDeaths
{
    private final double empirePopulation1904;
    private final double empirePopulation1914;

    private final double EmpireWarDeaths_1904 = 25_589;
    private final double EmpireWarDeaths_1905 = 25_363;
    private final double EmpireWarDeaths_1914 = 177_000;

    public ApplyWarDeaths(long empirePopulation1904, long empirePopulation1914)
    {
        this.empirePopulation1904 = empirePopulation1904;
        this.empirePopulation1914 = empirePopulation1914;
    }

    public void apply(TerritoryDataSet tds)
    {
        for (Territory t : tds.values())
        {
            apply(t);
        }
    }

    private void apply(Territory t)
    {
        apply(t, 1904, EmpireWarDeaths_1904, empirePopulation1904, 1904);
        apply(t, 1905, EmpireWarDeaths_1905, empirePopulation1904, 1904);
        apply(t, 1914, EmpireWarDeaths_1914, empirePopulation1914, 1914);
    }

    private void apply(Territory t, int year, double empireDeaths, double empirePopulation, int referenceYear)
    {
        TerritoryYear ty = t.territoryYearOrNull(referenceYear);
        if (ty == null && referenceYear == 1914)
            throw new RuntimeException("No 1914 data for " + t.name);
        
        double fraction = ty.progressive_population.total.both / empirePopulation;
        long extraDeaths = Math.round(empireDeaths * fraction);
        
        t.extraDeaths(year, extraDeaths);
    }
}
