package rtss.ww2losses.population194x;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.synthetic.InterpolateMortalityTable;
import rtss.data.population.calc.RescalePopulation;
import rtss.data.population.projection.ForwardPopulationT;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.population.struct.PopulationContext;
import rtss.data.population.synthetic.PopulationADH;
import rtss.forward_1926_193x.Adjust_1939;
import rtss.ww2losses.params.AreaParameters;

/**
 * Вычислить возрастную структуру населения СССР или РСФСР на начало 1940 года
 */
public class Population_In_Early_1940 extends UtilBase_194x
{
    private CombinedMortalityTable cmt;
    private AreaParameters ap;

    public Population_In_Early_1940(AreaParameters ap) throws Exception
    {
        this.ap = ap;
    }

    public PopulationByLocality evaluate() throws Exception
    {
        PopulationContext fctx = new PopulationContext();
        PopulationByLocality p = evaluate(fctx);
        return fctx.end(p);
    }

    /*
     * Оставляет контекст незакрытым, позволяя дальнейшую передвижку
     */
    public PopulationByLocality evaluate(PopulationContext fctx) throws Exception
    {
        if (useADH)
        {
            PopulationByLocality p = PopulationADH.getPopulationByLocality(ap.area, 1940);
            return fctx.begin(p);
        }
        else
        {
            /*
             * Загрузить структуру населения по переписи 1939 года
             */
            PopulationByLocality p = PopulationByLocality.census(ap.area, 1939);
            p.resetUnknownForEveryLocality();
            p.recalcTotalForEveryLocality();
            p = p.smooth(true);
            p = new Adjust_1939().adjust(ap.area, p);
            p.validate();
            p = p.cloneTotalOnly();

            /*
             * Перемасштабировать на начало 1939 года и границы 1946 года
             */
            p = RescalePopulation.scaleTotal(p, null, ap.ADH_MALES_1939, ap.ADH_FEMALES_1939);
            show_struct("начало 1939 в границах 1946", p, fctx);

            /*
             * Передвижка с начала 1939 до начала 1940 года
             */
            ForwardPopulationT fw = new ForwardPopulationT();
            p = fctx.begin(p);
            fw.setBirthRateTotal(ap.CBR_1939);
            double yfraction = 1.0;
            p = forward(fw, p, fctx, yfraction, ap.CDR_1939);

            /*
             * Перемасштабировать для точного совпадения общей численности полов с расчётом АДХ
             */
            PopulationByLocality pto = RescalePopulation.scaleTotal(p, fctx, ap.ADH_MALES_1940, ap.ADH_FEMALES_1940);
            pto.validate();
            show_struct("начало 1940", pto, fctx);

            return pto;
        }
    }

    private PopulationByLocality forward(
            ForwardPopulationT fw,
            PopulationByLocality p,
            PopulationContext fctx,
            double yfraction,
            double cdr)
            throws Exception
    {
        CombinedMortalityTable mt1;
        
        switch (ap.area)
        {
        case USSR:
        case RSFSR:
            mt1 = CombinedMortalityTable.load("mortality_tables/USSR/1938-1939");
            mt1.comment("ГКС-СССР-1938");
            break;
            
        default:
            throw new IllegalArgumentException();
        }
        
        CombinedMortalityTable mt2 = CombinedMortalityTable.loadTotal("mortality_tables/RSFSR/1940");
        mt2.comment("АДХ-РСФСР-1940");

        cmt = InterpolateMortalityTable.forTargetRates(mt1, mt2, p, fctx, fw.getBirthRateTotal(), cdr, 4, null);

        return fw.forward(p, fctx, cmt, yfraction);
    }
}
