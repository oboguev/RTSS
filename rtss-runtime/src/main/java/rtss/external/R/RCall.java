package rtss.external.R;

/**
 * Interface for local and remote calls of R
 */
public interface RCall
{
    public String execute(String s, boolean reuse) throws Exception;
    public void stop() throws Exception;
    public String ping(String tag) throws Exception;
}
