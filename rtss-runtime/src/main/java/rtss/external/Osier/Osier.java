package rtss.external.Osier;

import rtss.config.Config;

/**
 * Execute R commands
 */
public class Osier
{
    private static OsierCall ocall;
    
    public static synchronized String execute(String s, boolean reuse) throws Exception
    {
        String response = ocall().execute(s, reuse);
        if (response == null)
            throw new Exception("No reply from Osier");
        return response;
    }
    
    public static synchronized void stop() throws Exception
    {
        if (ocall!= null)
            ocall.stop();
        ocall = null;
    }
    
    public static synchronized String ping(String tag) throws Exception
    {
        return ocall().ping(tag);
    }
    
    private static synchronized OsierCall ocall() throws Exception
    {
        if (ocall == null)
        {
            String endpoint = Config.asString("Osier.server.endpoint", "");
            endpoint = endpoint.trim();
            if (endpoint.length() == 0)
            {
                ocall = new OsierLocal().setLog(Config.asBoolean("Osier.server.log", false));                 
            }
            else
            {
                ocall = new OsierClient();
            }
        }
        
        return ocall;
    }
    
    private static final String LINE =  "==================================";
    static String BEGIN_SCRIPT = LINE + " BEGIN SCRIPT EXECUTION " + LINE;
    static String END_SCRIPT = LINE + " END OF SCRIPT EXECUTION " + LINE;

}