package rtss.pre1917.merge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public static Set<String> leaveOnlyCombined(Set<String> tnames) throws Exception
    {
        Set<String> xs = new HashSet<>(tnames);

        for (MergeDescriptor md : MergeCitiesDescriptors)
        {
            if (md.parent != null)
                xs.remove(md.parent);
            for (String child : md.children)
                xs.remove(child);
            if (!xs.contains(md.combined))
                throw new Exception("leaveOnlyCombined: set does not include " + md.combined);
        }

        return xs;
    }
    
    public static MergeDescriptor findContaining(String what)
    {
        return MergeDescriptor.findContaining(MergeCitiesDescriptors, what);
    }
    
    public static String combined2parent(String combined)
    {
        return MergeDescriptor.combined2parent(MergeCitiesDescriptors, combined);
    }

    public static String parent2combined(String combined)
    {
        return MergeDescriptor.parent2combined(MergeCitiesDescriptors, combined);
    }
    
    public static void removeCities(Map<String, ?> m)
    {
        for (MergeDescriptor md : MergeCitiesDescriptors)
        {
            for (String city : md.children)
                m.remove(city);
        }
    }
}
