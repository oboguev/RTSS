package rtss.pre1917.calc.by_rate;

import rtss.util.Util;

public class ByGrowthRate extends ByRateBase
{
    public static void main(String[] args)
    {
        try
        {
            new ByGrowthRate().eval();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private ByGrowthRate() throws Exception
    {
    }

    @Override
    double rate(double cbr, double cdr)
    {
        return cbr - cdr;
    }
}
