package rtss.math.interpolate;

import rtss.util.Util;

public class LinearInterpolator
{
    private final double a;
    private final double b;
    
    public LinearInterpolator(double x1, double y1, double x2, double y2)
    {
        if (x2 == x1)
            throw new IllegalArgumentException();
        
        a = (y2 - y1) / (x2 - x1);
        b = y2 - a * x2;
        
       if (!Util.isValid(a))
           throw new IllegalArgumentException();
    }
    
    public double interp(double x)
    {
        return a * x + b;
    }
}
