package data.mortality;

import java.util.HashMap;
import java.util.Map;

import my.Util;

public class SingleMortalityTable
{
    private Map<Integer, MortalityInfo> m = new HashMap<>();
    public static final int MAX_AGE = 100;
    
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
            mi.lх = asInt(el[1]);
            mi.dх = asInt(el[2]);
            mi.qх = asDouble(el[3]);
            mi.pх = asDouble(el[4]);
            mi.Lх = asInt(el[5]);
            mi.Tх = asInt(el[6]);
            mi.eх = asDouble(el[7]);
            
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
    }
    
    private int asInt(String s)
    {
        return Integer.parseInt(s.replace(",", ""));
    }

    private double asDouble(String s)
    {
        return Double.parseDouble(s.replace(",", ""));
    }
}
