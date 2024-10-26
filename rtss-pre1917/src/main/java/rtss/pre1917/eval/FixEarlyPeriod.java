package rtss.pre1917.eval;

import java.util.ArrayList;
import java.util.List;

import rtss.data.selectors.BirthDeath;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.data.URValue;
import rtss.pre1917.data.migration.TotalMigration;
import rtss.util.Util;

/*
 * Скорректировать рождаемость и смертность в указанном "левом" промежутке, сделав их значения такими же,
 * как в указанном "правом" промежутке.
 *   
 * Мы берём "правый" промежуток, устраняя из него революционный 1905 год как заведомо атипичный, 
 * и вычисляем среднее значение показателей (рождаемости и смертности) для значений в этом "правом" промежутке 
 * после устранения выбросов (outliers уклоняющихся от среднего значения более чем на 1.4 средних отклонений), 
 * после чего среднее для "правого" промежутка значение пересчитывается по остатку. 
 * 
 * Мы затем модифицируем число рождений и смертей для годов в "левой" части шкалы таким образом, 
 * чтобы достичь для этих лет показателей рождаемости и смертности по прогрессивному расчёту равных 
 * вычисленным средним значениям для "правой" части.
 * 
 * Поскольку изменение числа рождений и смертей в "левые" годы приведёт к изменению населения в "правой" части 
 * и изменит значения показателей движения в "правой" части, мы повторяем процесс итеративно до схождения, 
 * покуда изменение между значениями целевых рождаемости и смертности между шагами не станет менее 0.01 промилле.
 * 
 * "Левые" и "правые" промежутки могут быть указаны для смертности и рождаемости раздельно.
 */
public class FixEarlyPeriod
{
    private final TotalMigration totalMigration = TotalMigration.getTotalMigration();
    private final double PROMILLE = 1000.0;

    public FixEarlyPeriod() throws Exception
    {
    }

    /*
     * by - год, с которого наладилась регистрация рождаемости 
     * dy - год, с которого наладилась регистрация смертности 
     */
    public Territory fix(Territory t, Territory tCensus, int by, int dy) throws Exception
    {
        int byl1 = 1896;
        int byl2 = by - 1;

        int byr1 = by;
        int byr2 = 1916;

        int dyl1 = 1896;
        int dyl2 = dy - 1;

        int dyr1 = dy;
        int dyr2 = 1916;

        return fix(t, tCensus, byl1, byl2, byr1, byr2, dyl1, dyl2, dyr1, dyr2);
    }

    /*
     * [byl1 ... byl2] = корректируемый (левый) участок для рождений 
     * [byr1 ... byr2] = участок (правый), по значениям которого проводится коррекция рождений
     *  
     * [dyl1 ... dyl2] = корректируемый (левый) участок для смертей 
     * [dyr1 ... dyr2] = участок (правый), по значениям которого проводится коррекция смертей 
     */
    public Territory fix(Territory t, Territory tCensus, int byl1, int byl2, int byr1, int byr2,
            int dyl1, int dyl2, int dyr1, int dyr2) throws Exception
    {
        final double rate_threshold = 0.01;
        double prev_av_cbr = 0;
        double prev_av_cdr = 0;
        Territory xt = t;

        for (int pass = 0;; pass++)
        {
            xt = fix_pass(xt, tCensus, byl1, byl2, byr1, byr2, dyl1, dyl2, dyr1, dyr2);

            if (pass >= 1 &&
                Math.abs(av_cbr - prev_av_cbr) < rate_threshold &&
                Math.abs(av_cdr - prev_av_cdr) < rate_threshold)
            {
                break;
            }
            else if (pass > 100)
            {
                throw new Exception("FixEarlyPeriod failed to converge for " + t.name);
            }

            prev_av_cbr = av_cbr;
            prev_av_cdr = av_cdr;
        }

        return xt;
    }

    private double av_cbr;
    private double av_cdr;

