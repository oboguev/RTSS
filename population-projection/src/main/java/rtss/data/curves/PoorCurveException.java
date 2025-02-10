package rtss.data.curves;

/**
 * Could not fit a good curve
 */
public class PoorCurveException extends Exception
{
    private static final long serialVersionUID = 1L;

    public PoorCurveException()
    {
    }

    public PoorCurveException(String msg)
    {
        super(msg);
    }

    public PoorCurveException(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}
