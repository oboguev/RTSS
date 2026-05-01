package rtss.pre1917.war;

import rtss.pre1917.LoadData;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.util.Util;

public class ApplyWarDeaths
{
    private final long empirePopulation1904;
    private final long empirePopulation1914;

    public static final long EmpireWarDeaths_1904 = 25_589;
    public static final long EmpireWarDeaths_1905 = 25_363;
    public static final long EmpireWarDeaths_1914 = 177_000;

    public static final long RsfsrWarDeaths_1904 = 13_465;
    public static final long RsfsrWarDeaths_1905 = 13_346;
    public static final long RsfsrWarDeaths_1914 = 98_600;

    public ApplyWarDeaths(long empirePopulation1904, long empirePopulation1914)
    {
        this.empirePopulation1904 = empirePopulation1904;
        this.empirePopulation1914 = empirePopulation1914;
    }

    public void apply(TerritoryDataSet tds) throws Exception
    {
        for (Territory t : tds.values())
        {
            apply(t);
        }
    }

    private void apply(Territory t) throws Exception
    {
        apply(t, 1904, EmpireWarDeaths_1904, empirePopulation1904, 1904);
        apply(t, 1905, EmpireWarDeaths_1905, empirePopulation1904, 1904);
        apply(t, 1914, EmpireWarDeaths_1914, empirePopulation1914, 1914);
    }

    private void apply(Territory t, int year, long empireDeaths, long empirePopulation, int referenceYear) throws Exception
    {
        WarLossShare wls = new LoadData().loadWarLossShare();

        TerritoryYear ty = t.territoryYearOrNull(referenceYear);

        if (ty == null || ty.progressive_population == null ||
            ty.progressive_population.total == null ||
            ty.progressive_population.total.both == null)
        {
            if (Taxon.isComposite(t.name))
                return;
        }

        Double fraction = null;
        
        if (year == 1914)
            fraction = wls.getLossPercentageVsEmpireForTeritory(t.name);
        
        if (fraction != null)
        {
            fraction /= 100.0;
        }
        else
        {
            // Util.err("No war loss data for " + t.name);
            fraction = (double) ty.progressive_population.total.both / empirePopulation;
        }

        long extraDeaths = Math.round(empireDeaths * fraction);

        t.extraDeaths(year, extraDeaths);
    }
    
    public void print(TerritoryDataSet tds) throws Exception
    {
        WarLossShare wls = new LoadData().loadWarLossShare();

        Util.out("=========================================");
        for (String tname : tds.keySet())
        {
            Double f1 = wls.getLossPercentageVsEmpireForTeritory(tname);
            if (f1 != null)
                f1 /= 100.0;
            
            Territory t = tds.get(tname);
            TerritoryYear ty = t.territoryYearOrNull(1914);
            double f2 = (double) ty.progressive_population.total.both / empirePopulation1914;
            
            Util.out(String.format("\"%s\" %s %f", tname, f1, f2));
        }
        Util.out("=========================================");
    }
}
