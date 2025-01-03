package rtss.data.rates;

import rtss.data.population.PopulationByLocality;
import rtss.data.population.synthetic.PopulationADH;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;

/*
 * Пересчитать уровни рождаемости и смертности из нормировки относительно величины населения на начало года 
 * в нормировку на среднегодовую величину населения или обратно.   
 */
public class Recalibrate
{
    /*
     * Рождаемость и смертность в нормировке на население на начало года -> рождаемость в нормировке на среднегодовое население.
     * Миграция полагается нулевой.
     * 
     * Линейная формула верна лишь для низких величин рождаемости и смертности.
     */
    public static double cbr_e2m(double cbr, double cdr)
    {
        double f = 1 + (cbr - cdr) / 2;
        return cbr / f;
    }

    /*
     * Рождаемость и смертность в нормировке на население на начало года -> смертность в нормировке на среднегодовое население.   
     * Миграция полагается нулевой.   
     * 
     * Линейная формула верна лишь для низких величин рождаемости и смертности.
     * Она поэтому не годится для младенческой смертности, для преобразования которой следует использовать qx2mx.   
     */
    public static double cdr_e2m(double cbr, double cdr)
    {
        double f = 1 + (cbr - cdr) / 2;
        return cdr / f;
    }

    /*
     * Рождаемость и смертность в нормировке на среднегодовое население -> рождаемость в нормировке на население начала года.
     * Миграция полагается нулевой.   
     * 
     * Линейная формула верна лишь для низких величин рождаемости и смертности.
     */
    public static double cbr_m2e(double cbr, double cdr)
    {
        double f = 1 - (cbr - cdr) / 2;
        return cbr / f;
    }

    /*
     * Рождаемость и смертность в нормировке на среднегодовое население -> смертность в нормировке на население начала года.   
     * Миграция полагается нулевой.   
     * 
     * Линейная формула верна лишь для низких величин рождаемости и смертности.
     * Она поэтому не годится для младенческой смертности, для преобразования которой следует использовать mx2qx.   
     */
    public static double cdr_m2e(double cbr, double cdr)
    {
        double f = 1 - (cbr - cdr) / 2;
        return cdr / f;
    }
    
    /*
     * Преобразовать рождаемость или смертность для указанной территории (СССР или РСФСР) в указанный год
     * из нормировки на население на начало года -> в нормировку на среднегодовое население.
     */
    public static double e2m(Area area, int year, double rate) throws Exception
    {
        PopulationByLocality p1 = PopulationADH.getPopulationByLocality(area, year);
        PopulationByLocality p2 = PopulationADH.getPopulationByLocality(area, year + 1);
        double sp1 = p1.sum(Locality.TOTAL, Gender.BOTH, 0, PopulationByLocality.MAX_AGE);
        double sp2 = p2.sum(Locality.TOTAL, Gender.BOTH, 0, PopulationByLocality.MAX_AGE);
        double spm = (sp1 + sp2) / 2;
        return rate * sp1 / spm;
    }

    /*
     * Преобразовать рождаемость или смертность для указанной территории (СССР или РСФСР) в указанный год
     * из нормировки на среднегодовое население -> в нормировку на население на начало года.
     */
    public static double m2e(Area area, int year, double rate) throws Exception
    {
        PopulationByLocality p1 = PopulationADH.getPopulationByLocality(area, year);
        PopulationByLocality p2 = PopulationADH.getPopulationByLocality(area, year + 1);
        double sp1 = p1.sum(Locality.TOTAL, Gender.BOTH, 0, PopulationByLocality.MAX_AGE);
        double sp2 = p2.sum(Locality.TOTAL, Gender.BOTH, 0, PopulationByLocality.MAX_AGE);
        double spm = (sp1 + sp2) / 2;
        return rate * spm / sp1;
    }
}
