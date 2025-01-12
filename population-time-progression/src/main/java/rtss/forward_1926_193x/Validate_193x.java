package rtss.forward_1926_193x;

import java.util.ArrayList;
import java.util.List;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.EvalMortalityRate;
import rtss.data.mortality.synthetic.PatchMortalityTable;
import rtss.data.mortality.synthetic.PatchMortalityTable.PatchInstruction;
import rtss.data.mortality.synthetic.PatchMortalityTable.PatchOpcode;
import rtss.data.population.PopulationByLocality;
import rtss.data.population.synthetic.PopulationADH;
import rtss.data.rates.Recalibrate;
import rtss.data.rates.Recalibrate.Rates;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

/*
 * Сравнить смертность в 1937-1939 годах по передвижке с таблицей Госкомстата и по АДХ
 */
public class Validate_193x
{
    public static void main(String[] args)
    {
        try
        {
            new Validate_193x().validate_1939();
        }
        catch (Exception ex)
        {
            Util.err("*** Exception: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    }

    public Validate_193x() throws Exception
    {
    }

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

        double xcdr = EvalMortalityRate.eval(mt, p, null, cbr);

        Util.out(String.format("%d [население: %s] смертность по передвижке с таблицей ГКС: %.1f, по АДХ: %.1f",
                               year, which, xcdr, cdr));
    }
}
