package rtss.data.population.forward;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.Population;
import rtss.data.population.PopulationByLocality;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

/**
 * Обратная во времени передвижка населения по таблице смертности не имеющей отдельных частей
 * для городского и сельского населения, а только часть Total.
 * 
 * Все данные о численности населения должны находиться в PopulationForwardingContext с размером ALL_AGES:
 * 
 *     PopulationForwardingContext fctx = new PopulationForwardingContext(PopulationForwardingContext.ALL_AGES);
 *     PopulationByLocality pto = fctx.begin(p); // pto - пустой
 *     ....
 *     pto = backward(pto, fctx, mt, yfraction) <== повторяемое сколько требуется
 *     ....
 *     ptoEnd = fctx.end(pto);
 * 
 * Величины
 *     observed_deaths
 *     fctx_t_male_deaths
 *     fctx_t_female_deaths
 * содержат число "отмерших".
 * 
 * Родившиеся за период исчезают.
 * 
 */
public class BackwardPopulationT extends ForwardPopulation
{
    private double fctx_t_male_deaths = 0;
    private double fctx_t_female_deaths = 0;

    /*
     * Обратная передвижка населения во времени на целый год или на часть года.
     * Начальное население (@p) и таблица смертности (@mt) неизменны. 
     * Результат возвращается как новая структура.
     * При передвижке на целый год @yfraction = 1.0.
     * При передвижке на часть года @yfraction < 1.0.
     */
    public Population backward(final Population p,
            PopulationForwardingContext fctx,
            final CombinedMortalityTable mt,
            final double yfraction)
            throws Exception
    {
        PopulationByLocality xp = new PopulationByLocality(p);
        PopulationByLocality xp2 = backward(xp, fctx, mt, yfraction);
        return xp2.forLocality(Locality.TOTAL);
    }

    public PopulationByLocality backward(final PopulationByLocality p,
            PopulationForwardingContext fctx,
            final CombinedMortalityTable mt,
            final double yfraction)
            throws Exception
    {
        if (fctx == null || fctx.MAX_YEAR != MAX_AGE || p.sum() != 0)
            throw new IllegalArgumentException("население не перегружено целиком в PopulationForwardingContext, используйте PopulationForwardingContext.ALL_AGES");

        if (yfraction > 1)
            throw new IllegalArgumentException("передвижка на более чем год");

        backward(fctx, Locality.TOTAL, mt, yfraction);

        if (debug)
        {
            log(String.format("Deaths FCTX-TOTAL-MALE => %s", f2s(fctx_t_male_deaths)));
            log(String.format("Deaths FCTX-TOTAL-FEMALE => %s", f2s(fctx_t_female_deaths)));
            log("");
        }

        return PopulationByLocality.newPopulationTotalOnly();
    }

    public void backward(
            PopulationForwardingContext fctx,
            final Locality locality,
            final CombinedMortalityTable mt,
            final double yfraction)
            throws Exception
    {
        /* обратная передвижка мужского и женского населений по смертности из @p в @pto */
        backward(fctx, locality, Gender.MALE, mt, yfraction);
        backward(fctx, locality, Gender.FEMALE, mt, yfraction);
    }

    private void backward(
            PopulationForwardingContext fctx,
            final Locality locality,
            final Gender gender,
            final CombinedMortalityTable mt,
            final double yfraction)
            throws Exception
    {
        int ndays = (int) Math.round(yfraction * fctx.DAYS_PER_YEAR);
        if (ndays == 0)
            return;

        double[] day_lx = fctx.get_daily_lx(mt, locality, gender);
        /* lx[nd] содержит число выживших на день жизни nd согласно таблице смертности */

        double[] p = fctx.asArray(locality, gender);
        double[] p2 = new double[p.length];

        double sum_deaths = 0;

        for (int nd = ndays; nd < p.length; nd++)
        {
            int nd2 = nd - ndays;
            double v = p[nd];
            double v2 = v * day_lx[nd2] / day_lx[nd];
            p2[nd2] = v2;
            sum_deaths += v2 - v;
        }

        // восстановить умерших
        if (Util.True)
        {
            int back = fctx.DAYS_PER_YEAR;

            for (int nd = p.length - ndays; nd < p.length; nd++)
            {
                double ratio = day_lx[nd] / day_lx[nd - back];
                p2[nd] = p2[nd - back] * ratio;
                sum_deaths += p2[nd];
            }
        }

        fctx.fromArray(locality, gender, p2);

        switch (gender.name())
        {
        case "MALE":
            fctx_t_male_deaths += sum_deaths;
            break;

        case "FEMALE":
            fctx_t_female_deaths += sum_deaths;
            break;
        }

        observed_deaths += sum_deaths;
    }

    private String f2s(double v)
    {
        String s = String.format("%,15.0f", v);
        while (s.startsWith(" "))
            s = s.substring(1);
        return s;
    }
}
