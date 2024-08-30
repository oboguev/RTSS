package rtss.pre1917.data;

import java.util.HashMap;

import rtss.pre1917.eval.MergeCities;
import rtss.pre1917.util.WeightedAverage;
import rtss.util.Util;

public class TerritoryDataSet extends HashMap<String, Territory>
{
    private static final long serialVersionUID = 1L;
    
    public final DataSetType dataSetType;
    
    public TerritoryDataSet(DataSetType dataSetType)
    {
        this.dataSetType = dataSetType;
    }

    public TerritoryDataSet dup()
    {
        TerritoryDataSet tds = new TerritoryDataSet(dataSetType);
        for (String name : keySet())
            tds.put(name, get(name).dup());
        return tds;
    }

    public void evalTaxon(String name, boolean overwrite) throws Exception
    {
        for (int year = 1891; year <= 1915; year++)
            evalTaxon(name, year, overwrite);
    }

    public void evalTaxon(String name, int year, boolean overwrite) throws Exception
    {
        Taxon tx = Taxon.of(name, year, dataSetType);
        if (tx == null)
            return;

        /* cascade child sub-taxon evaluation */
        for (String xname : tx.territories.keySet())
            evalTaxon(xname, year, overwrite);

        Territory ter = get(name);
        if (ter == null)
            Util.noop();

        /* calculated TerritoryYear */
        TerritoryYear cty = new TerritoryYear(ter, year);
        WeightedAverage av_cbr = new WeightedAverage();
        WeightedAverage av_cdr = new WeightedAverage();

        for (String xname : tx.territories.keySet())
        {
            /* child and Territory TerritoryYear */
            Territory xter = get(xname);
            double fraction = tx.territories.get(xname);
            
            if (xter != null && xter.hasYear(year))
            {
                TerritoryYear xty = xter.territoryYear(year);
                if (xty.population.all() != null)
                {
                    if (cty.population.all() == null)
                        cty.population.total.both = 0L;
                    cty.population.total.both = Math.round(cty.population.all() + fraction * xty.population.all());
                }

                if (xty.births.all() != null)
                {
                    if (cty.births.all() == null)
                        cty.births.total.both = 0L;
                    cty.births.total.both = Math.round(cty.births.all() + fraction * xty.births.all());
                }

                if (xty.deaths.all() != null)
                {
                    if (cty.deaths.all() == null)
                        cty.deaths.total.both = 0L;
                    cty.deaths.total.both = Math.round(cty.deaths.all() + fraction * xty.deaths.all());
                }

                if (cty.population.all() != null && xty.cbr != null)
                    av_cbr.add(xty.cbr, cty.population.all());
                if (cty.population.all() != null && xty.cdr != null)
                    av_cdr.add(xty.cdr, cty.population.all());
            }
        }

        cty.cbr = av_cbr.doubleResult();
        cty.cdr = av_cdr.doubleResult();

        if (cty.cbr != null && cty.cdr != null)
            cty.ngr = cty.cbr - cty.cdr;

        /* merge cty to ty */
        TerritoryYear ty = ter.territoryYear(year);

        if (cty.population.all() != null && (overwrite || ty.population.all() == null))
            ty.population.total.both = cty.population.all();

        if (cty.births.all() != null && (overwrite || ty.births.all() == null))
            ty.births.total.both = cty.births.all();

        if (cty.deaths != null && (overwrite || ty.deaths == null))
            ty.deaths = cty.deaths;

        if (cty.cbr != null && (overwrite || ty.cbr == null))
            ty.cbr = cty.cbr;

        if (cty.cdr != null && (overwrite || ty.cdr == null))
            ty.cdr = cty.cdr;

        if (cty.ngr != null && (overwrite || ty.ngr == null))
            ty.ngr = cty.ngr;
    }
    
    public void mergeCities() throws Exception
    {
        new MergeCities(this).merge();  
    }
}
