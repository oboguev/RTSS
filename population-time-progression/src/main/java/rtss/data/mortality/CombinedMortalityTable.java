package rtss.data.mortality;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.FastUUID;

/*
 * Таблица смертности для трёх видов местности (URBAN, RURAL и TOTAL).
 * Каждая секция (для того или вида вида местности) содержит содержит разделы для полов (MALE, FEMALE, BOTH) в этой местности.
 * 
 * Может также содержать таблицу только для местности TOTAL (с тремя полами для этой местности). 
 */
public class CombinedMortalityTable
{
    public static final int MAX_AGE = SingleMortalityTable.MAX_AGE;

    protected Map<String, SingleMortalityTable> m = new HashMap<>();
    private boolean sealed = false;

    protected CombinedMortalityTable()
    {
        source = "unknown";
    }

    public static CombinedMortalityTable newEmptyTable()
    {
        return new CombinedMortalityTable();
    }
    
    public boolean isTotalOnly() throws Exception
    {
        for (Gender gender : Gender.ThreeGenders)
        {
            if (getSingleTable(Locality.URBAN, gender) != null)
                return false;
        }

        for (Gender gender : Gender.ThreeGenders)
        {
            if (getSingleTable(Locality.RURAL, gender) != null)
                return false;
        }
        
        return true;
    }

    static public CombinedMortalityTable loadTotal(String path) throws Exception
    {
        CombinedMortalityTable cmt = new CombinedMortalityTable();
        cmt.source = path;
        cmt.loadTables(path, Gender.BOTH, Locality.TOTAL);
        cmt.loadTables(path, Gender.MALE, Locality.TOTAL);
        cmt.loadTables(path, Gender.FEMALE, Locality.TOTAL);
        return cmt;
    }

    static public CombinedMortalityTable load(String path) throws Exception
    {
        return new CombinedMortalityTable(path);
    }

    public MortalityInfo get(Locality locality, Gender gender, int age) throws Exception
    {
        String key = key(locality, gender);
        return m.get(key).get(age);
    }

    public CombinedMortalityTable(String path) throws Exception
    {
        do_load(path);
    }

