package rtss.ww2losses.population_194x;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.EvalMortalityRate;
import rtss.data.population.PopulationByLocality;
import rtss.data.population.RescalePopulation;
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
         * Перемасштабировать на начало 1939 года и границы 1946 года
         */
        /* АДХ, "Население Советского Союза", стр. 131 */
        final double males_1939 = 90_013_000;
        final double females_1939 = 98_194_000;
        p = RescalePopulation.scaleTotal(p, fctx, males_1939, females_1939);
        show_struct("начало 1939 в границах 1946", p, fctx);

        /*
         * Продвижка с начала 1939 до начала 1940 года
         */
        ForwardPopulationT fw = new ForwardPopulationT();
        p = fctx.begin(p);
        fw.setBirthRateTotal(CBR_1939);
        double yfraction = 1.0;
        p = forward(fw, p, fctx, yfraction, CDR_1939);

        /*
         * Перемасштабировать для точного совпадения общей численности полов с расчётом АДХ
         */
        /* АДХ, "Население Советского Союза", стр. 125-126 */
        final double males_1940 = 92_316_000;
        final double females_1940 = 100_283_000;
        PopulationByLocality pto = RescalePopulation.scaleTotal(p, fctx, males_1940, females_1940);
        pto.validate();
        show_struct("начало 1940", pto, fctx);

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
    
    private void show_struct(String what, PopulationByLocality p, PopulationForwardingContext fctx) throws Exception
    {
        if (Util.False)
        {
            p = fctx.end(p);

            String struct = p.ageStructure(PopulationByLocality.STRUCT_0459, Locality.TOTAL, Gender.MALE);
            Util.out("");
            Util.out(">>> " + what + " male");
            Util.out(struct);

            struct = p.ageStructure(PopulationByLocality.STRUCT_0459, Locality.TOTAL, Gender.FEMALE);
            Util.out("");
            Util.out(">>> " + what + " female");
            Util.out(struct);
        }
    }
}
