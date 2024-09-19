package rtss.pre1917.validate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.eval.MergeTaxon;
import rtss.util.Util;

public class ValidateTaxons
{
    private TerritoryDataSet territories;

    public ValidateTaxons(TerritoryDataSet territories)
    {
        this.territories = territories;
    }

    public void validate_taxons() throws Exception
    {
        for (String tname : territories.keySet())
        {
            if (Taxon.isComposite(tname))
                validate_taxon(tname);
        }
    }

    private void validate_taxon(String txname) throws Exception
    {
        Territory t = territories.get(txname);
        Territory mt = new MergeTaxon(territories).mergeTaxon(txname);

        for (int year : t.years())
        {
            TerritoryYear ty = t.territoryYearOrNull(year);
            TerritoryYear mty = mt.territoryYearOrNull(year);
            if (ty == null || mty == null)
                continue;
            validate_taxon(mty, ty);
        }
    }

    private void validate_taxon(TerritoryYear mty, TerritoryYear ty) throws Exception
    {
        String[] lfn = { "population", "midyear_population", "births", "deaths" };
        List<String> xfn = Arrays.asList(lfn);
        xfn = appendNameComponent(xfn, "total", "rural", "urban");
        xfn = appendNameComponent(xfn, "male", "female", "both");
        
        for (String fn : xfn)
        {
            Long mv = FieldValue.getLong(mty, fn); 
            Long v = FieldValue.getLong(ty, fn);

            if (mv != null && v != null && !mv.equals(v)) 
            {
                String msg = String.format("Расхождение таксона с составляющими %d %s %s: %,d vs %,d (merged vs. excel-value, diff: %,d)",
                                           mty.year,
                                           mty.territory.name, fn,
                                           mv, v, Math.abs(mv - v));
                Util.err(msg);
            }
        }
    }
    
    /* ======================================================================== */

    private List<String> appendNameComponent(List<String> xl, String... names)
    {
        List<String> xxl = new ArrayList<>();
        for (String s : xl)
        {
            for (String name : names)
                xxl.add(s + "." + name);
        }
        return xxl;
    }
}
