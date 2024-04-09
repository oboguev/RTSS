package rtss.ww2losses.population_194x;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.EvalMortalityRate;
import rtss.data.population.PopulationByLocality;
import rtss.data.population.forward.ForwardPopulationT;
import rtss.data.population.forward.PopulationForwardingContext;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

/**
 * Вычислить возрастную структуру населения СССР на начало 1940 года
 */
public class USSR_Population_In_Early_1940
{
    /*
     * AДХ, "Население Советского Союза", стр. 120
     */
    public double CBR_1939 = 40.0;
    public double CDR_1939 = 20.1;

    private static final int MAX_AGE = PopulationByLocality.MAX_AGE;

    private CombinedMortalityTable cmt;

    public USSR_Population_In_Early_1940() throws Exception
    {
    }

    public PopulationByLocality evaluate() throws Exception
    {
        PopulationForwardingContext fctx = new PopulationForwardingContext();
        PopulationByLocality p = evaluate(fctx);
        return fctx.end(p);
    }

    /*
     * Оставляет контекст незакрытым, позволяя дальнейшую продвижку
     */
    public PopulationByLocality evaluate(PopulationForwardingContext fctx) throws Exception
    {
        PopulationByLocality p = PopulationByLocality.census(Area.USSR, 1939);
        p.resetUnknownForEveryLocality();
        p.recalcTotalForEveryLocality();
        p.validate();
        p = p.cloneTotalOnly();

        /*
         * Продвижка с 17 января 1939 до начала 1940 года
         */
        ForwardPopulationT fw = new ForwardPopulationT();
        fctx.begin(p);
        fw.setBirthRateTotal(CBR_1939);
        double yfraction = (365 - 16) / 365.0;
        p = forward(fw, p, fctx, yfraction, CDR_1939);

        /*
         * Перемасштабировать для точного совпадения общей численности полов с расчётом АДХ
         */
        /* АДХ, "Население Советского Союза", стр. 125-126 */
        final double males_1940 = 92_316;
        final double females_1940 = 100_283;

        PopulationByLocality pto = PopulationByLocality.newPopulationTotalOnly();
        scale(pto, p, fctx, Gender.MALE, males_1940);
        scale(pto, p, fctx, Gender.FEMALE, females_1940);
        pto.makeBoth(Locality.TOTAL);
        pto.validate();

        return pto;
    }

    private PopulationByLocality forward(
            ForwardPopulationT fw, 
            PopulationByLocality p, 
            PopulationForwardingContext fctx, 
            double yfraction, 
            double cdr)
            throws Exception
    {
        CombinedMortalityTable mt1 = CombinedMortalityTable.load("mortality_tables/USSR/1938-1939");
        double cdr1 = EvalMortalityRate.eval(mt1, p, fctx);

        CombinedMortalityTable mt2 = CombinedMortalityTable.loadTotal("mortality_tables/RSFSR/1940");
        double cdr2 = EvalMortalityRate.eval(mt2, p, fctx);

        double a = (cdr - cdr2) / (cdr1 - cdr2);
        if (a < 0) a = 0;
        if (a > 1) a = 1;
        // Util.out(String.format("комбинированная таблица: %.3f от ГКС-СССР-1938, %.3f от АДХ-РСФСР-1940", 1 - a, a));
        cmt = CombinedMortalityTable.interpolate(mt1, mt2, 1 - a);

        return fw.forward(p, fctx, cmt, yfraction);
    }

    private void scale(PopulationByLocality pto, PopulationByLocality p, PopulationForwardingContext fctx, Gender gender, double target)
            throws Exception
    {
        final Locality locality = Locality.TOTAL;
        double v = p.sum(locality, gender, 0, MAX_AGE) + fctx.sumAges(Locality.TOTAL, gender, 0, fctx.MAX_YEAR);
        final double scale = target / v;

        for (int age = 0; age <= MAX_AGE; age++)
        {
            v = p.get(locality, gender, age);
            pto.set(locality, gender, age, v * scale);
        }

        for (int nd = 0; nd <= fctx.MAX_DAY; nd++)
        {
            v = fctx.get(locality, gender, nd);
            fctx.set(locality, gender, nd, v * scale);
        }
    }
}
