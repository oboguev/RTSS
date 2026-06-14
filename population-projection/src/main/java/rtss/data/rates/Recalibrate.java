package rtss.data.rates;

import rtss.data.population.struct.PopulationByLocality;
import rtss.data.population.synthetic.PopulationADH;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.math.algorithms.MathUtil;

/*
 * Пересчитать уровни рождаемости и смертности из нормировки относительно численности населения в начале года 
 * в нормировку на среднегодовую величину населения или обратно.   
 */
public class Recalibrate
{
    private static double PROMILLE = 1000.0;

    public static class Rates
    {
        public double cbr;
        public double cdr;

        public Rates()
        {
        }

        public Rates cbr(double cbr)
        {
            this.cbr = cbr;
            return this;
        }

        public Rates cdr(double cdr)
        {
            this.cdr = cdr;
            return this;
        }

        public Rates(double cbr, double cdr)
        {
            this.cbr = cbr;
            this.cdr = cdr;
        }

        public double cbr()
        {
            return cbr;
        }

        public double cdr()
        {
            return cdr;
        }
    }

    /*
     * Рождаемость и смертность в нормировке на среднегодовое население -> рождаемость и смертность в нормировке на население начала года.   
     * Миграция полагается нулевой.   
     * 
     * Линейная формула верна лишь для низких величин рождаемости и смертности.
     */
    public static Rates m2e(Rates rates)
    {
        Rates x = new Rates(0, 0);
        x.cbr = cbr_m2e_log(rates.cbr, rates.cdr);
        x.cdr = cdr_m2e_log(rates.cbr, rates.cdr);
        return x;
    }

    public static Rates m2e_linear(Rates rates)
    {
        Rates x = new Rates(0, 0);
        x.cbr = cbr_m2e_linear(rates.cbr, rates.cdr);
        x.cdr = cdr_m2e_linear(rates.cbr, rates.cdr);
        return x;
    }

    /*
     * Рождаемость и смертность в нормировке на население в начале года -> рождаемость и смертность в нормировке на среднегодовое население.
     * Миграция полагается нулевой.
     * 
     * Линейная формула верна лишь для низких величин рождаемости и смертности.
     */
    public static Rates e2m(Rates rates)
    {
        Rates x = new Rates(0, 0);
        x.cbr = cbr_e2m_log(rates.cbr, rates.cdr);
        x.cdr = cdr_e2m_log(rates.cbr, rates.cdr);
        return x;
    }

    public static Rates e2m_linear(Rates rates)
    {
        Rates x = new Rates(0, 0);
        x.cbr = cbr_e2m_linear(rates.cbr, rates.cdr);
        x.cdr = cdr_e2m_linear(rates.cbr, rates.cdr);
        return x;
    }

    /* ================================================================================================================= */

    /*
     * Рождаемость в нормировке на среднегодовое население -> рождаемость в нормировке на население начала года.
     *
     * Модель: население внутри года меняется экспоненциально с постоянным темпом роста. 
     * Миграция полагается нулевой.
     */
    private static double cbr_m2e_log(double cbr, double cdr)
    {
        double f = mean_to_start_population_factor_from_mid_rates(cbr, cdr);
        return cbr * f;
    }

    /*
     * Смертность в нормировке на среднегодовое население -> смертность в нормировке на население начала года.
     *
     * Модель: население внутри года меняется экспоненциально с постоянным темпом роста. 
     * Миграция полагается нулевой.
     */
    private static double cdr_m2e_log(double cbr, double cdr)
    {
        double f = mean_to_start_population_factor_from_mid_rates(cbr, cdr);
        return cdr * f;
    }

    /*
     * Рождаемость в нормировке на население начала года -> рождаемость в нормировке на среднегодовое население.
     *
     * Модель: население внутри года меняется экспоненциально с постоянным темпом роста. 
     * Миграция полагается нулевой.
     */
    private static double cbr_e2m_log(double cbr, double cdr)
    {
        double f = mean_to_start_population_factor_from_early_rates(cbr, cdr);
        return cbr / f;
    }

