package rtss.pre1917.eval;

import rtss.math.algorithms.MathUtil;
import rtss.pre1917.data.DemographicConstants;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;

/*
 * Вычислить кочевое население Астраханской губернии
 */
public class Astrakhan
{
    public static Territory calcSettled(Territory tFullAstrakan) throws Exception
    {
        Territory t = tFullAstrakan.dup("Астраханская (оседлое)");
        EvalProgressive.evalProgressive(t, DemographicConstants.перепись1897_Астраханская_губ_оседлое_население);
        
        evalUgviPopulation(t, tFullAstrakan, 
                           DemographicConstants.перепись1897_Астраханская_губ_оседлое_население,
                           DemographicConstants.перепись1897_Астраханская_губ_всё_население);

        return t;
    }

    public static Territory calcNomadic(Territory tFullAstrakan, int y1, int y2)
    {
        Territory t1 = calc("Астраханская (кочевые калмыки)",
                            tFullAstrakan,
                            DemographicConstants.перепись1897_Астраханская_губ_кочевое_калмыцкое_население,
                            DemographicConstants.рост_Астраханская_губ_кочевое_калмыцкое_население,
                            y1, y2);

        Territory t2 = calc("Астраханская (кочевые киргиз-кайсаки)",
                            tFullAstrakan,
                            DemographicConstants.перепись1897_Астраханская_губ_кочевое_киргиз_кайсацкое_население,
                            DemographicConstants.рост_Астраханская_губ_кочевое_киргиз_кайсацкое_население,
                            y1, y2);

        Territory t = new Territory("Астраханская (кочевники)");

        for (int year = y1; year <= y2; year++)
        {
            TerritoryYear ty = t.territoryYear(year);
            TerritoryYear ty1 = t1.territoryYearOrNull(year);
            TerritoryYear ty2 = t2.territoryYearOrNull(year);

            ty.progressive_population.total.both = ty1.progressive_population.total.both +
                                                   ty2.progressive_population.total.both;

            ty.population.total.both = ty1.population.total.both +
                                       ty2.population.total.both;
        }

        return t;
    }

    private static Territory calc(String tname, Territory tFullAstrakan, long censusPopulation, double ngr, int y1, int y2)
    {
        Territory t = new Territory(tname);

        TerritoryYear ty1897 = t.territoryYear(1897);
        ty1897.progressive_population.total.both = MathUtil.yearStartPopulation(27, censusPopulation, ngr);

        double a = 1 + ngr / DemographicConstants.PROMILLE;

        for (int year = y1; year <= y2; year++)
        {
            if (year != 1897)
            {
                TerritoryYear ty = t.territoryYear(year);
                double b = Math.pow(a, year - 1897);

                ty.progressive_population.total.both = Math.round(b * ty1897.progressive_population.total.both);
            }
        }

        evalUgviPopulation(t, tFullAstrakan, 
                           censusPopulation,
                           DemographicConstants.перепись1897_Астраханская_губ_всё_население);

        return t;
    }

    /*
     * Примерная доля населения по УГВИ относимая на под-территорию
     */
    private static void evalUgviPopulation(Territory t, Territory tFullAstrakan, 
                       long thisPopulationCensus,
                       long wholePopulationCensus)
    {
        final double share = (double) thisPopulationCensus / wholePopulationCensus;
        
        for (int year : t.years())
        {
            TerritoryYear ty = t.territoryYearOrNull(year);
            TerritoryYear tyFull = tFullAstrakan.territoryYearOrNull(year);
            if (ty != null && tyFull != null && tyFull.population.total.both != null)
                ty.population.total.both = Math.round(tyFull.population.total.both * share); 
        }
    }
    
    /* ======================================================================================= */

    public static boolean isAnyAstrakhan(String tname)
    {
        return tname.equals("Астраханская") ||
               tname.equals("Астраханская (оседлое)") ||
               tname.equals("Астраханская (кочевники)");
    }

    /*
     * Совокупное население Астраханской губернии за год
     */
    public static TerritoryYear territoryYearOrNull(TerritoryDataSet tds, int year)
    {
        Territory t = tds.get("Астраханская");
        if (t != null)
            return t.territoryYearOrNull(year);

        Territory t1 = tds.get("Астраханская (оседлое)");
        Territory t2 = tds.get("Астраханская (кочевники)");

        TerritoryYear ty1 = t1.territoryYearOrNull(year);
        TerritoryYear ty2 = t2.territoryYearOrNull(year);

        ty1 = ty1.dup(null);
        ty1.progressive_population.total.both += ty2.progressive_population.total.both;

        if (ty1.population.total.both != null && ty2.population.total.both != null)
            ty1.population.total.both += ty2.population.total.both;

        return ty1;
    }
}
