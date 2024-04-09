package rtss.data.population;

import rtss.data.population.forward.PopulationForwardingContext;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;

public class RescalePopulation
{
    private static final int MAX_AGE = PopulationByLocality.MAX_AGE;
    
    /*
     * Перемасштабировать население указанного пола @gender для точного совпадения с численностью задаваемой @target.
     * Для населений не имеющих сельско-городской разбивки.
     * Результат помещается в @pto.
     * Для @pto затем нужно применить makeBoth().
     */
    public static void scaleTotal(PopulationByLocality pto, PopulationByLocality p, Gender gender, double target) throws Exception
    {
        scaleTotal(pto, p, null, gender, target);
    }
    
    /*
     * Перемасштабировать население указанного пола @gender для точного совпадения с численностью задаваемой @target.
     * Для населений не имеющих сельско-городской разбивки.
     * Население находится в @p и в детском контексте @fctx.
     * Результат помещается в @pto и в @fctx.
     * Для @pto затем нужно применить makeBoth().
     */
    public static void scaleTotal(PopulationByLocality pto, PopulationByLocality p, PopulationForwardingContext fctx, Gender gender, double target) throws Exception
    {
        if (p.hasRuralUrban() || pto.hasRuralUrban())
            throw new IllegalArgumentException();
        
        final Locality locality = Locality.TOTAL;
        
        double v = p.sum(locality, gender, 0, MAX_AGE);
        if (fctx != null)
            v += fctx.sumAges(locality, gender, 0, fctx.MAX_YEAR);

        final double scale = target / v;
        
        for (int age = 0; age <= MAX_AGE; age++)
        {
            v = p.get(locality, gender, age);
            pto.set(locality, gender, age, v * scale);
        }
        
        if (fctx != null)
        {
            for (int nd = 0; nd <= fctx.MAX_DAY; nd++)
            {
                v = fctx.get(locality, gender, nd);
                fctx.set(locality, gender, nd, v * scale);
            }
        }
    }
    
    public static PopulationByLocality scaleTotal(PopulationByLocality p, double males, double females) throws Exception
    {
        return scaleTotal(p, null, males, females);
    }

    public static PopulationByLocality scaleTotal(PopulationByLocality p, PopulationForwardingContext fctx, double males, double females) throws Exception
    {
        if (p.hasRuralUrban())
            throw new IllegalArgumentException();
        PopulationByLocality pto = PopulationByLocality.newPopulationTotalOnly();
        scaleTotal(pto, p, fctx, Gender.MALE, males);
        scaleTotal(pto, p, fctx, Gender.FEMALE, females);
        pto.makeBoth(Locality.TOTAL);
        return pto;
    }
}
