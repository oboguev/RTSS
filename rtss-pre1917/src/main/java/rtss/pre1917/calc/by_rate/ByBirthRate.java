package rtss.pre1917.calc.by_rate;

import rtss.util.Util;

public class ByBirthRate extends ByRateBase 
{
    public static void main(String[] args)
    {
        try
        {
            new ByBirthRate().eval();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }
    
    private ByBirthRate() throws Exception
    {
    }

    @Override
    double rate(double cbr, double cdr)
    {
        return cbr;
    }
}
