package rtss.external.R;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import rtss.config.Config;
import rtss.util.Util;

/**
 * Execute R scripts
 */
public class R
{
    private static RCall rcall;
    
    public static synchronized String execute(String s, boolean reuse) throws Exception
    {
        String response = rcall().execute(s, reuse);
        if (response == null)
            throw new Exception("No reply from R");
        return response;
    }
    
    public static synchronized void stop() throws Exception
    {
        if (rcall!= null)
            rcall.stop();
        rcall = null;
    }
    
    public static synchronized String ping(String tag) throws Exception
    {
        return rcall().ping(tag);
    }
    
    private static synchronized RCall rcall() throws Exception
    {
        if (rcall == null)
        {
            String endpoint = Config.asString("R.server.endpoint", "");
            endpoint = endpoint.trim();
            if (endpoint.length() == 0)
            {
                rcall = new RLocal().setLog(Config.asBoolean("R.server.log", false));                 
            }
            else
            {
                rcall = new RClient();
            }
        }
        
        return rcall;
    }
    
    private static final String LINE =  "==================================";
    static String BEGIN_SCRIPT = LINE + " BEGIN SCRIPT EXECUTION " + LINE;
    static String END_SCRIPT = LINE + " END OF SCRIPT EXECUTION " + LINE;
    
    public static String c(int[] values)
    {
        StringBuilder sb = new StringBuilder("c(");
        String sep = "";
        
        for (int v : values)
        {
            sb.append(sep);
            sb.append("" + v);
            sep = ", ";
        }
        
        sb.append(")");
        return sb.toString();
    }

    public static String c(double[] values) throws Exception
    {
        StringBuilder sb = new StringBuilder("c(");
        String sep = "";
        
        for (double v : values)
        {
            sb.append(sep);
            sb.append(Util.f2s(v));
            sep = ", ";
        }
        
        sb.append(")");
        return sb.toString();
    }
    
    public static Map<String,String> keysFromReply(String reply, String[] keys) throws Exception
    {
        Map<String,String> m = new HashMap<>();
        
        for (String line : reply.replace("\r", "").split("\n"))
        {
            line = line.trim();
            for (String key : keys)
            {
                String ks = key + ": ";
                if (line.startsWith(ks))
                {
                    String value = line.substring(ks.length());
                    value = value.trim();
                    if (m.containsKey(key))
                        throw new Exception("Duplicate response key: " + key);
                    m.put(key,  value);
                }
            }
        }
        
        for (String key : keys)
        {
            if (!m.containsKey(key))
                throw new Exception("Response missing key: " + key);
        }
        
        return m;
    }
    
    /*
     * Parse named vector, like this:
     * "A:0.023015645452356,B:0.0525300825459925,C:0.257545159036299,D:2.62975794141521e+22"
     * "0:3.24779361199068e-05,1:-7.57230761681375e-05,5:5.22042572978099e-05,10:-6.83018193803524e-05" 
     */
    public static LinkedHashMap<String,Double> namedVectorSD(String vecs) throws Exception
    {
        LinkedHashMap<String,Double> m = new LinkedHashMap<String,Double>();

        for (String kvs : vecs.trim().split(","))
        {
            String[] s = kvs.split(":");
            if (s.length != 2)
                throw new Exception("Unable to parse named vector");
            m.put(s[0], Double.parseDouble(s[1]));
        }
        
        return m;
    }
}
