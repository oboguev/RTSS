package rtss.external.Osier;

/**
 * Interface for local and remote calls of Osier
 */
public interface OsierCall
{
    public void setDefaultStartupScript(boolean visible) throws Exception;
    public void setStartupScript(String sc) throws Exception;
    public String execute(String s, boolean reuse) throws Exception;
    public void stop() throws Exception;
    public String ping(String tag) throws Exception;
}