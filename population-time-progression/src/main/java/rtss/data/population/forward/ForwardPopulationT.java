package rtss.data.population.forward;

import rtss.data.asfr.AgeSpecificFertilityRates;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.MortalityInfo;
import rtss.data.population.PopulationByLocality;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

/**
 * Продвижка населения по таблице смертности не имеющей отдельных частей
 * для городского и сельского населения, а только часть Total.
 */
public class ForwardPopulationT extends ForwardPopulation
{
    protected AgeSpecificFertilityRates ageSpecificFertilityRates;
    protected double BirthRateTotal;

    private double t_male_births = 0;
    private double t_female_births = 0;
    private double t_male_deaths_from_births = 0;
    private double t_female_deaths_from_births = 0;

    private double p_t_male_deaths = 0;
    private double p_t_female_deaths = 0;

    private double fctx_t_male_deaths = 0;
    private double fctx_t_female_deaths = 0;
    
    /*
     * В настоящее время мы не используем эту функцию практически.
     *     
     * Для целей передвижки требуются либо коэффициенты рождаемости нормированные на население начала года
     * для временного отрезка [0, T] (а не на население середины года с отрезком [-T,-T]).
     * 
     * Либо (и лучше всего) мы можем сделать передвижку двухфазной:
     *   - в первой фазе делается передвижка наличного населения по таблице смертности (без добавления рождений), 
     *     из чего устанавливается среднее за период население (его половозростная структура)
     *   - среднее население -- единствнный результат первой фазы, сугубо внутренней,      
     *   - откуда определяется количество рождений на основе ASFR калиброванных относительно
     *     среднего за период населения
     *   - вторая фаза проводит передвижку с данным количеством рождений, 
     *     результат этой фазы и является окончательным итогом  
     */
    private ForwardPopulationT setBirthRateTotal(AgeSpecificFertilityRates ageSpecificFertilityRates)
    {
        this.ageSpecificFertilityRates = ageSpecificFertilityRates;
        return this;
    }

    /*
     * Для целей передвижки требуется уровень рождаемости нормированный на население начала года
     * для временного отрезка [0, T] (а не на население середины года с отрезком [-T,-T]).    
     */
    public ForwardPopulationT setBirthRateTotal(double BirthRateTotal)
    {
        this.BirthRateTotal = BirthRateTotal;
        return this;
    }

    public double getBirthRateTotal()
    {
        return BirthRateTotal;
    }

    /*
     * Продвижка населения во времени на целый год или на часть года.
     * Начальное население (@p) и таблица смертности (@mt) неизменны. 
     * Результат возвращается как новая структура.
     * При продвижке на целый год @yfraction = 1.0.
     * При продвижке на часть года @yfraction < 1.0.
     */
    public PopulationByLocality forward(final PopulationByLocality p,
            PopulationForwardingContext fctx,
            final CombinedMortalityTable mt,
            final double yfraction)
            throws Exception
    {
        if (debug)
        {
            log(String.format("P = %s", p.toString()));
            log(String.format("FCTX = %s", fctx.toString()));
            log("");
        }

        /* пустая структура для получения результатов */
        PopulationByLocality pto = PopulationByLocality.newPopulationTotalOnly();

        /* продвижка сельского и городского населений, сохранить результат в @pto */
        if (p.hasRuralUrban())
            throw new IllegalArgumentException();
        forward(pto, p, fctx, Locality.TOTAL, mt, yfraction);

        /* проверить внутреннюю согласованность результата */
        pto.validate();

        if (debug)
        {
            log(String.format("Deaths P-TOTAL-MALE => %s", f2s(p_t_male_deaths)));
            log(String.format("Deaths P-TOTAL-FEMALE => %s", f2s(p_t_female_deaths)));
            log(String.format("Deaths FCTX-TOTAL-MALE => %s", f2s(fctx_t_male_deaths)));
            log(String.format("Deaths FCTX-TOTAL-FEMALE => %s", f2s(fctx_t_female_deaths)));
            log("");

            log(String.format("Births TOTAL-MALE = %s", f2s(t_male_births)));
            log(String.format("Births TOTAL-FEMALE = %s", f2s(t_female_births)));
            log("");

            log(String.format("Deaths from births TOTAL-MALE = %s", f2s(t_male_deaths_from_births)));
            log(String.format("Deaths from births TOTAL-FEMALE = %s", f2s(t_female_deaths_from_births)));
            log("");

            log(String.format("Observed births = %s", f2s(this.getObservedBirths())));
            log(String.format("Observed deaths = %s", f2s(this.getObservedDeaths())));
        }

        return pto;
    }

