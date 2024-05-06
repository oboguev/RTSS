package rtss.external;

/**
 * Invoke @c when application is terminated 
 */
public class ShutdownHook extends Thread
{
    public static void add(Runnable c) 
    {
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(c));
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
