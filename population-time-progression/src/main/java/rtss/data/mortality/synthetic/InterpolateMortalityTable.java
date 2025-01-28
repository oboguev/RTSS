package rtss.data.mortality.synthetic;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.EvalMortalityRate;
import rtss.data.population.PopulationByLocality;
import rtss.data.population.forward.PopulationContext;
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
            final PopulationContext fctx,
            double cbr,
            double cdr) throws Exception
    {
        double cdr1 = new EvalMortalityRate().eval(mt1, p, fctx, cbr);
        double cdr2 = new EvalMortalityRate().eval(mt2, p, fctx, cbr);

        double a = (cdr - cdr2) / (cdr1 - cdr2);

        if (a < 0 || a > 1)
            Util.err("Таблица для данного уровня смертности не составляема");

        if (a < 0)
            a = 0;
        if (a > 1)
            a = 1;

        CombinedMortalityTable mt = CombinedMortalityTable.interpolate(mt1, mt2, 1 - a);

        double cdr_mt = new EvalMortalityRate().eval(mt, p, fctx, cbr);

        if (Util.differ(cdr, cdr_mt, 0.005))
            throw new Exception("Error evaluating combined mortality table: verification mismatch");

        return mt;
    }

    /*
     * Создать таблицу смертности комбинацией двух таблиц таким образом, что при данном населении (@p, @fctx)
     * и рождаемости @cbr, она даёт смертность @cdr. Для возрастов выше @toAge комбинация не проводится и
     * всегда применяются значения @mt1.
     * 
     * @mt2 - таблица с более высокой детской смертностью, чем @mt1.
     * 
     * Ищется положение кривой смертности для возрастов [0 ... @toAge] между @mt1 и @mt2 приводящее к @cdr.
     */

    public static CombinedMortalityTable forTargetRates(
            final CombinedMortalityTable mt1,
            final CombinedMortalityTable mt2,
            final PopulationByLocality p,
            double cbr,
            double cdr,
            int toAge,
            Double imr) throws Exception
    {
        return forTargetRates(mt1, mt2, p, null, cbr, cdr, toAge, imr);
    }

    public static CombinedMortalityTable forTargetRates(
            final CombinedMortalityTable mt1,
            final CombinedMortalityTable mt2,
            final PopulationByLocality p,
            final PopulationContext fctx,
            double cbr,
            double cdr,
            int toAge,
            Double imr) throws Exception
    {
        double w1 = 0;
        double w2 = 1;
        int iterations = 0;

        CombinedMortalityTable xmt1 = mt1;
        CombinedMortalityTable xmt2 = CombinedMortalityTable.interpolate(mt1, mt2, toAge, w2);
        
        // xmt1 = patchInfantMortalityRate(xmt1, imr);
        // xmt2 = patchInfantMortalityRate(xmt2, imr);

        double cdr1 = new EvalMortalityRate().eval(xmt1, p, fctx, cbr);
        double cdr2 = new EvalMortalityRate().eval(xmt2, p, fctx, cbr);

        if (cdr2 <= cdr1)
            throw new Exception("Таблица для данного уровня смертности несоставима");
        
        for (;;)
        {
            if (iterations++ > 1000)
                throw new Exception("Таблица для данного уровня смертности несоставима за 1000 итераций");
            
            if (Math.abs(cdr - cdr2) < 0.01)
                return xmt2;

            if (Math.abs(cdr - cdr1) < 0.01)
                return xmt1;
            
            if (!(cdr >= cdr1 && cdr <= cdr2))
                throw new Exception("Таблица для данного уровня смертности несоставима");
            
            double w = (w1 + w2) /2;
            CombinedMortalityTable xmtw = CombinedMortalityTable.interpolate(mt1, mt2, toAge, w);
            xmtw = patchInfantMortalityRate(xmtw, imr);
            double cdrw = new EvalMortalityRate().eval(xmtw, p, fctx, cbr);
            
            if (Math.abs(cdr - cdrw) < 0.01)
                return xmtw;
            
            if (cdrw > cdr)
            {
                w2 = w;
                cdr2 = cdrw;
                xmt2 = xmtw;
            }
            else
            {
                w1 = w;
                cdr1 = cdrw;
                xmt1 = xmtw;
            }
        }
    }
    
    private static CombinedMortalityTable patchInfantMortalityRate(CombinedMortalityTable mt, Double imr) throws Exception
    {
        if (imr == null)
        {
            return mt;
        }
        else
        {
            CombinedMortalityTable xmt = PatchMortalityTable.patchInfantMortalityRate(mt, imr, "patched infant mortality");
            return xmt;
        }
    }
}
