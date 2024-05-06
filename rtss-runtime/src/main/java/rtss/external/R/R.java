package rtss.external.R;

import java.util.LinkedHashMap;

import rtss.config.Config;
import rtss.external.ShutdownHook;
import rtss.external.Osier.Osier;
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

            ShutdownHook.add(R::do_stop);
        }
        
        return rcall;
    }
    
    private static void do_stop()
    {
        try
        {
            stop();
        }
        catch (Exception ex)
        {
            Util.noop();
        }
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
    
    /*
     * Parse named vector, like this:
     * "A:0.023015645452356,B:0.0525300825459925,C:0.257545159036299,D:2.62975794141521e+22"
     * "0:3.24779361199068e-05,1:-7.57230761681375e-05,5:5.22042572978099e-05,10:-6.83018193803524e-05" 
     */
    public static LinkedHashMap<String,Double> namedVectorSD(String vecs) throws Exception
    {
        LinkedHashMap<String,Double> m = new LinkedHashMap<>();

        for (String kvs : vecs.trim().split(","))
        {
            String[] s = kvs.split(":");
            if (s.length != 2)
                throw new Exception("Unable to parse named vector");
            m.put(s[0], Double.parseDouble(s[1]));
        }
        
        return m;
    }

    /*
     * Parse named vector integer -> double
     * "1:0.023015645452356,2:0.0525300825459925,3:0.257545159036299,4:2.62975794141521e+22"
     */    
    public static LinkedHashMap<Integer,Double> namedVectorID(String vecs) throws Exception
    {
        LinkedHashMap<Integer,Double> m = new LinkedHashMap<>();

        for (String kvs : vecs.trim().split(","))
        {
            String[] s = kvs.split(":");
            if (s.length != 2)
                throw new Exception("Unable to parse named vector");
            m.put(Integer.parseInt(s[0]), Double.parseDouble(s[1]));
        }
        
        return m;
    }
    
    /*
     * Parse non-sparse named vector integer -> double, starting from index 1
     */    
    public static double[] indexedVectorD(String vecs) throws Exception
    {
        Integer imin = null;
        Integer imax = null;
        
        LinkedHashMap<Integer,Double> m = namedVectorID(vecs);
        
        for (Integer i : m.keySet())
        {
            if (imin == null)
                imin = i;
            else 
                imin = Math.min(imin, i);

            if (imax == null)
                imax = i;
            else 
                imax = Math.max(imax, i);
        }
        
        if (imin == null || imax == null || imin != 1)
            throw new Exception("Invalid indexed vector");
        
        double[] d = new double[imax];
        
        for (int i = imin; i <= imax; i++)
        {
            Double v = m.get(i);
            if (v == null)
                throw new Exception("Missing element in an indexed vector");
            d[i - 1] = v;
        }
        
        return d;
    }
}
