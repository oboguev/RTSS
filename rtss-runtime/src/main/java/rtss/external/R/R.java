package rtss.external.R;

import rtss.config.Config;

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
}
