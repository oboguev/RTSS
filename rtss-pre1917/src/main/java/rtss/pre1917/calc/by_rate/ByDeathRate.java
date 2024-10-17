package rtss.pre1917.calc.by_rate;

import rtss.util.Util;

public class ByDeathRate extends ByRateBase
{
    public static void main(String[] args)
    {
        try
        {
            new ByDeathRate().eval();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }
    
    private ByDeathRate() throws Exception
    {
    }

    @Override
    double rate(double cbr, double cdr)
    {
        return cdr;
    }
}
