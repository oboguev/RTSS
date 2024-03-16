package rtss.data.mortality;

import java.util.HashMap;
import java.util.Map;

import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;

public class CombinedMortalityTable
{
    protected Map<String, SingleMortalityTable> m = new HashMap<>();
    
    protected CombinedMortalityTable()
    {
    }
    
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
    
    protected String key(Locality locality, Gender gender)
    {
        return locality + "-" + gender;
    }
    
    protected void setTable(Locality locality, Gender gender, SingleMortalityTable smt)
    {
        m.put(key(locality, gender), smt);
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
    
    /*
     * Create mortality table interpolating between @mt1 and @mt2.
     * We only interpolate qx and px.
     * Other fields are left invalid.
     * @weight ranges from 0 to 1:
     *       0 selects @mt1
     *       1 selects @mt2
     *  ]0..1[ selects the value interpolated as (mt1 * (1 - weight) * mt1 + mt2 * weight)   
     */
    public static CombinedMortalityTable interpolate(CombinedMortalityTable mt1, CombinedMortalityTable mt2, double weight)  throws Exception
    {
        CombinedMortalityTable cmt = new CombinedMortalityTable();
        cmt.interpolate(Gender.BOTH, mt1, mt2, weight);
        cmt.interpolate(Gender.MALE, mt1, mt2, weight);
        cmt.interpolate(Gender.FEMALE, mt1, mt2, weight);
        return cmt;
    }
    
    private void interpolate(Gender gender, CombinedMortalityTable mt1, CombinedMortalityTable mt2, double weight)  throws Exception
    {
        interpolate(Locality.TOTAL, gender, mt1, mt2, weight);
        interpolate(Locality.RURAL, gender, mt1, mt2, weight);
        interpolate(Locality.URBAN, gender, mt1, mt2, weight);
    }

    private void interpolate(Locality locality, Gender gender, CombinedMortalityTable mt1, CombinedMortalityTable mt2, double weight) throws Exception
    {
        String key = key(locality, gender);
        SingleMortalityTable smt1 = mt1.m.get(key);
        SingleMortalityTable smt2 = mt2.m.get(key);
        m.put(key, SingleMortalityTable.interpolate(smt1, smt2, weight));
    }
}
