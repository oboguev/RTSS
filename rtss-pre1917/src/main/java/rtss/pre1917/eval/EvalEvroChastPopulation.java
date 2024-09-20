package rtss.pre1917.eval;

import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.util.Util;

public class EvalEvroChastPopulation
{
    public void eval(TerritoryDataSet tds) throws Exception
    {
        for (String tname : tds.keySet())
        {
            Territory t = tds.get(tname);
            eval(t);
        }
        
        // show(tds, "Ярославская");
        // show(tds, "50 губерний Европейской России");
    }
    
    private void eval(Territory t) throws Exception
    {
        TerritoryYear ty = t.territoryYear(1897);
        if (ty.midyear_population.total.both == null)
            throw new Exception("Missing data");
        
        for (int year = 1898; year <= 1910; year++)
        {
            TerritoryYear xty = t.territoryYear(year);
            
            long incr1 = ty.births.total.both - ty.deaths.total.both;
            long incr2 = xty.births.total.both - xty.deaths.total.both;
            long incr = Math.round((incr1 + incr2) / 2.0);
            
            xty.midyear_population.total.both = ty.midyear_population.total.both + incr;
            ty = xty;
        }
    }
    
    private void show(TerritoryDataSet tds, String tname)
    {
        Territory t = tds.get(tname);
        
        Util.out(String.format("%s - midyear population", tname));
        for (int year = 1897; year <= 1910; year++)
        {
            TerritoryYear ty = t.territoryYear(year);
            Util.out(String.format("%d %,d", year, ty.midyear_population.total.both));
        }

        Util.out("");
        Util.out("==========================");
        Util.out("");
        Util.out(String.format("%s - beginning of year population", tname));

        for (int year = 1898; year <= 1910; year++)
        {
            TerritoryYear ty1 = t.territoryYear(year - 1);
            TerritoryYear ty2 = t.territoryYear(year);
            long pop = ty1.midyear_population.total.both + ty2.midyear_population.total.both;
            pop = Math.round(pop / 2.0);
            Util.out(String.format("%d %,d", year, pop));
        }

        Util.out("");
        Util.out("==========================");
        Util.out("");
        Util.out(String.format("%s - listed population", tname));

        for (int year = 1897; year <= 1910; year++)
        {
            TerritoryYear ty = t.territoryYear(year);
            String s = "";
            if (ty.population.total.both != null)
                s = String.format("%,d", ty.population.total.both);
            Util.out(String.format("%d %s", year, s));
        }
    }
}
