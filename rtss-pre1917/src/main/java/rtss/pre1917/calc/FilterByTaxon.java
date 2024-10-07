package rtss.pre1917.calc;

import java.util.HashSet;
import java.util.Set;

import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.TerritoryDataSet;

public class FilterByTaxon
{
    public static TerritoryDataSet filterByTaxon(String txname, TerritoryDataSet tds) throws Exception
    {
        Set<String> tnames = new HashSet<>();

        for (int year = tds.minYear(); year <= tds.maxYear(); year++)
        {
            Taxon tx = Taxon.of(txname, year, tds);
            tx = tx.flatten(tds, year);

            for (String tname : tx.territories.keySet())
                tnames.add(tname);
        }

        TerritoryDataSet tds2 = tds.dup();
        tds2.clear();
        for (String tname : tnames)
        {
            if (tds.containsKey(tname))
                tds2.put(tname, tds.get(tname).dup());
        }

        return tds2;
    }

    public static TerritoryDataSet filteredOutByTaxon(String txname, TerritoryDataSet tds) throws Exception
    {
        Set<String> tnames = new HashSet<>();

        for (int year = tds.minYear(); year <= tds.maxYear(); year++)
        {
            Taxon tx = Taxon.of(txname, year, tds);
            tx = tx.flatten(tds, year);

            for (String tname : tx.territories.keySet())
                tnames.add(tname);
        }

        TerritoryDataSet tds2 = tds.dup();
        tds2.clear();
        for (String tname : tds.keySet())
        {
            if (!tnames.contains(tname))
                tds2.put(tname, tds.get(tname).dup());
        }

        return tds2;
    }
}
