package rtss.pre1917.validate;

import java.util.Set;

import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.util.Util;

public class ValidateTaxons
{
    public void validate_taxons(TerritoryDataSet territories)
    {
        TerritoryDataSet t2 = territories.dup();
        t2.evalTaxon("Империя", true);

        for (int year = 1891; year <= 1915; year++)
        {
            Taxon tx = Taxon.of("Империя", year);
            Set<String> txnames = tx.allCompositeSubTaxons(true);
            for (String txname : txnames)
            {
                validate_taxon(txname, year, territories, t2);
            }
            Util.noop();
        }

        Util.noop();
    }

    private void validate_taxon(String txname, int year, TerritoryDataSet territories, TerritoryDataSet t2)
    {
        Territory ter = territories.get(txname);
        Territory ter2 = t2.get(txname);
        String msg;

        if (!(ter.hasYear(year) && ter2.hasYear(year)))
            return;

        TerritoryYear ty = ter.territoryYear(year);
        TerritoryYear ty2 = ter2.territoryYear(year);

        if (differ(ty.population, ty2.population, 0.005))
        {
            msg = String.format("Taxon differs: %s %d population listed=%,3d calculated=%,3d diff=%.2f%%",
                                txname, year, ty.population, ty2.population, 
                                pctDiff(ty.population, ty2.population));
            Util.out(msg);
        }

        // ### validate births, deaths, cbr, cdr
    }

    private boolean differ(Long a, Long b, double tolerance)
    {
        if (a == null || b == null)
            return false;
        if (a == b)
            return false;
        if (a == 0 && b == 0)
            return false;
        return (double) Math.abs(a - b) / (double) Math.max(a, b) > tolerance;
    }

    private boolean differ(Double a, Double b, double tolerance)
    {
        if (a == null || b == null)
            return false;
        if (a == 0 && b == 0)
            return false;
        return Math.abs(a - b) / Math.max(a, b) > tolerance;
    }

    private double pctDiff(double a, double b)
    {
        double min = Math.min(a, b);
        double max = Math.max(a, b);
        if (min > 0)
        {
            return 100 * (max - min) / min;
        }
        else
        {
            return 100 * (max - min) / max;
        }
    }
}
