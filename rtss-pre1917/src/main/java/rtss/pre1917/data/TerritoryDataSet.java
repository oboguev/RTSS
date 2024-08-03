package rtss.pre1917.data;

import java.util.HashMap;

import rtss.pre1917.util.WeightedAverage;

public class TerritoryDataSet extends HashMap<String, Territory>
{
    private static final long serialVersionUID = 1L;

    public TerritoryDataSet dup()
    {
        TerritoryDataSet tds = new TerritoryDataSet();
        for (String name : keySet())
            tds.put(name, get(name).dup());
        return tds;
    }

    public void evalTaxon(String name, boolean overwrite)
    {
        for (int year = 1891; year <= 1914; year++)
            evalTaxon(name, year, overwrite);
    }

    public void evalTaxon(String name, int year, boolean overwrite)
    {
        Taxon tx = Taxon.of(name, year);
        if (tx == null)
            return;

        /* cascade child sub-taxon evaluation */
        for (String xname : tx.territories.keySet())
            evalTaxon(xname, year, overwrite);

        Territory ter = get(name);

        /* calculated TerritoryYear */
        TerritoryYear cty = new TerritoryYear(ter, year);
        WeightedAverage av_cbr = new WeightedAverage();
        WeightedAverage av_cdr = new WeightedAverage();

        for (String xname : tx.territories.keySet())
        {
            /* child and Territory TerritoryYear */
            Territory xter = get(xname);
            double fraction = tx.territories.get(xname);

            if (xter.hasYear(year))
            {
                TerritoryYear xty = xter.territoryYear(year);
                if (xty.population != null)
                {
                    if (cty.population == null)
                        cty.population = 0L;
                    cty.population = Math.round(cty.population + fraction * xty.population);
                }

                if (xty.births != null)
                {
                    if (cty.births == null)
                        cty.births = 0L;
                    cty.births = Math.round(cty.births + fraction * xty.births);
                }

                if (xty.deaths != null)
                {
                    if (cty.deaths == null)
                        cty.deaths = 0L;
                    cty.deaths = Math.round(cty.deaths + fraction * xty.deaths);
                }

                if (cty.population != null && xty.cbr != null)
                    av_cbr.add(xty.cbr, cty.population);
                if (cty.population != null && xty.cdr != null)
                    av_cdr.add(xty.cdr, cty.population);
            }
        }

        cty.cbr = av_cbr.doubleResult();
        cty.cdr = av_cdr.doubleResult();

        if (cty.cbr != null && cty.cdr != null)
            cty.ngr = cty.cbr - cty.cdr;

        /* merge cty to ty */
        TerritoryYear ty = ter.territoryYear(year);

        if (cty.population != null && (overwrite || ty.population == null))
            ty.population = cty.population;

        if (cty.births != null && (overwrite || ty.births == null))
            ty.births = cty.births;

        if (cty.deaths != null && (overwrite || ty.deaths == null))
            ty.deaths = cty.deaths;

        if (cty.cbr != null && (overwrite || ty.cbr == null))
            ty.cbr = cty.cbr;

        if (cty.cdr != null && (overwrite || ty.cdr == null))
            ty.cdr = cty.cdr;

        if (cty.ngr != null && (overwrite || ty.ngr == null))
            ty.ngr = cty.ngr;
    }
}
