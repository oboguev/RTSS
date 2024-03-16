package rtss.ww2losses;

public class RecombineRates extends EvaluatePopulationLossBase
{
    public void evaluate() throws Exception
    {
        eval("Variant 1 birth rate", 34.60, 13.39, 13.39, 13.39, 13.39, 13.39, 13.39, 13.39, 13.39, 26.00);
        eval("Variant 1 death rate", 23.20, 47.50, 47.50, 47.50, 47.50, 47.50, 47.50, 47.50, 47.50, 12.30);
        
        eval("Variant 2 birth rate", 34.60, 13.35, 13.35, 13.35, 13.35, 13.35, 13.35, 13.35, 13.35, 26.00);
        eval("Variant 2 death rate", 23.20, 44.72, 44.72, 46.44, 46.44, 48.32, 48.32, 50.37, 50.37, 12.30);
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
}
