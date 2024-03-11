package data.mortality;

import java.util.HashMap;
import java.util.Map;

public class CombinedMortalityTable
{
    private Map<String, SingleMortalityTable> m = new HashMap<>();
    
    public CombinedMortalityTable(String path) throws Exception
    {
        load(path);
    }
    
    private void load(String path) throws Exception
    {
        loadTables(path, "both");
        loadTables(path, "male");
        loadTables(path, "female");
    }
    
    private void loadTables(String path, String gender) throws Exception
    {
        loadTables(path, gender, "total");
        loadTables(path, gender, "rural");
        loadTables(path, gender, "urban");
    }
    
    private String key(String locale, String gender)
    {
        return locale + "-" + gender;
    }

    private void loadTables(String path, String gender, String locale) throws Exception
    {
        String fng = gender;
        if (fng.equals("both"))
            fng = "both_genders";
        
        String fn = String.format("%s/%s_%s.txt", path, fng, locale);
        
        SingleMortalityTable mt = new SingleMortalityTable(fn);
        m.put(key(locale, gender), mt);
    }
}
