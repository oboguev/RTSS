package rtss.util;

public class Loggable
{
    protected boolean debug = Util.False;
    public StringBuilder log = new StringBuilder();
    
    public void debug(boolean debug)
    {
        this.debug = debug;
    }
    
    protected void log(String s)
    {
        if (debug)
        {
            log.append(s);
            log.append(Util.nl);
        }
    }
    
    public void clearLog()
    {
        log.setLength(0);
    }

    public String log()
    {
        String s = log.toString();
        log.setLength(0);
        return s;
    }
}
