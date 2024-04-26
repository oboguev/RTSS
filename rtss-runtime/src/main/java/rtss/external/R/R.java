package rtss.external.R;

public class R
{
    private static RLocal rl = new RLocal().setLog(true); 
    
    public static String execute(String s, boolean reuse) throws Exception
    {
        return rl.execute(s, reuse);
    }
    
    private static final String LINE =  "==================================";
    static String BEGIN_SCRIPT = LINE + " BEGIN SCRIPT EXECUTION " + LINE;
    static String END_SCRIPT = LINE + " END OF SCRIPT EXECUTION " + LINE;
}
