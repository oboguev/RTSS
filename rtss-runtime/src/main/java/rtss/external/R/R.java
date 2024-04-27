package rtss.external.R;

/**
 * Execute R scripts
 */
public class R
{
    private static RLocal rlocal;
    
    public static synchronized String execute(String s, boolean reuse) throws Exception
    {
        if (rlocal == null)
            rlocal = new RLocal().setLog(true); 
            
        String response = rlocal.execute(s, reuse);
        if (response == null)
            throw new Exception("No reply from R");
        return response;
    }
    
    public static synchronized void stop() throws Exception
    {
        if (rlocal != null)
            rlocal.stop();
        rlocal = null;
    }
    
    public static synchronized String ping(String tag)
    {
        if (rlocal == null)
            rlocal = new RLocal().setLog(true);
        return rlocal.ping(tag);
    }
    
    private static final String LINE =  "==================================";
    static String BEGIN_SCRIPT = LINE + " BEGIN SCRIPT EXECUTION " + LINE;
    static String END_SCRIPT = LINE + " END OF SCRIPT EXECUTION " + LINE;
}
