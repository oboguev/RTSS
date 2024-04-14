package rtss.ww2losses.population_194x;

import rtss.data.population.PopulationByLocality;
import rtss.data.population.forward.PopulationForwardingContext;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

public class UtilBase_194x
{
    /*
     * true => использовать данные для населения 1940 и 1941 гг. расчитанные АДХ
     * false => вычислять их продвижкой от переписи 1939 года 
     */
    public static final boolean useADH = true;    
    
    protected void show_struct(String what, PopulationByLocality p, PopulationForwardingContext fctx) throws Exception
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
    
    protected double forward_6mo(double v, double rate)
    {
        double f = Math.sqrt(1 + rate / 1000);
        return v * f;
    }
}
