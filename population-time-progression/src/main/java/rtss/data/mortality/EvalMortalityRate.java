package rtss.data.mortality;

import rtss.data.population.PopulationByLocality;
import rtss.data.population.forward.PopulationForwardingContext;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;

/**
 * Вычислить годовой уровень смертности при данной таблице смертности,
 * данной возрастной структуре населения и данном уровне рождаемости
 */
public class EvalMortalityRate
{
    private static final int MAX_AGE = CombinedMortalityTable.MAX_AGE;

    public static double eval(final CombinedMortalityTable mt, final PopulationByLocality p, double cbr) throws Exception
    {
        return eval(mt, p, null, cbr);
    }

    public static double eval(
            final CombinedMortalityTable mt, 
            PopulationByLocality p, 
            PopulationForwardingContext fctx, 
            double cbr) throws Exception
    {
        if (fctx == null)
        {
            fctx = new PopulationForwardingContext();
            p = fctx.begin(p);
        }
        
        double total_pop = p.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);
        if (fctx != null)
            total_pop += fctx.sumAllAges(Locality.TOTAL, Gender.BOTH);

        double total_deaths = 0;

        if (p.hasRuralUrban())
        {
            for (int age = 0; age <= MAX_AGE; age++)
            {
                total_deaths += deaths(mt, p, Locality.URBAN, Gender.MALE, age);
                total_deaths += deaths(mt, p, Locality.URBAN, Gender.FEMALE, age);
                total_deaths += deaths(mt, p, Locality.RURAL, Gender.MALE, age);
                total_deaths += deaths(mt, p, Locality.RURAL, Gender.FEMALE, age);
            }

            if (fctx != null)
            {
                total_deaths += deaths(mt, fctx, Locality.URBAN, Gender.MALE);
                total_deaths += deaths(mt, fctx, Locality.RURAL, Gender.MALE);
                total_deaths += deaths(mt, fctx, Locality.URBAN, Gender.FEMALE);
                total_deaths += deaths(mt, fctx, Locality.RURAL, Gender.FEMALE);
            }
        }
        else
        {
            for (int age = 0; age <= MAX_AGE; age++)
            {
                total_deaths += deaths(mt, p, Locality.TOTAL, Gender.MALE, age);
                total_deaths += deaths(mt, p, Locality.TOTAL, Gender.FEMALE, age);
            }

            if (fctx != null)
            {
                total_deaths += deaths(mt, fctx, Locality.TOTAL, Gender.MALE);
                total_deaths += deaths(mt, fctx, Locality.TOTAL, Gender.MALE);
            }
        }
        
        if (cbr > 0)
        {
            total_deaths += deaths_from_births(total_pop, cbr, mt);
        }

        return 1000 * total_deaths / total_pop;
    }

    /*
     * Число смертей за год в части населения @p указанной (@locality, @gender и @age) за грядущий год. 
     */
    private static double deaths(
            final CombinedMortalityTable mt,
            final PopulationByLocality p,
            Locality locality,
            Gender gender,
            int age) throws Exception
    {
        double v = p.get(Locality.TOTAL, Gender.MALE, age);
        MortalityInfo mi = mt.get(Locality.TOTAL, Gender.MALE, age);
        return mi.qx * v;
    }

    /*
     * Число смертей в части детского контекста указанной (@locality и @gender) за грядущий год.
     */
    private static double deaths(
            final CombinedMortalityTable mt,
            final PopulationForwardingContext fctx,
            Locality locality,
            Gender gender) throws Exception
    {
        double[] lx = fctx.get_daily_lx(mt, locality, gender);
        double[] p = fctx.asArray(locality, gender);

        double sum_deaths = 0;
        for (int nd = 0; nd < p.length; nd++)
        {
            sum_deaths += p[nd] * (1 - lx[nd + fctx.DAYS_PER_YEAR] / lx[nd]);
        }

        return sum_deaths;
    }
    
    private static final double MaleFemaleBirthRatio = 1.06;
    
    /*
     * Число смертей от новых рождений за год
     */
    private static double deaths_from_births(final double total_pop, final double cbr, final CombinedMortalityTable mt) throws Exception
    {
        double all_births = total_pop * cbr / 1000; 
        double m_births = all_births * MaleFemaleBirthRatio / (1 + MaleFemaleBirthRatio);
        double f_births = all_births * 1.0 / (1 + MaleFemaleBirthRatio);
        return deaths_from_births(Gender.MALE, m_births, mt) + deaths_from_births(Gender.FEMALE, f_births, mt);
    }

    private static double deaths_from_births(Gender gender, double births, final CombinedMortalityTable mt) throws Exception
    {
        double deaths = 0;
        
        /*
         * распределить рождения по числу дней
         */
        PopulationForwardingContext fctx = new PopulationForwardingContext();
        int ndays = fctx.DAYS_PER_YEAR;
        double[] day_births = new double[ndays];
        for (int nd = 0; nd < ndays; nd++)
            day_births[nd] = births / ndays;

        /*
         * подвергнуть рождения смертности
         * lx[nd] содержит число выживших на день жизни nd согласно таблице смертности,
         * таким образом day_lx[nd] / day_lx[0] представляет долю выживших к этому дню со дня рождения 
         */
        double[] day_lx = fctx.get_daily_lx(mt, Locality.TOTAL, gender);
        for (int nd = 0; nd < ndays; nd++)
            deaths += day_births[nd] * (1 - day_lx[nd] / day_lx[0]);
        return deaths;
    }
}
