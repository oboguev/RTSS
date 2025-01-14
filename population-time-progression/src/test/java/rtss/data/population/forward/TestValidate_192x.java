package rtss.data.population.forward;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.EvalMortalityRate;
import rtss.data.population.Population;
import rtss.data.population.PopulationByLocality;
import rtss.data.population.synthetic.PopulationADH;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

public class TestValidate_192x
{
    public static void main(String[] args)
    {
        try
        {
            new TestValidate_192x().validate_1926();
        }
        catch (Exception ex)
        {
            Util.err("*** Exception: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    }

    private static final int MAX_AGE = Population.MAX_AGE;
    private static final double PROMILLE = 1000.0;

    private final boolean DoSmoothPopulation = Util.True;
    private PopulationByLocality p1926 = PopulationByLocality.census(Area.USSR, 1926).smooth(DoSmoothPopulation);
    protected CombinedMortalityTable mt1926 = new CombinedMortalityTable("mortality_tables/USSR/1926-1927");

    private TestValidate_192x() throws Exception
    {
    }

    private void validate_1926() throws Exception
    {
        final double BirthRateTotal = 44.0;
        final double BirthRateRural = 46.1;
        final double ruralPopulation = p1926.sum(Locality.RURAL, Gender.BOTH, 0, MAX_AGE);
        final double urbanPopulation = p1926.sum(Locality.URBAN, Gender.BOTH, 0, MAX_AGE);
        final double BirthRateUrban = (BirthRateTotal * (ruralPopulation + urbanPopulation) - BirthRateRural * ruralPopulation) / urbanPopulation;

        double xcdr1 = new EvalMortalityRate().eval(mt1926, p1926, null, BirthRateTotal);

        ForwardPopulationUR fw_ur = new ForwardPopulationUR();
        fw_ur.debug(true);
        fw_ur.BirthRateRural = BirthRateRural;
        fw_ur.BirthRateUrban = BirthRateUrban;
        PopulationForwardingContext fctx_ur = new PopulationForwardingContext();
        PopulationByLocality px_ur = fctx_ur.begin(p1926);
        PopulationByLocality p2_ur = fw_ur.forward(px_ur, fctx_ur, mt1926, 1.0);
        PopulationByLocality pend_ur = fctx_ur.end(p2_ur);
        double xcdr2_ur = PROMILLE * fw_ur.getObservedDeaths() / p1926.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);

        PopulationByLocality p1926_total = p1926.cloneTotalOnly();
        ForwardPopulationT fw_t = new ForwardPopulationT();
        fw_t.debug(true);
        fw_t.BirthRateTotal = BirthRateTotal;
        PopulationForwardingContext fctx_t = new PopulationForwardingContext();
        PopulationByLocality px_t = fctx_t.begin(p1926_total);
        PopulationByLocality p2_t = fw_t.forward(px_t, fctx_t, mt1926, 1.0);
        PopulationByLocality pend_t = fctx_t.end(p2_t);
        double xcdr2_t = PROMILLE * fw_t.getObservedDeaths() / p1926.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);

        double xcdr3_1926 = new EvalMortalityRate().eval(mt1926, PopulationADH.getPopulationByLocality(Area.USSR, 1926), null, BirthRateTotal);
        double xcdr3_1927 = new EvalMortalityRate().eval(mt1926, PopulationADH.getPopulationByLocality(Area.USSR, 1927), null, BirthRateTotal);
        
        // По АДХ 
        // CBR: 1926 = 45.6, 1927 = 46.3  
        // CDR: 1926 = 25.5, 1927 = 26.5  
        Util.out(String.format("CDR eval по переписи, %.1f", xcdr1));
        Util.out(String.format("CDR передвижкой-UR по переписи, %.1f", xcdr2_ur));
        Util.out(String.format("CDR передвижкой-T по переписи, %.1f", xcdr2_t));
        Util.out(String.format("CDR eval по АДХ-1926, %.1f", xcdr3_1926));
        Util.out(String.format("CDR eval по АДХ-1927, %.1f", xcdr3_1927));
        
        Util.unused(pend_t, pend_ur);
    }
}
