package rtss.data.rates;

import rtss.data.population.PopulationByLocality;
import rtss.data.population.synthetic.PopulationADH;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;

/*
 * Пересчитать уровни рождаемости и смертности из нормировки относительно численности населения в начале года 
 * в нормировку на среднегодовую величину населения или обратно.   
 */
public class Recalibrate
{
    private static double PROMILLE = 1000.0;
    
    /*
     * Рождаемость и смертность в нормировке на население в начале года -> рождаемость в нормировке на среднегодовое население.
     * Миграция полагается нулевой.
     * 
     * Линейная формула верна лишь для низких величин рождаемости и смертности.
     */
    public static double cbr_e2m(double cbr, double cdr)
    {
        double f = 1 + (cbr - cdr) / (PROMILLE * 2);
        return cbr / f;
    }

    /*
     * Рождаемость и смертность в нормировке на население в начале года -> смертность в нормировке на среднегодовое население.   
     * Миграция полагается нулевой.   
     * 
     * Линейная формула верна лишь для низких величин рождаемости и смертности.
     * Она поэтому не годится для младенческой смертности, для преобразования которой следует использовать qx2mx.   
     */
    public static double cdr_e2m(double cbr, double cdr)
    {
        double f = 1 + (cbr - cdr) / (PROMILLE * 2);
        return cdr / f;
    }

    /*
     * Рождаемость и смертность в нормировке на среднегодовое население -> рождаемость в нормировке на население в начале года.
     * Миграция полагается нулевой.   
     * 
     * Линейная формула верна лишь для низких величин рождаемости и смертности.
     */
    public static double cbr_m2e(double cbr, double cdr)
    {
        double f = 1 - (cbr - cdr) / (PROMILLE * 2);
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
        double f = 1 - (cbr - cdr) / (PROMILLE * 2);
        return cdr / f;
    }
    
    /*
     * Преобразовать рождаемость или смертность для указанной территории (СССР или РСФСР) в указанный год
     * из нормировки на население в начале года -> в нормировку на среднегодовое население.
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
     * из нормировки на среднегодовое население -> в нормировку на население в начале года.
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

    public static class Rates
    {
        public double cbr;
        public double cdr;
        
        public Rates(double cbr, double cdr)
        {
            this.cbr = cbr;
            this.cdr = cdr;
        }
    }
    
    /*
     * Рождаемость и смертность в нормировке на среднегодовое население -> рождаемость и смертность в нормировке на население начала года.   
     * Миграция полагается нулевой.   
     * 
     * Линейная формула верна лишь для низких величин рождаемости и смертности.
     * Она поэтому не годится для младенческой смертности, для преобразования которой следует использовать mx2qx.   
     */
    public static Rates m2e(Rates rates)
    {
        Rates x = new Rates(0, 0);
        x.cbr = cbr_m2e(rates.cbr, rates.cdr);
        x.cdr = cdr_m2e(rates.cbr, rates.cdr);
        return x;
    }

    /*
     * Рождаемость и смертность в нормировке на население в начале года -> рождаемость и смертность в нормировке на среднегодовое население.
     * Миграция полагается нулевой.
     * 
     * Линейная формула верна лишь для низких величин рождаемости и смертности.
     * Она поэтому не годится для младенческой смертности, для преобразования которой следует использовать qx2mx.   
     */
    public static Rates e2m(Rates rates)
    {
        Rates x = new Rates(0, 0);
        x.cbr = cbr_e2m(rates.cbr, rates.cdr);
        x.cdr = cdr_e2m(rates.cbr, rates.cdr);
        return x;
    }
}
