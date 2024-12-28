package rtss.ww2losses.old;

import rtss.data.selectors.BirthDeath;
import rtss.ww2losses.params.AreaParameters;
import rtss.util.Util;

public class Defactor extends EvaluatePopulationLossBase
{
    public Defactor(AreaParameters params)
    {
        super(params);
    }

    public void evaluate()
    {
        defactorBirthRate(34.6, 20.7);
        defactorBirthRate(34.6, 19.3);
        defactorBirthRate(34.6, 19.0);
    }
    
    private void defactorBirthRate(double cbr_1940, double cbr_1940_1944)
    {
        double f_1940_1944 = promille2factor(cbr_1940_1944, BirthDeath.BIRTH);
        double f5 = Math.pow(f_1940_1944, 5);
        
        double f_1940 = promille2factor(cbr_1940, BirthDeath.BIRTH);
        double f4 = f5 / f_1940;
        
        double f = Math.pow(f4, 1.0 / 4);
        double p = factor2promille(f, BirthDeath.BIRTH);
        
        double p_arith = (5 * cbr_1940_1944 - cbr_1940) / 4;
        
        Util.out(String.format("Defactor 1940 %.1f from 1940-1940-1944 %.1f => average for 1941-1944 is %.2f, arithmetic average is %.2f", 
                               cbr_1940, cbr_1940_1944, p, p_arith));
    }
}
