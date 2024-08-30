package rtss.pre1917.validate;

import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;

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
        Territory ter = territories.get(txname);
        for (int year : ter.years())
        {
            
        }
    }
}
