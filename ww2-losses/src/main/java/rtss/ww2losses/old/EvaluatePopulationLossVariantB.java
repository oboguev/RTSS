package rtss.ww2losses.old;

import rtss.ww2losses.params.AreaParameters;

public class EvaluatePopulationLossVariantB extends EvaluatePopulationLossBase
{
    public EvaluatePopulationLossVariantB(AreaParameters params)
    {
        super(params);
    }

    public void evaluate() throws Exception
    {
        init();
        
        for (Year y : years)
        {
            y.actual.deaths = y.expected.deaths + params.ACTUAL_EXCESS_DEATHS / NYears;
        }
        
        double cbr1 = 0;
        double cbr2 = params.CBR_1940;

        for (;;)
        {
            double cbr = (cbr1 + cbr2) / 2;
            calcActual(cbr);
            double bd = birthsDeficit();

            if (Math.abs(bd - params.ACTUAL_BIRTH_DEFICIT) < 0.01)
            {
                break;
            }
            
            if (bd < params.ACTUAL_BIRTH_DEFICIT)
            {
                /* cbr was too high */
                cbr2 = cbr;
            }
            else
            {
                cbr1 = cbr;
            }
        }

        for (Year y : years)
        {
            params.var_cbr[y.nyear] = 1000 * y.actual.births / y.actual.start;
            params.var_cdr[y.nyear] = 1000 * y.actual.deaths / y.actual.start;
        }

        print(true);
    }
    
    private void calcActual(double cbr)
    {
        for (Year y : years)
        {
            Population p = y.actual;
            p.births = p.start * cbr / 1000.0;
            p.end = p.start + p.births - p.deaths;
            p.mid = Math.sqrt(p.start * p.end);
            if (y.next != null)
                y.next.actual.start = p.end;
        }
    }
}