    private void do_load(String path) throws Exception
    {
        source = path;
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

    public void setTable(Locality locality, Gender gender, SingleMortalityTable smt) throws Exception
    {
        checkWritable();
        m.put(key(locality, gender), smt);
    }

    public SingleMortalityTable getSingleTable(Locality locality, Gender gender) throws Exception
    {
        return m.get(key(locality, gender));
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

    /*****************************************************************************************************/

    /*
     * Create mortality table interpolating between @mt1 and @mt2.
     * @weight ranges from 0 to 1:
     *       0 selects @mt1
     *       1 selects @mt2
     *  ]0..1[ selects the value interpolated as (mt1 * (1 - weight) * mt1 + mt2 * weight)   
     */
    public static CombinedMortalityTable interpolate(CombinedMortalityTable mt1, CombinedMortalityTable mt2, double weight)
            throws Exception
    {
        CombinedMortalityTable cmt = new CombinedMortalityTable();

        double pct1 = 100 * (1 - weight);
        double pct2 = 100 * weight;

        cmt.source = String.format("interpolated between %.1f%% [%s] and %.1f%% [%s]",
                                   pct1, mt1.source,
                                   pct2, mt2.source);

        if (mt1.comment() != null && mt2.comment() != null)
        {
            cmt.comment = String.format("interpolated between %.1f%% [%s] and %.1f%% [%s]",
                                        pct1, mt1.comment(),
                                        pct2, mt2.comment());
        }
        else
        {
            cmt.comment = "interpolated";
        }

        cmt.interpolate(Gender.BOTH, mt1, mt2, weight);
        cmt.interpolate(Gender.MALE, mt1, mt2, weight);
        cmt.interpolate(Gender.FEMALE, mt1, mt2, weight);

        return cmt;
    }

    private void interpolate(Gender gender, CombinedMortalityTable mt1, CombinedMortalityTable mt2, double weight) throws Exception
    {
        interpolate(Locality.TOTAL, gender, mt1, mt2, weight);
        interpolate(Locality.RURAL, gender, mt1, mt2, weight);
        interpolate(Locality.URBAN, gender, mt1, mt2, weight);
    }

    private void interpolate(Locality locality, Gender gender, CombinedMortalityTable mt1, CombinedMortalityTable mt2,
            double weight) throws Exception
    {
        String key = key(locality, gender);
        SingleMortalityTable smt1 = mt1.m.get(key);
        SingleMortalityTable smt2 = mt2.m.get(key);
        if (smt1 != null && smt2 != null)
            m.put(key, SingleMortalityTable.interpolate(smt1, smt2, weight));
    }

    /*
     * Create mortality table interpolating between @mt1 and @mt2 in age range [0 ... @toAge].
     * Above @toAge, use @mt1.
     * 
     * @weight ranges from 0 to 1:
     *       0 selects @mt1
     *       1 selects @mt2
     *  ]0..1[ selects the value interpolated as (mt1 * (1 - weight) * mt1 + mt2 * weight)   
     */
    public static CombinedMortalityTable interpolate(CombinedMortalityTable mt1, CombinedMortalityTable mt2, int toAge, double weight)
            throws Exception
    {
        CombinedMortalityTable cmt = new CombinedMortalityTable();

        double pct1 = 100 * (1 - weight);
        double pct2 = 100 * weight;

        cmt.source = String.format("interpolated between %.1f%% [%s] and %.1f%% [%s] for ages 0-%d",
                                   pct1, mt1.source,
                                   pct2, mt2.source,
                                   toAge);

        if (mt1.comment() != null && mt2.comment() != null)
        {
            cmt.comment = String.format("interpolated between %.1f%% [%s] and %.1f%% [%s] for ages 0-%d",
                                        pct1, mt1.comment(),
                                        pct2, mt2.comment(),
                                        toAge);
        }
        else
        {
            cmt.comment = "interpolated";
        }

        cmt.interpolate(Gender.BOTH, mt1, mt2, toAge, weight);
        cmt.interpolate(Gender.MALE, mt1, mt2, toAge, weight);
        cmt.interpolate(Gender.FEMALE, mt1, mt2, toAge, weight);

        return cmt;
    }

    private void interpolate(Gender gender, CombinedMortalityTable mt1, CombinedMortalityTable mt2, int toAge, double weight) throws Exception
    {
        interpolate(Locality.TOTAL, gender, mt1, mt2, toAge, weight);
        interpolate(Locality.RURAL, gender, mt1, mt2, toAge, weight);
        interpolate(Locality.URBAN, gender, mt1, mt2, toAge, weight);
    }

    private void interpolate(Locality locality, Gender gender, CombinedMortalityTable mt1, CombinedMortalityTable mt2,
            int toAge, double weight) throws Exception
    {
        String key = key(locality, gender);
        SingleMortalityTable smt1 = mt1.m.get(key);
        SingleMortalityTable smt2 = mt2.m.get(key);
        if (smt1 != null && smt2 != null)
            m.put(key, SingleMortalityTable.interpolate(smt1, smt2, toAge, weight));
    }

    /*****************************************************************************************************/

    public void saveTable(String dirPath, String comment) throws Exception
    {
        saveTable(dirPath, comment, Locality.TOTAL);
        saveTable(dirPath, comment, Locality.RURAL);
        saveTable(dirPath, comment, Locality.URBAN);
    }

    public void saveTable(String dirPath, String comment, Locality locality) throws Exception
    {
        saveTable(dirPath, comment, locality, Gender.BOTH);
        saveTable(dirPath, comment, locality, Gender.MALE);
        saveTable(dirPath, comment, locality, Gender.FEMALE);
    }

    public void saveTable(String dirPath, String comment, Locality locality, Gender gender) throws Exception
    {
        SingleMortalityTable smt = getSingleTable(locality, gender);

        if (smt != null)
        {
            String lname = locality.toString();
            String gname = gender.toString();

            if (gname.equals("both"))
                gname = "both_genders";

            String fname = String.format("%s_%s.txt", gname, lname);
            File f = new File(new File(dirPath), fname);
            smt.saveTable(f.getAbsoluteFile().getCanonicalPath(), comment);
        }
    }

    /*****************************************************************************************************/

    private String comment;

    public String comment()
    {
        return comment;
    }

    public void comment(String comment)
    {
        // checkWritable();
        this.comment = comment;
    }

    /*****************************************************************************************************/

    private String source;
    private final String tid = FastUUID.getUniqueId();

    public String tableId()
    {
        return tid;
    }

    public int hashCode()
    {
        return tid.hashCode();
    }

    public boolean equals(Object x)
    {
        if (x == null || !(x instanceof CombinedMortalityTable))
            return false;
        return ((CombinedMortalityTable) x).tid.equals(tid);
    }

    /*****************************************************************************************************/
    
    public void seal()
    {
        for (SingleMortalityTable smt : m.values())
            smt.seal();
        sealed = true;
    }
    
    private void checkWritable() throws Exception
    {
        if (sealed)
            throw new Exception("Table is sealed and cannot be modified");
    }
}
