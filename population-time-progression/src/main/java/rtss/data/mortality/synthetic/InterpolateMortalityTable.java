package rtss.data.mortality.synthetic;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.EvalMortalityRate;
import rtss.data.population.PopulationByLocality;
import rtss.data.population.forward.PopulationForwardingContext;
import rtss.util.Util;

public class InterpolateMortalityTable
{
    /*
     * Создать таблицу смертности комбинацией двух таблиц таким образом, что при данном населении (@p, @fctx)
     * и рождаемости @cbr, она даёт смертность @cdr
     */
    public static CombinedMortalityTable forTargetRates(
            final CombinedMortalityTable mt1, 
            final CombinedMortalityTable mt2,
            final PopulationByLocality p,
            double cbr,
            double cdr) throws Exception
    {
        return forTargetRates(mt1, mt2, p, null, cbr, cdr);
    }

    public static CombinedMortalityTable forTargetRates(
            final CombinedMortalityTable mt1, 
            final CombinedMortalityTable mt2,
            final PopulationByLocality p, 
            final PopulationForwardingContext fctx, 
            double cbr,
            double cdr) throws Exception
    {
        double cdr1 = EvalMortalityRate.eval(mt1, p, fctx, cbr);
        double cdr2 = EvalMortalityRate.eval(mt2, p, fctx, cbr);

        double a = (cdr - cdr2) / (cdr1 - cdr2);

        if (a < 0 || a > 1)
            Util.err("Таблица для данного уровня смертности не составляема");
        
        if (a < 0) a = 0;
        if (a > 1) a = 1;

        return CombinedMortalityTable.interpolate(mt1, mt2, 1 - a);
    }
}
