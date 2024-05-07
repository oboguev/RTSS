package rtss.external;

/**
 * Invoke @c when application is terminated 
 */
public class ShutdownHook extends Thread
{
    public static ShutdownHook add(Runnable c) 
    {
        ShutdownHook hook = new ShutdownHook(c);
        Runtime.getRuntime().addShutdownHook(hook);
        return hook;
    }
    
    /* ================================================= */
    
    private Runnable c; 
    private ShutdownHook(Runnable c)
    {
        this.c = c;
    }
    
    @Override
    public void run()
    {
        c.run();
    }
}
