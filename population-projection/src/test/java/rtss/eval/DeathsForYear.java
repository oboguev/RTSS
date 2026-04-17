package rtss.eval;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.synthetic.MortalityTableADH;
import rtss.data.population.projection.ForwardPopulationT;
import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationContext;
import rtss.data.population.synthetic.PopulationADH;
import rtss.data.rates.Recalibrate;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

public class DeathsForYear
{
    public static void main(String[] args)
    {
        try
        {
            // АДХ-РСФСР стр. 164
            final int year = 1939;
            final double cbr = 39.8;
            final double cdr = 23.9;
            final int uptoAge = 5;
            new DeathsForYear().eval(Area.RSFSR, year, cbr, cdr, uptoAge);
            Util.out("*** Completed");
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
        }
    }
    
    private void eval(Area area, int year, double cbr, double cdr, int uptoAge) throws Exception
    {
        CombinedMortalityTable mt = MortalityTableADH.getMortalityTable(area, year);
        Population p = PopulationADH.getPopulation(area, year);
        cbr = Recalibrate.m2e(area, year, cbr) ;
        
        ForwardPopulationT fw = new ForwardPopulationT();
        fw.setBirthRateTotal(cbr);
        fw.forward(p.toPopulationContext(), mt, 1.0);
        
        PopulationContext dx = fw.deathsByGenderAge();
        double d_all = dx.sum();
        double d_children = dx.sumAges(Locality.TOTAL, Gender.BOTH, 0, uptoAge);
        
        Util.out(String.format("В %d году в %s умерли %s человек", year, area.toString(), f2s(d_all)));
        Util.out(String.format("Из них дети в возрастах 0-%d лет: %s", uptoAge, f2s(d_children)));
        Util.out(String.format("Или %.1f%%", d_children / d_all * 100));
        Util.noop();
    }

    private String f2s(double v)
    {
        String s = String.format("%,15.0f", v);
        while (s.startsWith(" "))
            s = s.substring(1);
        return s;
    }
}
