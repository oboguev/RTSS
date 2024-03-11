package data.mortality;

import java.util.HashMap;
import java.util.Map;

import data.selectors.Gender;
import data.selectors.Locality;

public class CombinedMortalityTable
{
    private Map<String, SingleMortalityTable> m = new HashMap<>();
    
    public MortalityInfo get(Locality locality, Gender gender, int age) throws Exception
    {
        String key = key(locality, gender);
        return m.get(key).get(age);
    }

    public CombinedMortalityTable(String path) throws Exception
    {
        load(path);
    }
    
    private void load(String path) throws Exception
    {
        loadTables(path, Gender.BOTH);
        loadTables(path, Gender.MALE);
        loadTables(path, Gender.FEMALE);
    }
    
    private void loadTables(String path, Gender gender) throws Exception
    {
        loadTables(path, gender, Locality.TOTAL);
        loadTables(path, gender, Locality.RURAL);
        loadTables(path, gender, Locality.URBAN);
    }
    
    private String key(Locality locality, Gender gender)
    {
        return locality + "-" + gender;
    }

    private void loadTables(String path, Gender gender, Locality locality) throws Exception
    {
        String fng = gender.toString();
        if (fng.equals("both"))
            fng = "both_genders";
        
        String fn = String.format("%s/%s_%s.txt", path, fng, locality.toString());
        
        SingleMortalityTable mt = new SingleMortalityTable(fn);
        m.put(key(locality, gender), mt);
    }
}