    /*
     * Смертность в нормировке на население начала года -> смертность в нормировке на среднегодовое население.
     *
     * Модель: население внутри года меняется экспоненциально с постоянным темпом роста. 
     * Миграция полагается нулевой.
     */
    private static double cdr_e2m_log(double cbr, double cdr)
    {
        double f = mean_to_start_population_factor_from_early_rates(cbr, cdr);
        return cdr / f;
    }

    /*
     * Pmean / Pstart, когда CBR/CDR заданы на население начала года.
     *
     * Здесь:
     *   r = (CBR - CDR) / 1000 = (Pend - Pstart) / Pstart
     *   Pend / Pstart = 1 + r
     *
     * При экспоненциальном изменении населения:
     *   Pmean = log_average(Pstart, Pend)
     */
    private static double mean_to_start_population_factor_from_early_rates(double cbr, double cdr)
    {
        double r = (cbr - cdr) / PROMILLE;

        double pstart = 1.0;
        double pend = 1.0 + r;

        if (!(pend > 0.0) || !Double.isFinite(pend))
            throw new IllegalArgumentException("Invalid natural growth: end-year population must be positive");

        return MathUtil.log_average(pstart, pend);
    }

    /*
     * Pmean / Pstart, когда CBR/CDR заданы на среднегодовое население.
     *
     * Здесь:
     *   k = (CBR - CDR) / 1000
     *
     * Но это уже не (Pend - Pstart) / Pstart, а логарифмический темп:
     *
     *   k = log(Pend / Pstart)
     *
     * потому что:
     *
     *   Pend - Pstart = (CBR - CDR) / 1000 * Pmean
     *
     * а для экспоненциальной траектории:
     *
     *   (Pend - Pstart) / Pmean = log(Pend / Pstart)
     */
    private static double mean_to_start_population_factor_from_mid_rates(double cbr, double cdr)
    {
        double k = (cbr - cdr) / PROMILLE;

        double pstart = 1.0;
        double pend = Math.exp(k);

        if (!(pend > 0.0) || !Double.isFinite(pend))
            throw new IllegalArgumentException("Invalid natural growth: end-year population must be positive");

        return MathUtil.log_average(pstart, pend);
    }

    /* ================================================================================================================= */

    /*
     * Рождаемость и смертность в нормировке на население в начале года -> рождаемость в нормировке на среднегодовое население.
     * Миграция полагается нулевой.
     * 
     * Линейная формула верна лишь для низких величин рождаемости и смертности.
     */
    private static double cbr_e2m_linear(double cbr, double cdr)
    {
        double f = 1 + (cbr - cdr) / (PROMILLE * 2);
        return cbr / f;
    }

    /*
     * Рождаемость и смертность в нормировке на население в начале года -> смертность в нормировке на среднегодовое население.   
     * Миграция полагается нулевой.   
     * 
     * Линейная формула верна лишь для низких величин рождаемости и смертности, а точнее прироста (CBR - CDR).
     */
    private static double cdr_e2m_linear(double cbr, double cdr)
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
    private static double cbr_m2e_linear(double cbr, double cdr)
    {
        double f = 1 - (cbr - cdr) / (PROMILLE * 2);
        return cbr / f;
    }

    /*
     * Рождаемость и смертность в нормировке на среднегодовое население -> смертность в нормировке на население начала года.   
     * Миграция полагается нулевой.   
     * 
     * Линейная формула верна лишь для низких величин рождаемости и смертности.
     */
    private static double cdr_m2e_linear(double cbr, double cdr)
    {
        double f = 1 - (cbr - cdr) / (PROMILLE * 2);
        return cdr / f;
    }

    /* ================================================================================================================= */

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
        double spm = MathUtil.log_average(sp1, sp2);
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
        double spm = MathUtil.log_average(sp1, sp2);
        return rate * spm / sp1;
    }
}
