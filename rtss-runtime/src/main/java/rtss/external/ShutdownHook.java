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
    
    public void remove()
    {
        Runtime.getRuntime().removeShutdownHook(this);
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
