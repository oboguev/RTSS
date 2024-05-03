package rtss.external.Osier;

/**
 * Interface for local and remote calls of Osier
 */
public interface OsierCall
{
    public String execute(String s, boolean reuse) throws Exception;
    public void stop() throws Exception;
    public String ping(String tag) throws Exception;
}