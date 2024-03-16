package rtss.data.mortality;

import java.util.HashMap;
import java.util.Map;

import rtss.util.Util;

public class SingleMortalityTable
{
    private Map<Integer, MortalityInfo> m = new HashMap<>();
    public static final int MAX_AGE = 100;
    private String path;
    
    private SingleMortalityTable()
    {
    }
    
    public MortalityInfo get(int age) throws Exception
    {
        MortalityInfo mi = m.get(age);
        if (mi == null)
            throw new Exception("Missing mortality table data");
        return mi;
    }
    
    public SingleMortalityTable(String path) throws Exception
    {
        load(path);
    }
    
    private void load(String path) throws Exception
    {
        this.path = path;
        
        String rdata = Util.loadResource(path);
        rdata = rdata.replace("\r\n", "\n");
        for (String line : rdata.split("\n"))
        {
            char unicode_feff = '\uFEFF';
            line = line.replace("" + unicode_feff, "");
                    
            int k = line.indexOf('#');
            if (k != -1)
                line = line.substring(0, k);
            line = line.replace("\t", " ").replaceAll(" +", " ").trim();
            if (line.length() == 0)
                continue;
            
            String[] el = line.split(" ");
            if (el.length != 8)
                throw new Exception("Invalid format of mortality table");
            
            MortalityInfo mi = new MortalityInfo();
            mi.x = asInt(el[0]);
            mi.lx = asInt(el[1]);
            mi.dx = asInt(el[2]);
            mi.qx = asDouble(el[3]);
            mi.px = asDouble(el[4]);
            mi.Lx = asInt(el[5]);
            mi.Tx = asInt(el[6]);
            mi.ex = asDouble(el[7]);
            
            if (mi.x < 0 || mi.x > MAX_AGE)
                throw new Exception("Invalid data in mortality table");
            
            if (m.containsKey(mi.x))
                throw new Exception("Duplicate entries in mortality table");
            
            m.put(mi.x, mi);
        }
        
        for (int age = 0; age <= MAX_AGE; age++)
        {
            if (!m.containsKey(age))
                throw new Exception("Mising entry in mortality table");
        }
        
        validate();
    }
    
    public void validate() throws Exception
    {
        /*
         * Tables do have minor inconsistencies, so we'll allow some tolerance margin
         */
        for (int age = 0; age <= MAX_AGE; age++)
        {
            MortalityInfo mi = get(age);
            check_eq("px+qx for age " + age, mi.px + mi.qx, 1.0, 0.011);
            if (Util.False && Math.abs(Math.round(mi.lx * mi.qx) - mi.dx) > 2) // ###
                throw new Exception("Inconsistent mortality table");
        
            if (age != MAX_AGE)
            {
                MortalityInfo mi2 = get(age + 1);
                if (Util.False && Math.abs(mi.lx - mi.dx - mi2.lx) > 2) // ###
                    throw new Exception("Inconsistent mortality table");
            }
        }
    }
    
    @SuppressWarnings("unused")
    private void check_eq(String what, double a, double b) throws Exception
    {
        check_eq(what, a, b, 0.00001);
    }

    private void check_eq(String what, double a, double b, double diff) throws Exception
    {
        if (differ(a,b, diff))
        {
            inconsistent(what + " differ by " + (a - b));
        }
    }
    
    private void inconsistent(String what) throws Exception
    {
        String msg = "Inconsistent mortality table: " + what + " in " + path;
        // ### throw new Exception("Inconsistent mortality table");
        Util.err(msg);
    }

    @SuppressWarnings("unused")
    private boolean differ(double a, double b)
    {
        return differ(a, b, 0.00001);
    }

    private boolean differ(double a, double b, double diff)
    {
        return Math.abs(a - b) / Math.max(Math.abs(a), Math.abs(b)) > diff;
    }

    private int asInt(String s)
    {
        return Integer.parseInt(s.replace(",", ""));
    }

    private double asDouble(String s)
    {
        return Double.parseDouble(s.replace(",", ""));
    }
    
    static SingleMortalityTable interpolate(SingleMortalityTable mt1, SingleMortalityTable mt2, double weight) throws Exception
    {
        SingleMortalityTable smt = new SingleMortalityTable();
        smt.path = "interpolated";
        smt.do_interpolate(mt1, mt2, weight);
        return smt;
    }

    private void do_interpolate(SingleMortalityTable mt1, SingleMortalityTable mt2, double weight) throws Exception
    {
        if (weight < 0 || weight > 1)
            throw new Exception("Incorrect interpolation weight");
        
        for (int age = 0; age <= MAX_AGE; age++)
        {
            MortalityInfo mi1 = mt1.get(age);
            MortalityInfo mi2 = mt2.get(age);
            MortalityInfo mi = new MortalityInfo();
            mi.px = (1 - weight) * mi1.px + weight * mi2.px;
            mi.qx = 1.0 - mi.px;
            m.put(age,  mi);
        }
    }
}
