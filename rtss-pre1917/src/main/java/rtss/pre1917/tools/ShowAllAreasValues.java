package rtss.pre1917.tools;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.util.Util;

public class ShowAllAreasValues extends ShowAreaValues
{
    public static void main(String[] args)
    {
        try
        {
            // new ShowAllAreasValues().show_values_all();
            new ShowAllAreasValues(LoadOptions.MERGE_POST1897_REGIONS).show_values_all();
            // rawShowAllAreasValues().show_values_all();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    protected static ShowAllAreasValues rawShowAllAreasValues() throws Exception
    {
        TerritoryDataSet rawUGVI = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY);
        TerritoryDataSet rawCSK = new LoadData().loadEzhegodnikRossii(LoadOptions.DONT_VERIFY);
        TerritoryDataSet rawCensus1897 = new LoadData().loadCensus1897(LoadOptions.DONT_VERIFY);
        ShowAllAreasValues raw = new ShowAllAreasValues(rawUGVI, rawCSK, rawCensus1897);
        raw.setOnlyRaw();
        return raw;
    }

    @SuppressWarnings("unused")
    protected ShowAllAreasValues(TerritoryDataSet tdsUGVI,
            TerritoryDataSet tdsCSK,
            TerritoryDataSet tdsCensus1897) throws Exception
    {
        super(tdsUGVI, tdsCSK, tdsCensus1897);
    }

    @SuppressWarnings("unused")
    protected ShowAllAreasValues(LoadOptions... options) throws Exception
    {
        super(options);
    }

    @SuppressWarnings("unused")
    protected ShowAllAreasValues() throws Exception
    {
        super(new LoadOptions[0]);
    }

    protected void show_values_all() throws Exception
    {
        for (String tname : Util.sort(tdsUGVI.keySet()))
        {
            if (!Taxon.isComposite(tname))
                show_values(tname);
        }
    }
}
