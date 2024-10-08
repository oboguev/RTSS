package rtss.pre1917.merge;

import java.util.ArrayList;
import java.util.List;

import rtss.pre1917.data.TerritoryDataSet;

public class MergeCities
{
    public static final List<MergeDescriptor> MergeCitiesDescriptors = new ArrayList<>();
    
    static
    {
        define("Московская с Москвой", "Московская", "г. Москва");
        define("Санкт-Петербургская с Санкт-Петербургом", "Санкт-Петербургская", "г. Санкт-Петербург");
        define("Варшавская с Варшавой", "Варшавская", "г. Варшава");
        define("Херсонская с Одессой", "Херсонская", "г. Николаев", "г. Одесса");
        define("Таврическая с Севастополем", "Таврическая", "г. Севастополь");
        define("Бакинская с Баку", "Бакинская", "г. Баку");
        define("Область войска Донского", null, "Ростовское и./Д град.");
    }
    
    private static void define(String combined, String parent, String... children)
    {
        MergeCitiesDescriptors.add(new MergeDescriptor(combined, parent, children));     
    }

    /* ================================================================== */
    
    private TerritoryDataSet territories;

    public MergeCities(TerritoryDataSet territories)
    {
        this.territories = territories;
    }

    public void merge() throws Exception
    {
        new MergeTerritories(territories).merge(MergeCitiesDescriptors);
    }
}
