package rtss.ww2losses;

public class EvaluatePopulationLossVariantB extends EvaluatePopulationLossBase
{
    public void evaluate() throws Exception
    {
        init();
        
        for (Year y : years)
        {
            y.actual.deaths = y.expected.deaths + ACTUAL_EXCESS_DEATHS / NYears;
        }
        
        double cbr1 = 0;
        double cbr2 = CBR_1940;

        for (;;)
        {
            double cbr = (cbr1 + cbr2) / 2;
            calcActual(cbr);
            double bd = birthsDeficit();

            if (Math.abs(bd - ACTUAL_BIRTH_DEFICIT) < 0.05)
            {
                print(true);
                return;
            }
            
            if (bd < ACTUAL_BIRTH_DEFICIT)
            {
                /* cbr was too high */
                cbr2 = cbr;
            }
            else
            {
                cbr1 = cbr;
            }
        }
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
