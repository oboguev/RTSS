package rtss.data.population.calc;

import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

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
    public static void scaleTotal(PopulationByLocality pto, PopulationByLocality p, PopulationContext fctx, Gender gender, double target)
            throws Exception
    {
        if (p.hasRuralUrban() || pto.hasRuralUrban())
            throw new IllegalArgumentException();

        final Locality locality = Locality.TOTAL;

        double v = p.sum(locality, gender, 0, MAX_AGE);
        if (fctx != null)
            v += fctx.sumAllAges(locality, gender);

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
                v = fctx.getDay(locality, gender, nd);
                fctx.setDay(locality, gender, nd, v * scale);
            }
        }
    }

    public static PopulationByLocality scaleTotal(PopulationByLocality p, double males, double females) throws Exception
    {
        return scaleTotal(p, null, males, females);
    }

    public static PopulationContext scaleTotal(final PopulationContext fctx, double males, double females) throws Exception
    {
        PopulationByLocality p = PopulationByLocality.newPopulationTotalOnly();
        PopulationByLocality pto = PopulationByLocality.newPopulationTotalOnly();

        PopulationContext r_fctx = fctx.clone();
        scaleTotal(pto, p, r_fctx, Gender.MALE, males);
        scaleTotal(pto, p, r_fctx, Gender.FEMALE, females);
        return r_fctx;
    }

    public static PopulationByLocality scaleTotal(PopulationByLocality p, PopulationContext fctx, double males, double females) throws Exception
    {
        if (p.hasRuralUrban())
            throw new IllegalArgumentException();
        PopulationByLocality pto = PopulationByLocality.newPopulationTotalOnly();
        scaleTotal(pto, p, fctx, Gender.MALE, males);
        scaleTotal(pto, p, fctx, Gender.FEMALE, females);
        pto.makeBoth(Locality.TOTAL);
        return pto;
    }
    
    /* ================================================================================================================= */

    /*
     * Перемасштабировать городское, сельское и суммарное население пропорционально имеющемуся таким образом,
     * чтобы общее население равнялось @target
     */
    public static PopulationByLocality scaleAllTo(PopulationByLocality p, double target) throws Exception
    {
        double scale = target / p.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);
        return scaleAllBy(p, scale);
    }

    public static PopulationByLocality scaleAllBy(PopulationByLocality p, double scale) throws Exception
    {
        Population rural = scaleBy(p.forLocality(Locality.RURAL), scale);
        Population urban = scaleBy(p.forLocality(Locality.URBAN), scale);
        Population total = scaleBy(p.forLocality(Locality.TOTAL), scale);

        return new PopulationByLocality(total, urban, rural);
    }

    public static Population scaleAllTo(Population p, double target) throws Exception
    {
        double scale = target / p.sum(Gender.BOTH, 0, MAX_AGE);
        return scaleBy(p, scale);
    }

    public static Population scaleBy(Population p, double scale) throws Exception
    {
        if (p == null)
            return null;

        double[] m = p.asArray(Gender.MALE);
        m = Util.multiply(m, scale);

        double[] f = p.asArray(Gender.FEMALE);
        f = Util.multiply(f, scale);

        double m_unknown = scale * p.getUnknown(Gender.MALE);
        double f_unknown = scale * p.getUnknown(Gender.FEMALE);

        return new Population(p.locality(),
                              m, m_unknown, p.valueConstraint(Gender.MALE),
                              f, f_unknown, p.valueConstraint(Gender.FEMALE));
    }

    public static Population scaleTo(Population p, double new_amount) throws Exception
    {
        if (p == null)
            return null;

        double old_amount = p.sum(Gender.BOTH, 0, MAX_AGE) + p.getUnknown(Gender.BOTH);
        return scaleBy(p, new_amount / old_amount);
    }

    public static PopulationContext scaleAllTo(PopulationContext p, double target) throws Exception
    {
        double scale = target / p.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);
        return scaleAllBy(p, scale);
    }

    public static PopulationContext scaleAllBy(PopulationContext p, double scale) throws Exception
    {
        if (p == null)
            return null;
        else
            return p.scaleAllBy(scale);
    }

    public static PopulationContext scaleBy(PopulationContext p, double scale) throws Exception
    {
        if (p == null)
            return null;
        else
            return p.scaleAllBy(scale);
    }

    /* ================================================================================================================= */
    
    /*
     * Для одного указанного года возраста
     */
    public static PopulationByLocality scaleYearAgeAllTo(PopulationByLocality p, int age, double target) throws Exception
    {
        double scale = target / p.sum(Locality.TOTAL, Gender.BOTH, age, age);
        return scaleYearAgeAllBy(p, age, scale);
    }

    public static Population scaleYearAgeAllTo(Population p, int age, double target) throws Exception
    {
        double scale = target / p.sum(Gender.BOTH, age, age);
        return scaleYearAgeBy(p, age, scale);
    }

    public static PopulationByLocality scaleYearAgeAllBy(PopulationByLocality p, int age, double scale) throws Exception
    {
        Population rural = scaleYearAgeBy(p.forLocality(Locality.RURAL), age, scale);
        Population urban = scaleYearAgeBy(p.forLocality(Locality.URBAN), age, scale);
        Population total = scaleYearAgeBy(p.forLocality(Locality.TOTAL), age, scale);

        return new PopulationByLocality(total, urban, rural);
    }

    public static Population scaleYearAgeBy(Population p, int age, double scale) throws Exception
    {
        if (p == null)
            return null;

        double[] m = p.asArray(Gender.MALE);
        m[age] *= scale;

        double[] f = p.asArray(Gender.FEMALE);
        f[age] *= scale;

        double m_unknown = p.getUnknown(Gender.MALE);
        double f_unknown = p.getUnknown(Gender.FEMALE);

        return new Population(p.locality(),
                              m, m_unknown, p.valueConstraint(Gender.MALE),
                              f, f_unknown, p.valueConstraint(Gender.FEMALE));
    }
}