    private Territory fix_pass(Territory t, Territory tCensus, int byl1, int byl2, int byr1, int byr2,
            int dyl1, int dyl2, int dyr1, int dyr2) throws Exception
    {
        av_cbr = averageRate(t, byr1, byr2, BirthDeath.BIRTH);
        av_cdr = averageRate(t, dyr1, dyr2, BirthDeath.DEATH);

        /* ================================= seed data ================================= */

        TerritoryYear tyCensus = tCensus.territoryYearOrNull(1897);

        TerritoryYear ty1896 = t.territoryYearOrNull(1896);
        TerritoryYear ty1897 = t.territoryYearOrNull(1897);
        TerritoryYear ty1898 = t.territoryYearOrNull(1898);

        Territory xt = t.dup();
        TerritoryYear xty1896 = xt.territoryYearOrNull(1896);
        TerritoryYear xty1897 = xt.territoryYearOrNull(1897);

        adjust_births(xty1897, byl1, byl2, av_cbr, tyCensus.population.total.both);
        adjust_deaths(xty1897, dyl1, dyl2, av_cdr, tyCensus.population.total.both);

        long in = xty1897.births.total.both - xty1897.deaths.total.both;
        in += totalMigration.saldo(t.name, 1897);
        long in1 = Math.round(in * 27.0 / 365.0);
        long in2 = in - in1;

        ty1897.progressive_population.total.both = tyCensus.population.total.both - in1;
        ty1898.progressive_population.total.both = tyCensus.population.total.both + in2;

        if (xty1896 != null)
        {
            adjust_births(xty1896, byl1, byl2, av_cbr, tyCensus.population.total.both);
            adjust_deaths(xty1896, dyl1, dyl2, av_cdr, tyCensus.population.total.both);

            in = xty1896.births.total.both - xty1896.deaths.total.both;
            in += totalMigration.saldo(t.name, 1896);
            ty1896.progressive_population.total.both = ty1897.progressive_population.total.both - in;
        }

        for (int year = 1898; year <= 1916; year++)
        {
            TerritoryYear xty = xt.territoryYearOrNull(year);
            if (xty != null)
            {
                adjust_births(xty, byl1, byl2, av_cbr, xty.progressive_population.total.both);
                adjust_deaths(xty, dyl1, dyl2, av_cdr, xty.progressive_population.total.both);

                TerritoryYear xty_next = xt.territoryYearOrNull(year + 1);
                if (xty_next != null)
                {
                    in = null2zero(xty.births.total.both) - null2zero(xty.deaths.total.both);
                    in += totalMigration.saldo(t.name, year);

                    xty_next.progressive_population.total.both = xty.progressive_population.total.both + in;
                }
            }
        }

        return xt;
    }

    private void adjust_births(TerritoryYear ty, int byl1, int byl2, double av_cbr, long pop)
    {
        if (ty.year >= byl1 && ty.year <= byl2)
        {
            ty.births.leaveOnlyTotalBoth();
            ty.births.total.both = Math.round(pop * av_cbr / PROMILLE);
        }
    }

    private void adjust_deaths(TerritoryYear ty, int dyl1, int dyl2, double av_cdr, long pop)
    {
        if (ty.year >= dyl1 && ty.year <= dyl2)
        {
            ty.deaths.leaveOnlyTotalBoth();
            ty.deaths.total.both = Math.round(pop * av_cdr / PROMILLE);
        }
    }

    /* ============================================================ */

    private double averageRate(Territory t, int y1, int y2, BirthDeath bd) throws Exception
    {
        List<Double> list = new ArrayList<>();

        for (int year = y1; year <= y2; year++)
        {
            TerritoryYear ty = t.territoryYearOrNull(year);
            if (ty == null || year == 1905 || movement(ty, bd).total.both == null)
                continue;

            long pop = ty.progressive_population.total.both;
            long mv = movement(ty, bd).total.both;
            list.add((PROMILLE * mv) / pop);
        }

        double[] r = toArray(list);
        r = eliminateOutliers(r);

        return average(r);
    }

    private URValue movement(TerritoryYear ty, BirthDeath bd)
    {
        switch (bd)
        {
        case BIRTH:
            return ty.births;

        case DEATH:
            return ty.deaths;

        default:
            return null;
        }
    }

    private long null2zero(Long v)
    {
        return v == null ? 0 : v;
    }

    /* ============================================================ */

    private double[] toArray(List<Double> list)
    {
        double[] r = new double[list.size()];
        int k = 0;
        for (Double d : list)
            r[k++] = d;
        return r;
    }

    private double[] eliminateOutliers(double[] dx)
    {
        double av = average(dx);
        double stdev = stdev(dx);

        List<Double> list = new ArrayList<>();
        for (Double d : dx)
        {
            if (Math.abs(av - d) < 1.4 * stdev)
                list.add(d);
        }

        return toArray(list);
    }

    private double average(double[] dx)
    {
        double v = 0;
        for (double d : dx)
            v += d;
        return v / dx.length;
    }

    private double stdev(double[] dx)
    {
        double av = average(dx);
        double v = 0;
        for (double d : dx)
            v += Math.pow(d - av, 2);
        return Math.sqrt(v / (dx.length - 1));
    }
}