    public void forward(PopulationByLocality pto,
            final PopulationByLocality p,
            PopulationForwardingContext fctx,
            final Locality locality,
            final CombinedMortalityTable mt,
            final double yfraction)
            throws Exception
    {
        /* продвижка мужского и женского населений по смертности из @p в @pto */
        forward(pto, p, fctx, locality, Gender.MALE, mt, yfraction);
        forward(pto, p, fctx, locality, Gender.FEMALE, mt, yfraction);

        /* добавить рождения */
        double birthRate = BirthRateTotal;

        if (fctx != null)
        {
            add_births(fctx, p, locality, mt, ageSpecificFertilityRates, birthRate, yfraction);
        }
        else
        {
            double births;

            if (ageSpecificFertilityRates != null)
            {
                births = yfraction * ageSpecificFertilityRates.births(p);
            }
            else
            {
                double sum = p.sum(locality, Gender.BOTH, 0, MAX_AGE);
                births = yfraction * sum * birthRate / 1000;
            }

            observed_births += births;

            double m_births = births * MaleFemaleBirthRatio / (1 + MaleFemaleBirthRatio);
            double f_births = births * 1.0 / (1 + MaleFemaleBirthRatio);
            
            if (debug)
            {
                log(String.format("Births TOTAL-MALE = %s", f2s(m_births)));
                log(String.format("Births TOTAL-FEMALE = %s", f2s(f_births)));
            }

            pto.add(locality, Gender.MALE, 0, m_births);
            pto.add(locality, Gender.FEMALE, 0, f_births);

            if (Util.True)
            {
                // TODO: подвергнуь смертности
                throw new Exception("use fctx != null");
            }
        }

        /* вычислить графу "оба пола" из отдельных граф для мужчин и женщин */
        pto.makeBoth(locality);
    }

    private void add_births(PopulationForwardingContext fctx,
            final PopulationByLocality p,
            final Locality locality,
            final CombinedMortalityTable mt,
            final AgeSpecificFertilityRates ageSpecificFertilityRates,
            final double birthRate,
            final double yfraction) throws Exception
    {
        double births;

        if (ageSpecificFertilityRates != null)
        {
            births = yfraction * ageSpecificFertilityRates.births(p);
        }
        else
        {
            double sum = p.sum(locality, Gender.BOTH, 0, MAX_AGE) + fctx.sumAllAges(locality, Gender.BOTH);
            births = yfraction * sum * birthRate / 1000;
        }

        observed_births += births;

        double m_births = births * MaleFemaleBirthRatio / (1 + MaleFemaleBirthRatio);
        double f_births = births * 1.0 / (1 + MaleFemaleBirthRatio);

        int ndays = (int) Math.round(yfraction * fctx.DAYS_PER_YEAR);
        ndays = Math.max(1, ndays);

        add_births(fctx, locality, Gender.MALE, m_births, mt, ndays);
        add_births(fctx, locality, Gender.FEMALE, f_births, mt, ndays);
    }

    private void add_births(PopulationForwardingContext fctx,
            final Locality locality,
            final Gender gender,
            double total_births,
            final CombinedMortalityTable mt,
            final int ndays) throws Exception
    {
        fctx.addTotalBirths(locality, gender, total_births);

        /*
         * распределить рождения равномерно по числу дней
         */
        double[] day_births = new double[ndays];
        for (int nd = 0; nd < ndays; nd++)
            day_births[nd] = total_births / ndays;

        /*
         * подвергнуть рождения смертности
         * lx[nd] содержит число выживших на день жизни nd согласно таблице смертности,
         * таким образом day_lx[nd] / day_lx[0] представляет долю выживших к этому дню со дня рождения 
         */
        double[] day_lx = fctx.get_daily_lx(mt, locality, gender);
        for (int nd = 0; nd < ndays; nd++)
            day_births[nd] *= day_lx[nd] / day_lx[0];

        /*
         * добавить результат в контекст
         */
        for (int nd = 0; nd < ndays; nd++)
            fctx.add(locality, gender, nd, day_births[nd]);
        
        double deaths_from_births = total_births - Util.sum(day_births);
        observed_deaths += deaths_from_births;
        
        switch (gender)
        {
        case MALE:
            t_male_births += total_births;
            t_male_deaths_from_births += deaths_from_births;
            break;

        case FEMALE:
            t_female_births += total_births;
            t_female_deaths_from_births += deaths_from_births;
            break;

        case BOTH:
            throw new IllegalArgumentException();
        }

        if (debug)
        {
            log(String.format("Births TOTAL-%s = %s", gender.name(), f2s(total_births)));
            // log(String.format("Deaths from births TOTAL-%s = %s", gender.name(), f2s(deaths_from_births)));
        }
    }

