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
    
    /***********************************************************/

    public SingleMortalityTable(String path) throws Exception
    {
        load(path);
    }
    
    private void load(String path) throws Exception
    {
        this.path = path;
        
        String rdata = Util.loadResource(path);
        rdata = rdata.replace("\r\n", "\n");
        String[] lines = rdata.split("\n");

        for (String line : lines)
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
                throw new Exception("Duplicate entries in mortality table " + path + ", age = " + mi.x);
            
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
            check_eq(String.format("px+qx for age %d px = %f, qx = %f", age, mi.px, mi.qx), 
                     mi.px + mi.qx, 1.0, 0.011);
            if (Math.abs(Math.round(mi.lx * mi.qx) - mi.dx) > 2)
                inconsistent("lx * qx - dx for age " + age);
        
            if (age != MAX_AGE)
            {
                MortalityInfo mi2 = get(age + 1);
                if (Util.False && Math.abs(mi.lx - mi.dx - mi2.lx) > 2)
                    inconsistent("lx - dx - lx_net for age " + age);
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
        String msg = "Inconsistent mortality table in " + path + ": " + what;
        // Util.err(msg);
        throw new Exception(msg);
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
    
    /***********************************************************/

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
    
    /***********************************************************/
    
    static SingleMortalityTable from_qx(String path, double[] qx) throws Exception
    {
        SingleMortalityTable smt = new SingleMortalityTable();
        smt.path = path;
        smt.from_qx(qx);
        return smt;
    }
    
    private void from_qx(double[] qx) throws Exception
    {
        if (qx.length != MAX_AGE + 1)
            throw new IllegalArgumentException("Incorrect qx length");
        
        MortalityInfo prev_mi = null;
        
        for (int age = 0; age <= MAX_AGE; age++)
        {
            MortalityInfo mi = new MortalityInfo();
            mi.x = age;
            mi.qx = qx[age];
            mi.px = 1 - mi.qx;
            
            if (age == 0)
            {
                mi.lx = 100_000;
            }
            else
            {
                mi.lx = prev_mi.lx - prev_mi.dx;
            }
            
            mi.dx = (int) Math.round(mi.lx * mi.qx);
            prev_mi = mi;
        }
        
        validate();
    }
}
