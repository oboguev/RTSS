package rtss.data.population.forward;

import java.util.ArrayList;
import java.util.List;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.EvalMortalityRate;
import rtss.data.mortality.synthetic.PatchMortalityTable;
import rtss.data.mortality.synthetic.PatchMortalityTable.PatchInstruction;
import rtss.data.mortality.synthetic.PatchMortalityTable.PatchOpcode;
import rtss.data.population.Population;
import rtss.data.population.PopulationByLocality;
import rtss.data.population.synthetic.PopulationADH;
import rtss.data.rates.Recalibrate;
import rtss.data.rates.Recalibrate.Rates;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.forward_1926_193x.Adjust_1937;
import rtss.forward_1926_193x.Adjust_1939;
import rtss.util.Util;

/*
 * Сравнить смертность в 1937-1939 годах по передвижке с таблицей Госкомстата и по АДХ
 */
public class TestValidate_193x
{
    public static void main(String[] args)
    {
        try
        {
            new TestValidate_193x().test_1();
            new TestValidate_193x().validate_1939();
        }
        catch (Exception ex)
        {
            Util.err("*** Exception: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    }

    // ### test: forward [8-100] by 1 year via EvalMortalityRate 
    // ### test: forward [0-100] by 1 year via EvalMortalityRate 
    // ### test: forward [8-100] by 1 year vs 0.3 + 0.7 
    // ### test: forward [0-100] by 1 year vs 0.3 + 0.7 

    // ### deaths in P
    // ### decimate CTX + shift CTX
    // ### add new births to CTX
    // ### decimate births + shift new births in CTX

    public TestValidate_193x() throws Exception
    {
    }

    private static final int MAX_AGE = Population.MAX_AGE;

    private final boolean DoSmoothPopulation = Util.True;

    private PopulationByLocality p1937_original = PopulationByLocality.census(Area.USSR, 1937).smooth(DoSmoothPopulation);
    private PopulationByLocality p1937 = new Adjust_1937().adjust(p1937_original);

    private PopulationByLocality p1939_original = PopulationByLocality.census(Area.USSR, 1939).smooth(DoSmoothPopulation);
    private PopulationByLocality p1939 = new Adjust_1939().adjust(Area.USSR, p1939_original);

    /* уровень младенческой смертности в СССР в 1939 году по АДХ (АДХ-СССР, стр. 135) */
    private static final double PROMILLE = 1000.0;
    private static final double ADH_USSR_infant_CDR_1937 = 184.0 / PROMILLE;
    private static final double ADH_USSR_infant_CDR_1938 = 174.0 / PROMILLE;
    private static final double ADH_USSR_infant_CDR_1939 = 168.0 / PROMILLE;
    private static final boolean use_ADH_USSR_InfantMortalityRate = true;

    /* рождаемость и смертность в 1937-1939 гг. в нормировке на середину года (АДХ-СССР, стр. 120) */
    private double CBR_1937_MIDYEAR = 39.9;
    private double CDR_1937_MIDYEAR = 21.7;

    private double CBR_1938_MIDYEAR = 39.0;
    private double CDR_1938_MIDYEAR = 20.9;

    private double CBR_1939_MIDYEAR = 40.0;
    private double CDR_1939_MIDYEAR = 20.1;

    private final double CBR_Rural_1926 = 46.1;
    private final double CBR_Urban_1926 = 34.4;
    private final double CBR_Total_1926 = 44.0;

    private void validate_1939() throws Exception
    {
        validate_193x(1937, "перепись 1937, исправленное", p1937);
        validate_193x(1937, "перепись 1937, исправленное total-only", p1937.cloneTotalOnly());
        validate_193x(1937, "перепись 1937, неисправленное", p1937_original);
        validate_193x(1937, "перепись 1937, неисправленное total-only", p1937_original.cloneTotalOnly());
        validate_193x(1939, "АДХ 1937", PopulationADH.getPopulationByLocality(Area.USSR, 1937));
        Util.out("");

        validate_193x(1938, "АДХ 1938", PopulationADH.getPopulationByLocality(Area.USSR, 1938));
        Util.out("");

        validate_193x(1939, "перепись 1939, исправленное", p1939);
        validate_193x(1939, "перепись 1939, исправленное total-only", p1939.cloneTotalOnly());
        validate_193x(1939, "перепись 1939, неисправленное", p1939_original);
        validate_193x(1939, "перепись 1939, неисправленное total-only", p1939_original.cloneTotalOnly());
        validate_193x(1939, "АДХ 1939", PopulationADH.getPopulationByLocality(Area.USSR, "1939-границы-1938"));
    }

    private void validate_193x(int year, String which, PopulationByLocality p) throws Exception
    {
        /* в нормировке на начало года */
        double cbr, cdr, infant_CDR;
        Rates r;

        switch (year)
        {
        case 1937:
            r = Recalibrate.m2e(new Rates(CBR_1937_MIDYEAR, CDR_1937_MIDYEAR));
            cbr = r.cbr;
            cdr = r.cdr;
            infant_CDR = ADH_USSR_infant_CDR_1937;
            break;

        case 1938:
            r = Recalibrate.m2e(new Rates(CBR_1938_MIDYEAR, CDR_1938_MIDYEAR));
            cbr = r.cbr;
            cdr = r.cdr;
            infant_CDR = ADH_USSR_infant_CDR_1938;
            break;

        case 1939:
            r = Recalibrate.m2e(new Rates(CBR_1939_MIDYEAR, CDR_1939_MIDYEAR));
            cbr = r.cbr;
            cdr = r.cdr;
            infant_CDR = ADH_USSR_infant_CDR_1939;
            break;

        default:
            throw new Exception("неверный год");
        }

        CombinedMortalityTable mt = CombinedMortalityTable.load("mortality_tables/USSR/1938-1939");
        mt.comment("ГКС-СССР-1938");

        if (use_ADH_USSR_InfantMortalityRate)
        {
            double[] qx = mt.getSingleTable(Locality.TOTAL, Gender.BOTH).qx();

            List<PatchInstruction> instructions = new ArrayList<>();
            PatchInstruction instruction;

            instruction = new PatchInstruction(PatchOpcode.Multiply, 0, 0, infant_CDR / qx[0]);
            instructions.add(instruction);

            instruction = new PatchInstruction(PatchOpcode.MultiplyWithDecay, 1, 5, infant_CDR / qx[0], 1.0);
            instructions.add(instruction);
            mt = PatchMortalityTable.patch(mt, instructions, "младенческая смертность по АДХ");
        }

        double xcdr1 = new EvalMortalityRate().eval(mt, p, null, cbr);
        double xcdr2 = fwdOneLeap(p, mt, cbr);
        double xcdr3 = fwdtwoLeaps(p, mt, cbr, 0.3, 0.9);

        // ### по двум передвижкам на 3 месяца и на 9 месяцев

        Util.out(String
                .format("%d [население: %s] смертность по EvalMortalityRate с таблицей ГКС: %.1f, по АДХ: %.1f, по 1-шаговой передвижке: %.1f, по 2-шаговой передвижке: %.1f",
                        year, which, xcdr1, cdr, xcdr2, xcdr3));
    }

    private double fwdOneLeap(PopulationByLocality p, CombinedMortalityTable mt, double cbr) throws Exception
    {
        if (p.hasRuralUrban())
        {
            // нет отдельных значений для cbr-rural и cbr-urban
            return 0;
        }
        else
        {
            ForwardPopulationT fw = new ForwardPopulationT();
            fw.setBirthRateTotal(cbr);
            PopulationForwardingContext fctx = new PopulationForwardingContext();
            PopulationByLocality p2 = fctx.begin(p);
            PopulationByLocality p3 = fw.forward(p2, fctx, mt, 1.0);
            PopulationByLocality p4 = fctx.end(p3);
            Util.unused(p4);

            double deaths = fw.getObservedDeaths();
            double xcbr = 1000 * deaths / p.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);
            return xcbr;
        }
    }

    private double fwdtwoLeaps(PopulationByLocality p, CombinedMortalityTable mt, double cbr, double yf1, double yf2) throws Exception
    {
        if (p.hasRuralUrban())
        {
            // нет отдельных значений для cbr-rural и cbr-urban
            return 0;
        }
        else
        {
            ForwardPopulationT fw = new ForwardPopulationT();
            fw.setBirthRateTotal(cbr);
            PopulationForwardingContext fctx = new PopulationForwardingContext();
            PopulationByLocality p2 = fctx.begin(p);
            PopulationByLocality p3 = fw.forward(p2, fctx, mt, yf1);
            double deaths = fw.getObservedDeaths();

            fw = new ForwardPopulationT();
            fw.setBirthRateTotal(cbr);
            PopulationByLocality p4 = fw.forward(p3, fctx, mt, yf2);
            deaths += fw.getObservedDeaths();

            PopulationByLocality p5 = fctx.end(p4);
            Util.unused(p5);

            double xcbr = 1000 * deaths / p.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);
            return xcbr;
        }
    }

    /* =========================================================================== */

    private void test_1() throws Exception
    {
        double infant_CDR = ADH_USSR_infant_CDR_1939;
        double cbr, cdr;
        Rates r = Recalibrate.m2e(new Rates(CBR_1939_MIDYEAR, CDR_1939_MIDYEAR));
        cbr = r.cbr;
        cdr = r.cdr;

        CombinedMortalityTable mt = CombinedMortalityTable.load("mortality_tables/USSR/1938-1939");
        mt.comment("ГКС-СССР-1938");

        if (Util.False && use_ADH_USSR_InfantMortalityRate)
        {
            double[] qx = mt.getSingleTable(Locality.TOTAL, Gender.BOTH).qx();

            List<PatchInstruction> instructions = new ArrayList<>();
            PatchInstruction instruction;

            instruction = new PatchInstruction(PatchOpcode.Multiply, 0, 0, infant_CDR / qx[0]);
            instructions.add(instruction);

            instruction = new PatchInstruction(PatchOpcode.MultiplyWithDecay, 1, 5, infant_CDR / qx[0], 1.0);
            instructions.add(instruction);
            mt = PatchMortalityTable.patch(mt, instructions, "младенческая смертность по АДХ");
        }

        PopulationByLocality p = p1939;
        // p = p.selectByAge(8, MAX_AGE);

        Util.out("EMR-UR");
        EvalMortalityRate emr_ur = new EvalMortalityRate();
        emr_ur.debug(true);
        double xcdr1_ur = emr_ur.eval(mt, p, null, CBR_Total_1926);
        
        Util.out("EMR-T");
        PopulationByLocality ptotal = p.cloneTotalOnly();
        EvalMortalityRate emr_t = new EvalMortalityRate();
        emr_t.debug(true);
        double xcdr2_t = emr_t.eval(mt, ptotal, null, CBR_Total_1926);

        Util.out("FW-UR");
        ForwardPopulationUR fwUR = new ForwardPopulationUR();
        fwUR.debug(true);
        fwUR.BirthRateRural = CBR_Rural_1926;
        fwUR.BirthRateUrban = CBR_Urban_1926;
        PopulationForwardingContext fctx_ur = new PopulationForwardingContext();
        PopulationByLocality px_ur = fctx_ur.begin(p);
        PopulationByLocality p2_ur = fwUR.forward(px_ur, fctx_ur, mt, 1.0);
        PopulationByLocality pend_ur = fctx_ur.end(p2_ur);

        Util.out("FW-T");
        ForwardPopulationT fwT = new ForwardPopulationT();
        fwT.debug(true);
        fwT.BirthRateTotal = CBR_Total_1926;
        PopulationForwardingContext fctx_t = new PopulationForwardingContext();
        PopulationByLocality px_t = fctx_t.begin(ptotal);
        PopulationByLocality p2_t = fwT.forward(px_t, fctx_t, mt, 1.0);
        PopulationByLocality pend_t = fctx_t.end(p2_t);

        Util.noop();
    }
}
