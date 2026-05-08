package rtss.pre1917.eval;

import rtss.math.algorithms.MathUtil;
import rtss.pre1917.data.DemographicConstants;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryYear;

/*
 * Вычислить кочевое население Астраханской губернии
 */
public class Astrakhan
{
    public static Territory calcSettled(Territory tAstrakan) throws Exception
    {
        Territory t = tAstrakan.dup("Астраханская (оседлое)");
        EvalProgressive.evalProgressive(t,  DemographicConstants.перепись1897_Астраханская_губ_оседлое_население);
        return t;
    }
    
    public static Territory calcNomadic(int y1, int y2)
    {
        Territory t1 = calc("Астраханская (кочевые калмыки)",
                            DemographicConstants.перепись1897_Астраханская_губ_кочевое_калмыцкое_население,
                            DemographicConstants.рост_Астраханская_губ_кочевое_калмыцкое_население,
                            y1, y2);

        Territory t2 = calc("Астраханская (кочевые киргиз-кайсаки)",
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
        }

        return t;
    }

    private static Territory calc(String tname, long censusPopulation, double ngr, int y1, int y2)
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

        return t;
    }
}
