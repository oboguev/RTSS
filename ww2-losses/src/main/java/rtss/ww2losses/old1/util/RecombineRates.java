package rtss.ww2losses.old1.util;

import rtss.data.selectors.BirthDeath;
import rtss.ww2losses.old1.EvaluatePopulationLossBase;
import rtss.ww2losses.params.AreaParameters;
import rtss.util.Util;

public class RecombineRates extends EvaluatePopulationLossBase
{
    public RecombineRates (AreaParameters params)
    {
        super(params);
    }

    public void evaluate() throws Exception
    {
        evalx("Variant 1 birth rate", BirthDeath.BIRTH, params.constant_cbr);
        evalx("Variant 1 death rate", BirthDeath.DEATH, params.constant_cdr);
        
        evalx("Variant 2 birth rate", BirthDeath.BIRTH, params.var_cbr);
        evalx("Variant 2 death rate", BirthDeath.DEATH, params.var_cdr);
    }
    
    private void evalx(String title, BirthDeath bd, double rate)  throws Exception
    {
        double[] rates = {rate, rate, rate, rate};
        evalx(title, bd, rates);
    }
    
    private void evalx(String title, BirthDeath bd, double[] rate)  throws Exception
    {
        double[] rates = new double[10];
        
        int ix = 0;
        
        switch (bd)
        {
        case BIRTH:
            rates[ix++] = params.CBR_1940;
            break;
        case DEATH:
            rates[ix++] = params.CDR_1940;
            break;
        }

        rates[ix++] = rate[0];
        rates[ix++] = rate[0];
        
        rates[ix++] = rate[1];
        rates[ix++] = rate[1];

        rates[ix++] = rate[2];
        rates[ix++] = rate[2];
        
        rates[ix++] = rate[3];
        rates[ix++] = rate[3];
        
        switch (bd)
        {
        case BIRTH:
            rates[ix++] = params.CBR_1946;
            break;
        case DEATH:
            rates[ix++] = params.CDR_1946;
            break;
        }
        
        eval(title, rates);
        
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
