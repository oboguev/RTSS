package my;

public class EvaluatePopulationLossVariantA extends EvaluatePopulationLossBase
{
    public void evaluate() throws Exception
    {
        init();
        
        double cbr1 = 0;
        double cbr2 = CBR_1940;
        
        for (;;)
        {
            double cbr = (cbr1 + cbr2) / 2;
            double cdr = cbr - actualGrowthPromille;

            calcActual(cbr, cdr);
            print();
            double excessDeaths = excessDeaths();
            double birthsDeficit = birthsDeficit();
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