    public void forward(PopulationByLocality pto,
            final PopulationByLocality p,
            PopulationForwardingContext fctx,
            final Locality locality,
            final Gender gender,
            final CombinedMortalityTable mt,
            final double yfraction)
            throws Exception
    {
        /* рождений пока нет */
        pto.set(locality, gender, 0, 0);
        
        double sum_deaths = 0;

        /* Продвижка по таблице смертности.
         * 
         * Для каждого года возраста мы берём часть населения этого возраста
         * указываемую @yfraction и переносим её в следующий год с приложением смертности.
         * Остальная часть (1.0 - yfraction) остаётся в текущем возрасте и не
         * подвергается смертности.
         */
        for (int age = 0; age <= MAX_AGE; age++)
        {
            MortalityInfo mi = mt.get(locality, gender, age);
            double current = p.get(locality, gender, age);

            double moving = current * yfraction;
            double staying = current - moving;
            double deaths = moving * (1.0 - mi.px);

            observed_deaths += deaths;
            sum_deaths += deaths;

            pto.add(locality, gender, age, staying);
            pto.add(locality, gender, Math.min(MAX_AGE, age + 1), moving - deaths);
        }
        
        switch (gender.name())
        {
        case "MALE":
            p_t_male_deaths += sum_deaths;
            break;

        case "FEMALE":
            p_t_female_deaths += sum_deaths;
            break;
        }

        /*
         * Продвижка контекста ранней детской смертности
         */
        if (fctx != null)
        {
            forward_context(pto, fctx, locality, gender, mt, yfraction);
        }
    }

    private void forward_context(
            PopulationByLocality pto,
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

        for (int nd = 0; nd < p.length; nd++)
        {
            int nd2 = nd + ndays;

            double v = p[nd];
            double v_initial = v;

            if (nd2 < p2.length)
            {
                v *= day_lx[nd2] / day_lx[nd];
                p2[nd2] = v;
                sum_deaths += v_initial - v;
            }
            else
            {
                int age = fctx.day2age(nd2);
                nd2 = age * fctx.DAYS_PER_YEAR;
                v *= day_lx[nd2] / day_lx[nd];
                pto.add(locality, gender, age, v);
                sum_deaths += v_initial - v;
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

    /*****************************************************************************************/

    protected void show_shortfall_header()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("       ");
        sb.append(COLUMN_DIVIDER);
        sb.append("   RURAL and URBAN   ");
        sb.append("    RURAL and URBAN   ");
        sb.append("    RURAL and URBAN   ");
        Util.out(sb.toString());

        sb.setLength(0);
        sb.append("  Age  ");
        for (int k = 0; k < 1; k++)
        {
            sb.append(COLUMN_DIVIDER);
            sb.append("         MALE        ");
            sb.append("         FEMALE       ");
            sb.append("     MALE + FEMALE    ");
        }
        Util.out(sb.toString());

        sb.setLength(0);
        sb.append("=======");
        for (int k = 0; k < 1; k++)
        {
            sb.append(COLUMN_DIVIDER);
            sb.append(" ====================");
            sb.append("  ====================");
            sb.append("  ====================");
        }
        Util.out(sb.toString());
    }

    private static final String COLUMN_DIVIDER = "  ‖ ";

    protected void show_shortfall(PopulationByLocality pExpected, PopulationByLocality pActual, int age1, int age2) throws Exception
    {
        String s = String.format("%d-%d", age1, age2);
        if (age1 == age2 & age1 == MAX_AGE)
            s = String.format("%d+", age1);
        s = String.format("%7s", s);
        StringBuilder sb = new StringBuilder(s);

        sb.append(COLUMN_DIVIDER);
        show_shortfall(sb, pExpected, pActual, Locality.TOTAL, Gender.MALE, age1, age2, "");
        show_shortfall(sb, pExpected, pActual, Locality.TOTAL, Gender.FEMALE, age1, age2, " ");
        show_shortfall(sb, pExpected, pActual, Locality.TOTAL, Gender.BOTH, age1, age2, " ");

        Util.out(sb.toString());
    }

    protected void show_shortfall(StringBuilder sb,
            PopulationByLocality pExpected,
            PopulationByLocality pActual,
            Locality locality,
            Gender gender,
            int age1, int age2,
            String prefix)
            throws Exception
    {
        double expected = pExpected.sum(locality, gender, age1, age2);
        double actual = pActual.sum(locality, gender, age1, age2);
        double deficit = expected - actual;
        double deficit_pct = 100 * deficit / expected;
        String s = String.format("%,10d (%7.2f%%)", Math.round(deficit), deficit_pct);
        sb.append(prefix);
        sb.append(s);
    }


    private String f2s(double v)
    {
        String s = String.format("%,15.0f", v);
        while (s.startsWith(" "))
            s = s.substring(1);
        return s;
    }
}
