package rtss.pre1917.merge;

import java.util.ArrayList;
import java.util.List;

import rtss.pre1917.data.TerritoryDataSet;

public class MergePost1897Regions
{
    public static final List<MergeDescriptor> MergePost1897Descriptors = new ArrayList<>();
    
    static
    {
        define("Иркутская с Камчатской", "Иркутская", "Камчатская");
        define("Кутаисская с Батумской", "Кутаисская", "Батумская");
        define("Люблинская с Седлецкой и Холмской", "Люблинская", "Седлецкая", "Холмская");
    }
    
    private static void define(String combined, String parent, String... children)
    {
        MergePost1897Descriptors.add(new MergeDescriptor(combined, parent, children));     
    }
    
    public static MergeDescriptor find(String combined)
    {
        return MergeDescriptor.find(MergePost1897Descriptors, combined);
    }

    /* ================================================================== */
    
    private TerritoryDataSet territories;

    public MergePost1897Regions(TerritoryDataSet territories)
    {
        this.territories = territories;
    }

    public void merge() throws Exception
    {
        new MergeTerritories(territories).merge(MergePost1897Descriptors);
    }

}
