package rtss.ww2losses.population_194x;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.EvalMortalityRate;
import rtss.data.population.PopulationByLocality;
import rtss.data.population.forward.ForwardPopulationT;
import rtss.data.population.forward.PopulationForwardingContext;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.ww2losses.params.AreaParameters;

/**
 * Вычислить возрастную структуру населения СССР на середину 1941 года
 */
public class USSR_Population_In_Middle_1941
{
    /*
     * AДХ, "Население Советского Союза", стр. 120
     */
    public double CBR_1939 = 40.0;
    public double CDR_1939 = 20.1;
    
    public double CBR_1940 = 36.1;
    public double CDR_1940 = 21.7;
    
    private static final int MAX_AGE = PopulationByLocality.MAX_AGE;
    
    private CombinedMortalityTable cmt;
    
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
        CombinedMortalityTable mt1938 = new CombinedMortalityTable("mortality_tables/USSR/1938-1939");

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
        p = fw.forward(p, fctx, mt1938, yfraction);
        
        /*
         * Продвижка с начала 1940 до начала 1941 года
         */
        fw.setBirthRateTotal(CBR_1940);
        p = fw.forward(p, fctx, mt1938, 1.0);
        
        /*
         * Продвижка с начала 1941 до середины 1941 года
         */
        fw.setBirthRateTotal(CBR_1940);
        p = fw.forward(p, fctx, mt1938, 0.5);
        
        /*
         * Перемасштабировать для точного совпадения общей численности полов с расчётом АДХ
         */
        final double USSR_1941_START = 195_392; // АДХ, "Население Советского Союза", стр. 77, 118, 126
        final double USSR_1941_MID = forward_6mo(USSR_1941_START, AreaParameters.forArea(Area.USSR, 4).growth_1940());

        /* АДХ, "Население Советского Союза", стр. 56, 74 */
        final double males_jun21 = 94_338; 
        final double females_jun21 = 102_378;
        final double total_jun21 = males_jun21 + females_jun21;
        
        final double females_mid1941 = females_jun21 * USSR_1941_MID / total_jun21; 
        final double males_mid1941 = males_jun21 * USSR_1941_MID / total_jun21; 
        
        PopulationByLocality pto = PopulationByLocality.newPopulationTotalOnly();
        scale(pto, p, fctx, Gender.FEMALE, females_mid1941);
        scale(pto, p, fctx, Gender.MALE, males_mid1941);
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

    private double forward_6mo(double v, double rate)
    {
        double f = Math.sqrt(1 + rate / 1000);
        return v * f;
    }
    
    private void scale(PopulationByLocality pto, PopulationByLocality p, PopulationForwardingContext fctx, Gender gender, double target) throws Exception
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
