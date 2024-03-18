package rtss.ww2losses;

import rtss.ww2losses.util.Util;

public class EvaluatePopulationLossVariantA extends EvaluatePopulationLossBase
{
    public EvaluatePopulationLossVariantA(AreaParameters params)
    {
        super(params);
    }
    
    public void evaluate() throws Exception
    {
        init();
        
        double cbr1 = 0;
        double cbr2 = params.CBR_1940;
        
        final double target_b2d =  params.ACTUAL_BIRTH_DEFICIT /  params.ACTUAL_EXCESS_DEATHS;

        for (;;)
        {
            double cbr = (cbr1 + cbr2) / 2;
            double cdr = cbr - actualGrowthPromille;

            calcActual(cbr, cdr);
            // print(false);
            double excessDeaths = excessDeaths();
            double birthsDeficit = birthsDeficit();
            
            double b2d = birthsDeficit / excessDeaths;
            
            if (Math.abs(excessDeaths - params.ACTUAL_EXCESS_DEATHS) < 0.04)
            {
                print(false);
                Util.out(String.format("Birth rate: %.2f", cbr));
                Util.out(String.format("Death rate: %.2f", cdr));
                
                params.constant_cbr = cbr;
                params.constant_cdr = cdr;
                
                return;
            }
            
            if (b2d > target_b2d)
            {
                /*
                 * Birth deficit is too high, cbr was too low
                 */
                cbr1 = cbr;
            }
            else
            {
                cbr2 = cbr;
            }
            
            Util.noop();
        }
    }

    /*
     * Calculate actual population assuming given constant cbr and cdr (promille)
     */
    private void calcActual(double cbr, double cdr)
    {
        for (Year y : years)
        {
            Population p = y.actual;
            p.births = p.start * cbr / 1000.0;
            p.deaths = p.start * cdr / 1000.0;
            p.end = p.start + p.births - p.deaths;
            p.mid = Math.sqrt(p.start * p.end);
            if (y.next != null)
                y.next.actual.start = p.end; 
        }
    }
}
