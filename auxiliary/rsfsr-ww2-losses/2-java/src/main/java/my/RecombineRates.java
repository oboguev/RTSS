package my;

public class RecombineRates
{
    public static enum BirthDeath
    {
        BIRTH,
        DEATH
    }
    
    public void evaluate() throws Exception
    {
        eval("Variant 1 birth rate", 34.60, 14.15, 14.15, 14.15, 14.15, 14.15, 14.15, 14.15, 14.15, 26.00);
        eval("Variant 1 death rate", 23.20, 48.26, 48.26, 48.26, 48.26, 48.26, 48.26, 48.26, 48.26, 12.30);
        eval("Variant 2 birth rate", 34.60, 14.12, 14.12, 14.12, 14.12, 14.12, 14.12, 14.12, 14.12, 26.00);
        eval("Variant 2 death rate", 23.20, 45.45, 45.45, 47.19, 47.19, 49.09, 49.09, 51.17, 51.17, 12.30);
    }
    
    private void eval(String title, double... fa) throws Exception
    {
        BirthDeath which = BirthDeath.BIRTH;
        
        boolean cb = title.toLowerCase().contains("birth");
        boolean cd = title.toLowerCase().contains("death");
        
        if (cb && cd || !cb && !cd)
            throw new Exception("Invalid title");

        if (cb)
        {
            which = BirthDeath.BIRTH;
        }
        else
        {
            which = BirthDeath.DEATH;
        }
        
        Util.out("");
        Util.out(title + ":");

        int ix = 0;
        for (int k = 1; k <= 5; k++)
        {
            double p1 = fa[ix++];
            double p2 = fa[ix++];
            double f1 = promille2factor(p1, which);
            double f2 = promille2factor(p2, which);
            double f = Math.sqrt(f1 * f2); 
            double p = factor2promille(f, which);
            
            Util.out(String.format("%4d: %.2f", 1940 + k, p));
        }
    }

    protected double factor2promille(double factor, BirthDeath which)
    {
        switch (which)
        {
        case BIRTH:
            return 1000 * (factor - 1.0); 
        case DEATH:
        default:
            return 1000 * (1.0 - factor); 
        }
    }
    
    protected double promille2factor(double promille, BirthDeath which)
    {
        switch (which)
        {
        case BIRTH:
            return 1 + promille / 1000.0;
        case DEATH:
        default:
            return 1 - promille / 1000.0;
        }
    }
}
