package rtss.util;

public class XY<T> 
{
    public T x;
    public T y;
    
    public XY()
    {
    }

    public XY(T x, T y)
    {
        this.x = x;
        this.y = y;
    }
    
    public static <T> XY<T> of(T x, T y)
    {
        return new XY<T>(x, y);
    }
}
