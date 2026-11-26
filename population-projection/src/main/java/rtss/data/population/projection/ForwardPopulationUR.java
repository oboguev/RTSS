package rtss.data.population.projection;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableDouble;

import rtss.data.asfr.AgeSpecificFertilityRates;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.MortalityInfo;
import rtss.data.population.projection.helper.CalcBirths;
import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

/**
 * Передвижка населения по таблице смертности имеющей отдельные части
 * для городского и сельского населения
 * 
 * ***************************************************************************************
 * 
 * При передвижке на несколько сегментов с длительностью не кратной году следует 
 * 
 * 1. Использовать PopulationContext с размером PopulationContext.ALL_AGES.
 *    В этом случае все данные о населении загружаются в контекст и отслеживаются по возрасту
 *    отчитываемому в днях, а не в годах.
 *    
 * 2. При использовании PopulationContext с детским размером (напр. 5 лет) и последовательной
 *    передвижке по полугодиям или иным отрезкам короче года, следует сначала передвинуть на отрезки равные
 *    году, а затем от этих реперных годовых точек отсчитывать передвижку до точек внутри года.
 *    
 *    Причину легко видеть на примере передвижки населения целиком сосредоточенного в одном годе возраста
 *    и без смертности.  Правильная передвижка населения такого с возрастным распределением (100, 0) на год 
 *    должна дать (0, 100).
 *    
 *    Однако передвижка на полгода даст (50, 50).
 *    Затем следующая передвижка ещё на полгода даст (25, 25+25 = 50, 25).
 *    Т.е. приведёт к размытию распределения из-за утраты информации о датах рождения.
 * 
 * ***************************************************************************************
 *
 * Когда часть населения уже в возрасте MAX_AGE должна (выжив) перейти в более старший возраст
 * MAX_AGE + 1, она остаётся в MAX_AGE.
 *   
 */
public class ForwardPopulationUR extends ForwardPopulation
{
    protected AgeSpecificFertilityRates ageSpecificFertilityRatesRural;
    protected AgeSpecificFertilityRates ageSpecificFertilityRatesUrban;

    protected double BirthRateRural;
    protected double BirthRateUrban;

    private double ur_male_births = 0;
    private double ur_female_births = 0;
    private double ur_male_deaths_from_births = 0;
    private double ur_female_deaths_from_births = 0;

    private double p_u_male_deaths = 0;
    private double p_r_male_deaths = 0;
    private double p_u_female_deaths = 0;
    private double p_r_female_deaths = 0;

    private double fctx_u_male_deaths = 0;
    private double fctx_r_male_deaths = 0;
    private double fctx_u_female_deaths = 0;
    private double fctx_r_female_deaths = 0;

    /*
     * Число смертей по типу местности, полу и возрасту в днях на начало передвижки.
     * Смерти накапливаются при последовательных вызовах forward().
     * Для рождённых во время передвижки см. newbornDeathRegistrationAge.
     */
    private final PopulationContext deathsByGenderAge = PopulationContext.newURPopulationContext();

    /*
     * Регистрация смертей в deathsByGenderAge индексируется по возрасту на момент НАЧАЛА передвижки,
     * начиная с дня 0 для только что родившихся, т.е. характеризует население имевшееся на момент
     * начала передвижки и даёт числа смертей для его возрастов.
     *
     * Но младенцы родившиеся и затем умершие уже в период передвижки имеют отрицательный возраст
     * относительно начала передвижки.
     *
     * Наша структура не поддерживает индексацию по отрицательным возрастам.
     *
     * Мы поэтому можем либо отнести смерти новорожденных либо на возраст день 0,
     * либо (чтобы избежать аккумуляции) на зеркально отражённый возраст.
     *
     * TODO:
     * Требует исправления структуры или двойной структуры.
     * Взамен можно пользоваться deathsByGenderAgeAtDeath.
     */
    public static enum NewbornDeathRegistrationAge
    {
        AT_AGE_DAY0, MIRROR_AGE
    }

    private NewbornDeathRegistrationAge newbornDeathRegistrationAge = NewbornDeathRegistrationAge.AT_AGE_DAY0;

    public void setNewbornDeathRegistrationAge(NewbornDeathRegistrationAge newbornDeathRegistrationAge)
    {
        this.newbornDeathRegistrationAge = newbornDeathRegistrationAge;
    }

    private int newbornDeathDay(int nd)
    {
        switch (newbornDeathRegistrationAge)
        {
        case AT_AGE_DAY0:
            return 0;
        case MIRROR_AGE:
            return nd;
        default:
            throw new IllegalArgumentException("неверная установка для newbornDeathRegistrationAge");
        }
    }

    public PopulationContext deathsByGenderAge()
    {
        return deathsByGenderAge;
    }

    /* Deaths by locality, sex and age in completed days at death, accumulated across forward() calls. */
    private final PopulationContext deathsByGenderAgeAtDeath = PopulationContext.newURPopulationContext();

    /* Both flags default to false: preserve legacy execution unless explicitly enabled. */
    private boolean recordDeathsByGenderAgeAtDeath = false;
    private boolean bufferDeathsByGenderAgeAtDeath = false;

    /* Enable before the first forward() call to obtain a complete cumulative distribution.
     * Disabling recording does not clear deaths already recorded. */
    public void setRecordDeathsByGenderAgeAtDeath(boolean enable)
    {
        this.recordDeathsByGenderAgeAtDeath = enable;
    }

    /* Select direct addDay() calls or a per-locality-and-sex temporary double[] buffer.
     * This flag has no effect when recording is disabled. */
    public void setBufferDeathsByGenderAgeAtDeath(boolean enable)
    {
        this.bufferDeathsByGenderAgeAtDeath = enable;
    }

    public PopulationContext deathsByGenderAgeAtDeath()
    {
        return deathsByGenderAgeAtDeath;
    }

    /* Independent cumulative newborn-death totals for validation. The legacy
     * ur_*_deaths_from_births counters combine urban and rural deaths. */
    private final double[][] newbornDeathsByLocalityGender =
            new double[Locality.values().length][Gender.values().length];

    public ForwardPopulationUR setBirthRateRural(AgeSpecificFertilityRates ageSpecificFertilityRates)
    {
        this.ageSpecificFertilityRatesRural = ageSpecificFertilityRates;
        return this;
    }

    public ForwardPopulationUR setBirthRateUrban(AgeSpecificFertilityRates ageSpecificFertilityRates)
    {
        this.ageSpecificFertilityRatesUrban = ageSpecificFertilityRates;
        return this;
    }

    private boolean usesASFR() throws Exception
    {
        if ((ageSpecificFertilityRatesUrban != null) != (ageSpecificFertilityRatesRural != null))
            throw new Exception("ASFR specified only for one locality");
        return ageSpecificFertilityRatesUrban != null;
    }

    public void setBirthRateRural(double rate)
    {
        this.BirthRateRural = rate;
    }

    public void setBirthRateUrban(double rate)
    {
        this.BirthRateUrban = rate;
    }

    /*
     * Оценить долю городского населения во всём населении (для указанного пола) 
     */
    public double urban_fraction(PopulationByLocality p, PopulationContext fctx, Gender gender) throws Exception
    {
        double urban = p.sum(Locality.URBAN, gender, 0, MAX_AGE);
        double total = p.sum(Locality.TOTAL, gender, 0, MAX_AGE);

        if (fctx != null)
        {
            urban += fctx.sumDays(Locality.URBAN, gender, 0, fctx.MAX_DAY);
            total += fctx.sumDays(Locality.TOTAL, gender, 0, fctx.MAX_DAY);
        }

        return urban / total;
    }

    /*
     * Линейная интерполяция между двумя точками
     */
    public Map<Integer, Double> interpolate_linear(int y1, double v1, int y2, double v2)
            throws Exception
    {
        Map<Integer, Double> m = new HashMap<>();

        if (y1 > y2)
            throw new Exception("Invalid arguments");

        if (y1 == y2)
        {
            if (v1 != v2)
                throw new Exception("Invalid arguments");
            m.put(y1, v1);
        }
        else
        {
            for (int y = y1; y <= y2; y++)
            {
                double v = v1 + (v2 - v1) * (y - y1) / (y2 - y1);
                m.put(y, v);
            }
        }

        return m;
    }

    /*
     * Передвижка населения во времени на целый год или на часть года.
     * Начальное население (@p) и таблица смертности (@mt) неизменны. 
     * Результат возвращается как новая структура.
     * При передвижке на целый год @yfraction = 1.0.
     * При передвижке на часть года @yfraction < 1.0.
     */
    public PopulationByLocality forward(final PopulationByLocality p,
            PopulationContext fctx,
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
        PopulationByLocality pto = PopulationByLocality.newPopulationByLocality();

        /* передвижка сельского и городского населений, сохранить результат в @pto */
        if (!p.hasRuralUrban())
            throw new IllegalArgumentException();
        forward(pto, p, fctx, Locality.RURAL, mt, yfraction);
        forward(pto, p, fctx, Locality.URBAN, mt, yfraction);

        /* вычислить совокупное население обеих зон суммированием городского и сельского */
        pto.recalcTotalLocalityFromUrbanRural();

        /* проверить внутреннюю согласованность результата */
        pto.validate();

        if (debug)
        {
            log("");
            log(String.format("Deaths P-U-MALE => %s", f2s(p_u_male_deaths)));
            log(String.format("Deaths P-R-MALE => %s", f2s(p_r_male_deaths)));
            log(String.format("Deaths P-UR-MALE => %s", f2s(p_u_male_deaths + p_r_male_deaths)));
            log("");

            log(String.format("Deaths P-U-FEMALE => %s", f2s(p_u_female_deaths)));
            log(String.format("Deaths P-R-FEMALE => %s", f2s(p_r_female_deaths)));
            log(String.format("Deaths P-UR-FEMALE => %s", f2s(p_u_female_deaths + p_r_female_deaths)));
            log("");

            log(String.format("Deaths FCTX-U-MALE => %s", f2s(fctx_u_male_deaths)));
            log(String.format("Deaths FCTX-R-MALE => %s", f2s(fctx_r_male_deaths)));
            log(String.format("Deaths FCTX-UR-MALE => %s", f2s(fctx_u_male_deaths + fctx_r_male_deaths)));
            log("");

            log(String.format("Deaths FCTX-U-FEMALE => %s", f2s(fctx_u_female_deaths)));
            log(String.format("Deaths FCTX-R-FEMALE => %s", f2s(fctx_r_female_deaths)));
            log(String.format("Deaths FCTX-UR-FEMALE => %s", f2s(fctx_u_female_deaths + fctx_r_female_deaths)));
            log("");

            log(String.format("Births TOTAL-MALE = %s", f2s(ur_male_births)));
            log(String.format("Births TOTAL-FEMALE = %s", f2s(ur_female_births)));
            log("");

            log(String.format("Deaths from births TOTAL-MALE = %s", f2s(ur_male_deaths_from_births)));
            log(String.format("Deaths from births TOTAL-FEMALE = %s", f2s(ur_female_deaths_from_births)));
            log("");

            log(String.format("Observed births = %s", f2s(this.getObservedBirths())));
            log(String.format("Observed deaths = %s", f2s(this.getObservedDeaths())));
        }

        return pto;
    }

    public void forward(
            PopulationContext fctx,
            final CombinedMortalityTable mt,
            final double yfraction)
            throws Exception
    {
        if (fctx.MAX_YEAR != MAX_AGE)
            throw new IllegalArgumentException("население не перегружено целиком в PopulationContext, используйте PopulationContext.ALL_AGES");

        PopulationByLocality p = PopulationByLocality.newPopulationByLocality();
        p.zero();
        PopulationByLocality pto = forward(p, fctx, mt, yfraction);
        Util.assertion(pto.sum() == 0);
    }

    public void forward(PopulationByLocality pto,
            final PopulationByLocality p,
            PopulationContext fctx,
            final Locality locality,
            final CombinedMortalityTable mt,
            final double yfraction)
            throws Exception
    {
        if (usesASFR())
        {
            forwardWithASFR(pto, p, fctx, locality, mt, yfraction);
            return;
        }

        PopulationContext fctx_initial = (fctx != null) ? fctx.clone() : null;

        /* передвижка мужского и женского населений по смертности из @p в @pto */
        forward(pto, p, fctx, locality, Gender.MALE, mt, yfraction);
        forward(pto, p, fctx, locality, Gender.FEMALE, mt, yfraction);

        /* добавить рождения */
        double birthRate;
        switch (locality)
        {
        case RURAL:
            birthRate = BirthRateRural;
            break;
        case URBAN:
            birthRate = BirthRateUrban;
            break;
        default:
            throw new Exception("Invalid locality");
        }

        if (fctx != null)
        {
            add_births(fctx_initial, fctx, p, locality, mt, birthRate, yfraction);
        }
        else
        {
            double sum = p.sum(locality, Gender.BOTH, 0, MAX_AGE);
            double births = sum * yfraction * birthRate / 1000;

            observed_births += births;

            double m_births = births * MaleFemaleBirthRatio / (1 + MaleFemaleBirthRatio);
            double f_births = births * 1.0 / (1 + MaleFemaleBirthRatio);

            if (debug)
            {
                log(String.format("Births %s-MALE = %s", locality.code(), f2s(m_births)));
                log(String.format("Births %s-FEMALE = %s", locality.code(), f2s(f_births)));
            }

            pto.add(locality, Gender.MALE, 0, m_births);
            pto.add(locality, Gender.FEMALE, 0, f_births);

            if (Util.True)
            {
                // TODO: подвергнуь рождеения смертности
                throw new Exception("use fctx != null");
            }
        }

        /* вычислить графу "оба пола" из отдельных граф для мужчин и женщин */
        pto.makeBoth(locality);
    }

    private void forwardWithASFR(PopulationByLocality pto,
            final PopulationByLocality p,
            PopulationContext fctx,
            final Locality locality,
            final CombinedMortalityTable mt,
            final double yfraction)
            throws Exception
    {
        /*
         * Мы выполняем двухфазную передвижку:
         * 
         *   - в первой фазе делается передвижка наличного населения по таблице смертности (без добавления рождений), 
         *     из чего устанавливается среднее за период население (его половозростная структура)
         *     
         *   - среднее население -- единстевнный результат первой фазы, сугубо внутренней,
         *     откуда определяется количество рождений на основе ASFR калиброванных относительно
         *     среднего за период населения
         *     
         *   - уточнение: на деле мы используем не среднее население, а интерполируем возрастные
         *     линии от начальгого до конечного значения с разрешением 1 день по экспоненциальному закону, 
         *     и для каждого дня определяем количество рождений от этой линии с учётом возраста линии в данный день,
         *     её численности в данный день и ASFR; это даёт число рождений в данный день   
         *     
         *   - вторая фаза проводит передвижку с вычисленным количеством рождений (массив значений для каждого дня), 
         *     результат второй фазы и становится окончательным итогом  
         */

        /*
         * Первая фаза
         */
        PopulationContext fctx1;
        if (fctx == null)
            fctx1 = new PopulationContext(PopulationContext.ALL_AGES);
        else
            fctx1 = fctx.cloneToMaxAge();

        fctx1.add(p.toPopulationContext());
        PopulationContext fctx2 = fctx1.clone();

        PopulationByLocality p1 = PopulationByLocality.newPopulationByLocality();
        p1.zero();
        
        PopulationByLocality pto1 = PopulationByLocality.newPopulationByLocality();
        pto1.zero();
        
        ForwardPopulationUR fw = new ForwardPopulationUR();
        fw.forward(pto1, p1, fctx2, locality, mt, yfraction);
        Util.assertion(pto1.sum() == 0);

        /*
         * fctx1 - население до передвижки первой фазы
         * fctx2 - население после передвижки первой фазы
         */

        int ndays = years2days(yfraction);
        ndays = Math.max(1, ndays);

        AgeSpecificFertilityRates asfrForLocality = (locality == Locality.URBAN) ? ageSpecificFertilityRatesUrban : ageSpecificFertilityRatesRural;

        // интерполировать возрастные линии и для каждого дня (c учётом старения) вычислить женское насееение, 
        // приложить ASFR и получить число рождений в данный день
        MutableDouble fertile_female_population = new MutableDouble();
        double[] day_births = CalcBirths.eval_day_births(fctx1, fctx2, ndays, locality, asfrForLocality, fertile_female_population);

        double[] m_births = Util.multiply(day_births, MaleFemaleBirthRatio / (1 + MaleFemaleBirthRatio));
        double[] f_births = Util.multiply(day_births, 1.0 / (1 + MaleFemaleBirthRatio));

        /*
         * Вторая фаза
         */
        /* передвижка мужского и женского населений по смертности из @p в @pto */
        forward(pto, p, fctx, locality, Gender.MALE, mt, yfraction);
        forward(pto, p, fctx, locality, Gender.FEMALE, mt, yfraction);

        if (fctx != null)
        {
            add_births(fctx, locality, Gender.MALE, m_births, mt, ndays);
            add_births(fctx, locality, Gender.FEMALE, f_births, mt, ndays);
        }
        else
        {
            observed_births += Util.sum(day_births);

            if (debug)
            {
                log(String.format("Births %s-MALE = %s", locality.code(), f2s(Util.sum(m_births))));
                log(String.format("Births %s-FEMALE = %s", locality.code(), f2s(Util.sum(f_births))));
            }

            pto.add(locality, Gender.MALE, 0, Util.sum(m_births));
            pto.add(locality, Gender.FEMALE, 0, Util.sum(f_births));

            if (Util.True)
            {
                // TODO: подвергнуь рождения смертности
                throw new Exception("use fctx != null");
            }
        }

        /* вычислить графу "оба пола" из отдельных граф для мужчин и женщин */
        pto.makeBoth(locality);

        Util.noop();
    }

    private void add_births(PopulationContext fctx_initial,
            PopulationContext fctx,
            final PopulationByLocality p,
            final Locality locality,
            final CombinedMortalityTable mt,
            final double birthRate,
            final double yfraction) throws Exception
    {
        double sum = p.sum(locality, Gender.BOTH, 0, MAX_AGE) + fctx_initial.sumAllAges(locality, Gender.BOTH);
        double births = sum * yfraction * birthRate / 1000;
        double m_births = births * MaleFemaleBirthRatio / (1 + MaleFemaleBirthRatio);
        double f_births = births * 1.0 / (1 + MaleFemaleBirthRatio);

        int ndays = years2days(yfraction);
        ndays = Math.max(1, ndays);

        add_births(fctx, locality, Gender.MALE, m_births, mt, ndays);
        add_births(fctx, locality, Gender.FEMALE, f_births, mt, ndays);
    }

    private void add_births(PopulationContext fctx,
            final Locality locality,
            final Gender gender,
            double total_births,
            final CombinedMortalityTable mt,
            final int ndays) throws Exception
    {
        /*
         * распределить рождения по числу дней
         */
        double[] day_births = new double[ndays];
        for (int nd = 0; nd < ndays; nd++)
            day_births[nd] = total_births / ndays;

        add_births(fctx, locality, gender, day_births, mt, ndays);
    }

    private void add_births(PopulationContext fctx,
            final Locality locality,
            final Gender gender,
            double[] day_births,
            final CombinedMortalityTable mt,
            final int ndays) throws Exception
    {
        double total_births = Util.sum(day_births);
        observed_births += total_births;
        fctx.addTotalBirths(locality, gender, total_births);

        /*
         * подвергнуть рождения смертности
         * lx[nd] содержит число выживших на день жизни nd согласно таблице смертности,
         * таким образом day_lx[nd] / day_lx[0] представляет долю выживших к этому дню со дня рождения 
         */
        /* Preserve the original birth cohorts for optional age-at-death registration. */
        double[] initial_day_births = day_births;
        day_births = Util.dup(day_births);
        double[] day_lx = fctx.get_daily_lx(mt, locality, gender);
        for (int nd = 0; nd < ndays; nd++)
            day_births[nd] *= day_lx[nd] / day_lx[0];

        /*
         * добавить результат в контекст
         */
        for (int nd = 0; nd < ndays; nd++)
            fctx.addDay(locality, gender, nd, day_births[nd]);

        double deaths_from_births = total_births - Util.sum(day_births);
        observed_deaths += deaths_from_births;

        /* Register newborn deaths by their conventional age at the start of forwarding. */
        recordNewbornDeathsByGenderAge(locality, gender, initial_day_births,
                                       day_births, ndays, deaths_from_births);

        /* Use the existing aggregate as the authoritative newborn death count. */
        newbornDeathsByLocalityGender[locality.ordinal()][gender.ordinal()] += deaths_from_births;
        if (recordDeathsByGenderAgeAtDeath)
            recordNewbornDeathsAtDeathAge(locality, gender, day_lx, initial_day_births,
                                          day_births, ndays, deaths_from_births);

        switch (gender)
        {
        case MALE:
            ur_male_births += total_births;
            ur_male_deaths_from_births += deaths_from_births;
            break;

        case FEMALE:
            ur_female_births += total_births;
            ur_female_deaths_from_births += deaths_from_births;
            break;

        case BOTH:
            throw new IllegalArgumentException();
        }

        if (debug)
        {
            log(String.format("Births %s-%s = %s", locality.code(), gender.name(), f2s(total_births)));
            log(String.format("Deaths from births %s-%s = %s", locality.code(), gender.name(), f2s(deaths_from_births)));
        }
    }

    public void forward(PopulationByLocality pto,
            final PopulationByLocality p,
            PopulationContext fctx,
            final Locality locality,
            final Gender gender,
            final CombinedMortalityTable mt,
            final double yfraction)
            throws Exception
    {
        /* рождений пока нет */
        pto.set(locality, gender, 0, 0);

        double sum_deaths = 0;
        DeathAgeRecorder recorder = newDeathAgeRecorder(locality, gender);

        /* Передвижка по таблице смертности.
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
            double deaths = moving * mi.qx;

            observed_deaths += deaths;
            sum_deaths += deaths;
            /* Annual-age legacy population: use the first day of the initial year of age. */
            deathsByGenderAge.addDay(locality, gender, age * DAYS_PER_YEAR, deaths);
            if (recorder != null)
                recordYearlyDeathsAtDeathAge(recorder, age, deaths);

            pto.add(locality, gender, age, staying);
            pto.add(locality, gender, Math.min(MAX_AGE, age + 1), moving - deaths);
        }

        if (recorder != null)
            recorder.flush();

        switch (locality.name() + "-" + gender.name())
        {
        case "URBAN-MALE":
            p_u_male_deaths += sum_deaths;
            break;

        case "URBAN-FEMALE":
            p_u_female_deaths += sum_deaths;
            break;

        case "RURAL-MALE":
            p_r_male_deaths += sum_deaths;
            break;

        case "RURAL-FEMALE":
            p_r_female_deaths += sum_deaths;
            break;
        }

        /*
         * Передвижка контекста ранней детской смертности
         */
        if (fctx != null)
        {
            forward_context(pto, fctx, locality, gender, mt, yfraction);
        }
    }

    private void forward_context(
            PopulationByLocality pto,
            PopulationContext fctx,
            final Locality locality,
            final Gender gender,
            final CombinedMortalityTable mt,
            final double yfraction)
            throws Exception
    {
        int ndays = years2days(yfraction);
        if (ndays == 0)
            return;

        double[] day_lx = fctx.get_daily_lx(mt, locality, gender);
        /* lx[nd] содержит число выживших на день жизни nd согласно таблице смертности */

        double[] p = fctx.asArray(locality, gender);
        double[] p2 = new double[p.length];

        double sum_deaths = 0;
        DeathAgeRecorder recorder = newDeathAgeRecorder(locality, gender);

        for (int nd = 0; nd < p.length; nd++)
        {
            int nd2 = nd + ndays;

            double v = p[nd];
            double v_initial = v;

            Util.assertion(v >= 0);

            if (nd2 < p2.length)
            {
                v *= survivalRate(fctx, day_lx, nd, nd2);
                if (recorder != null)
                    recordForwardDeathsAtDeathAge(recorder, fctx, day_lx, nd, nd2, nd, v_initial - v);
                p2[nd2] = v;

                // double deaths = v * deathRate(fctx, day_lx, nd, nd2);
            }
            else if (fctx.NYEARS < PopulationContext.ALL_AGES)
            {
                int age = fctx.day2age(nd2);
                nd2 = age * fctx.DAYS_PER_YEAR;

                v *= survivalRate(fctx, day_lx, nd, nd2);
                if (recorder != null)
                    recordForwardDeathsAtDeathAge(recorder, fctx, day_lx, nd, nd2, nd, v_initial - v);
                pto.add(locality, gender, Math.min(age, MAX_AGE), v);

                // double deaths = v * deathRate(fctx, day_lx, nd, nd2);
                // deathsByGenderAge.addDay(locality, gender, nd, deaths);
            }
            else
            {
                int back = fctx.DAYS_PER_YEAR;

                v *= survivalRate(fctx, day_lx, nd - back, nd2 - back);
                if (recorder != null)
                    recordForwardDeathsAtDeathAge(recorder, fctx, day_lx, nd - back, nd2 - back, nd, v_initial - v);
                p2[p2.length - 1] += v;

                // double deaths = v * deathRate(fctx, day_lx, nd - back, nd2 - back);
                // deathsByGenderAge.addDay(locality, gender, nd, deaths);
            }

            /* All three branches above use nd as the actual initial age, even when lx is shifted. */
            deathsByGenderAge.addDay(locality, gender, nd, v_initial - v);
            sum_deaths += v_initial - v;
        }

        if (recorder != null)
            recorder.flush();

        fctx.fromArray(locality, gender, p2);

        switch (locality.name() + "-" + gender.name())
        {
        case "URBAN-MALE":
            fctx_u_male_deaths += sum_deaths;
            break;

        case "URBAN-FEMALE":
            fctx_u_female_deaths += sum_deaths;
            break;

        case "RURAL-MALE":
            fctx_r_male_deaths += sum_deaths;
            break;

        case "RURAL-FEMALE":
            fctx_r_female_deaths += sum_deaths;
            break;
        }

        observed_deaths += sum_deaths;
    }

    /*
     * Register the newborn deaths using the same age convention as ForwardPopulationT.
     * Both arrays are indexed by age at the END of forwarding. The mirrored age
     * is therefore ndays - 1 - nd, not nd.
     *
     * UR computes the authoritative total as total births minus total survivors.
     * In MIRROR_AGE mode normalize the individual cohort losses to this total,
     * just as recordNewbornDeathsAtDeathAge() does. In AT_AGE_DAY0 mode there is
     * no need to distribute the total over individual birth cohorts.
     * This registration is independent of the optional age-at-death recorder.
     */
    private void recordNewbornDeathsByGenderAge(Locality locality, Gender gender,
            double[] births, double[] survivors, int ndays, double deaths) throws Exception
    {
        if (deaths == 0)
            return;

        if (newbornDeathRegistrationAge == NewbornDeathRegistrationAge.AT_AGE_DAY0)
        {
            deathsByGenderAge.addDay(locality, gender, 0, deaths);
            return;
        }

        double cohortLossSum = 0;
        for (int nd = 0; nd < ndays; nd++)
            cohortLossSum += births[nd] - survivors[nd];

        Util.assertion(cohortLossSum > 0);

        for (int nd = 0; nd < ndays; nd++)
        {
            double cohortLoss = births[nd] - survivors[nd];
            if (cohortLoss != 0)
            {
                int death_day = newbornDeathDay(ndays - 1 - nd);
                deathsByGenderAge.addDay(locality, gender, death_day,
                                         deaths * (cohortLoss / cohortLossSum));
            }
        }
    }

    /*
     * Validate the always-enabled registration by initial age independently of
     * the optional registration by age at death. Check all nine combinations
     * of locality and gender against the existing cumulative death counters.
     */
    public void validateDeathsByGenderAge() throws Exception
    {
        double um = p_u_male_deaths + fctx_u_male_deaths
                + newbornDeathsByLocalityGender[Locality.URBAN.ordinal()][Gender.MALE.ordinal()];
        double uf = p_u_female_deaths + fctx_u_female_deaths
                + newbornDeathsByLocalityGender[Locality.URBAN.ordinal()][Gender.FEMALE.ordinal()];
        double rm = p_r_male_deaths + fctx_r_male_deaths
                + newbornDeathsByLocalityGender[Locality.RURAL.ordinal()][Gender.MALE.ordinal()];
        double rf = p_r_female_deaths + fctx_r_female_deaths
                + newbornDeathsByLocalityGender[Locality.RURAL.ordinal()][Gender.FEMALE.ordinal()];

        validateDeathsByGenderAge(Locality.TOTAL, Gender.BOTH, getObservedDeaths());
        validateDeathsByGenderAge(Locality.TOTAL, Gender.MALE,
                p_u_male_deaths + p_r_male_deaths + fctx_u_male_deaths + fctx_r_male_deaths
                + ur_male_deaths_from_births);
        validateDeathsByGenderAge(Locality.TOTAL, Gender.FEMALE,
                p_u_female_deaths + p_r_female_deaths + fctx_u_female_deaths + fctx_r_female_deaths
                + ur_female_deaths_from_births);
        validateDeathsByGenderAge(Locality.URBAN, Gender.MALE, um);
        validateDeathsByGenderAge(Locality.URBAN, Gender.FEMALE, uf);
        validateDeathsByGenderAge(Locality.RURAL, Gender.MALE, rm);
        validateDeathsByGenderAge(Locality.RURAL, Gender.FEMALE, rf);
        validateDeathsByGenderAge(Locality.URBAN, Gender.BOTH, um + uf);
        validateDeathsByGenderAge(Locality.RURAL, Gender.BOTH, rm + rf);
    }

    private void validateDeathsByGenderAge(Locality locality, Gender gender, double expected) throws Exception
    {
        double actual = deathsByGenderAge.sumAllAges(locality, gender);
        if (!Util.same(expected, actual))
            throw new Exception(String.format(
                    "Deaths by initial age mismatch (%s, %s): expected = %.10f, actual = %.10f, difference = %.10f",
                    locality, gender, expected, actual, actual - expected));
    }

    /*****************************************************************************************/
    /* Optional registration of deaths by age at death. */

    private DeathAgeRecorder newDeathAgeRecorder(Locality locality, Gender gender)
    {
        return recordDeathsByGenderAgeAtDeath ? new DeathAgeRecorder(locality, gender) : null;
    }

    /*
     * One recorder per invocation of the inner forwarding/births routine.
     * Locality and sex remain fixed, so the buffer needs only one index (age in days).
     * The same distribution algorithm is used in direct and buffered modes.
     */
    private class DeathAgeRecorder
    {
        private final Locality locality;
        private final Gender gender;
        private final double[] buffer;

        DeathAgeRecorder(Locality locality, Gender gender)
        {
            this.locality = locality;
            this.gender = gender;
            buffer = bufferDeathsByGenderAgeAtDeath ? new double[deathsByGenderAgeAtDeath.NDAYS] : null;
        }

        void add(int day, double deaths) throws Exception
        {
            if (deaths == 0)
                return;

            /* Retain deaths beyond the representable age in the last daily age cell. */
            day = Math.min(day, deathsByGenderAgeAtDeath.MAX_DAY);

            if (buffer != null)
                buffer[day] += deaths;
            else
                deathsByGenderAgeAtDeath.addDay(locality, gender, day, deaths);
        }

        /* Merge only once per nonzero daily age, then clear the local buffer. */
        void flush() throws Exception
        {
            if (buffer == null)
                return;

            for (int day = 0; day < buffer.length; day++)
            {
                if (buffer[day] != 0)
                {
                    deathsByGenderAgeAtDeath.addDay(locality, gender, day, buffer[day]);
                    buffer[day] = 0;
                }
            }
        }
    }

    /*
     * Distribute the number of deaths already calculated by the legacy algorithm.
     * For a cohort exposed to mortality from lxFrom to lxTo, the fraction of its
     * deaths in [d, d+1) is (lx[d] - lx[d+1]) / (lx[lxFrom] - lx[lxTo]).
     * This telescoping sum preserves the old death count up to double rounding.
     * Subtract lx values before dividing: avoid cancellation in 1 - lx[d+1]/lx[d].
     *
     * ageFrom is the actual starting age, which may differ from lxFrom when
     * the legacy algorithm substitutes mortality rates of a younger age.
     * For births, both lxFrom and ageFrom are zero.
     */
    private void recordDeathsAtDeathAge(DeathAgeRecorder recorder, double[] day_lx,
            int lxFrom, int lxTo, int ageFrom, double deaths) throws Exception
    {
        if (deaths == 0)
            return;

        /* Legacy survivalRate() defines 0/0 as zero survival. There is no
         * daily distribution implied by lx in this case; assign to the first day. */
        if (day_lx[lxFrom] == 0 && day_lx[lxTo] == 0)
        {
            recorder.add(ageFrom, deaths);
            return;
        }

        double lxLoss = day_lx[lxFrom] - day_lx[lxTo];
        Util.assertion(lxLoss > 0);

        for (int d = lxFrom; d < lxTo; d++)
        {
            double dx = day_lx[d] - day_lx[d + 1];
            Util.assertion(dx >= 0);

            if (dx != 0)
                recorder.add(ageFrom + (d - lxFrom), deaths * (dx / lxLoss));
        }
    }

    /*
     * Mirror the index correction in survivalRate() without changing that method.
     * The caller has already applied any correction made by forward_context().
     * Only the mortality-table indices are shifted, not the actual age at death.
     * Keep this correction in sync with survivalRate() if it is ever changed.
     */
    private void recordForwardDeathsAtDeathAge(DeathAgeRecorder recorder, PopulationContext fctx,
            double[] day_lx, int lxFrom, int lxTo, int ageFrom, double deaths) throws Exception
    {
        if (deaths == 0)
            return;

        if (lxTo >= day_lx.length)
        {
            final int extra = 10;
            final int shift = fctx.DAYS_PER_YEAR + extra;
            lxFrom -= shift;
            lxTo -= shift;
        }

        recordDeathsAtDeathAge(recorder, day_lx, lxFrom, lxTo, ageFrom, deaths);
    }

    /*
     * Legacy yearly population has no daily age structure. Use a uniform
     * distribution within the year of age whose mortality was applied.
     * This is an approximation, not a reconstruction of exact ages at death.
     */
    private void recordYearlyDeathsAtDeathAge(DeathAgeRecorder recorder, int age, double deaths) throws Exception
    {
        if (deaths == 0)
            return;

        int firstDay = age * DAYS_PER_YEAR;
        double dailyDeaths = deaths / DAYS_PER_YEAR;

        for (int d = 0; d < DAYS_PER_YEAR; d++)
            recorder.add(firstDay + d, dailyDeaths);
    }

    /*
     * UR uses the difference between total births and total survivors as its
     * authoritative newborn death count. The sum of individual cohort losses
     * may differ slightly because floating-point additions/subtractions are
     * performed in a different order. Normalize those cohort losses to the
     * existing aggregate before distributing each cohort over days of age.
     * Both arrays are indexed by age at the end of the forwarding interval.
     */
    private void recordNewbornDeathsAtDeathAge(Locality locality, Gender gender,
            double[] day_lx, double[] births, double[] survivors, int ndays,
            double deaths) throws Exception
    {
        if (deaths == 0)
            return;

        double cohortLossSum = 0;
        for (int nd = 0; nd < ndays; nd++)
            cohortLossSum += births[nd] - survivors[nd];

        Util.assertion(cohortLossSum > 0);

        DeathAgeRecorder recorder = newDeathAgeRecorder(locality, gender);
        for (int nd = 0; nd < ndays; nd++)
        {
            double cohortLoss = births[nd] - survivors[nd];
            if (cohortLoss != 0)
                recordDeathsAtDeathAge(recorder, day_lx, 0, nd, 0,
                                       deaths * (cohortLoss / cohortLossSum));
        }
        recorder.flush();
    }

    /*
     * Compare the cumulative age-at-death distribution with the independent
     * legacy death counts. Enable recording before the first forwarding call
     * and leave it enabled throughout the sequence to be validated.
     * PopulationContext derives TOTAL and BOTH by summing its stored cells;
     * only URBAN/RURAL x MALE/FEMALE are written by the recorder.
     */
    public void validateDeathsByGenderAgeAtDeath() throws Exception
    {
        validateDeathsAtDeathAge(Locality.TOTAL, Gender.BOTH, getObservedDeaths());

        validateDeathsAtDeathAge(Locality.TOTAL, Gender.MALE,
                p_u_male_deaths + p_r_male_deaths + fctx_u_male_deaths + fctx_r_male_deaths
                + ur_male_deaths_from_births);
        validateDeathsAtDeathAge(Locality.TOTAL, Gender.FEMALE,
                p_u_female_deaths + p_r_female_deaths + fctx_u_female_deaths + fctx_r_female_deaths
                + ur_female_deaths_from_births);

        double um = p_u_male_deaths + fctx_u_male_deaths
                + newbornDeathsByLocalityGender[Locality.URBAN.ordinal()][Gender.MALE.ordinal()];
        double uf = p_u_female_deaths + fctx_u_female_deaths
                + newbornDeathsByLocalityGender[Locality.URBAN.ordinal()][Gender.FEMALE.ordinal()];
        double rm = p_r_male_deaths + fctx_r_male_deaths
                + newbornDeathsByLocalityGender[Locality.RURAL.ordinal()][Gender.MALE.ordinal()];
        double rf = p_r_female_deaths + fctx_r_female_deaths
                + newbornDeathsByLocalityGender[Locality.RURAL.ordinal()][Gender.FEMALE.ordinal()];

        validateDeathsAtDeathAge(Locality.URBAN, Gender.MALE, um);
        validateDeathsAtDeathAge(Locality.URBAN, Gender.FEMALE, uf);
        validateDeathsAtDeathAge(Locality.RURAL, Gender.MALE, rm);
        validateDeathsAtDeathAge(Locality.RURAL, Gender.FEMALE, rf);
        validateDeathsAtDeathAge(Locality.URBAN, Gender.BOTH, um + uf);
        validateDeathsAtDeathAge(Locality.RURAL, Gender.BOTH, rm + rf);
    }

    /* Compatibility with the spelling used in ForwardPopulationT. */
    public void valdateDeathsByGenderAgeAtDeath() throws Exception
    {
        validateDeathsByGenderAgeAtDeath();
    }

    private void validateDeathsAtDeathAge(Locality locality, Gender gender, double expected) throws Exception
    {
        double actual = deathsByGenderAgeAtDeath.sumAllAges(locality, gender);
        if (!Util.same(expected, actual))
            throw new Exception(String.format(
                    "Deaths mismatch (%s, %s): expected = %.10f, actual = %.10f, difference = %.10f",
                    locality, gender, expected, actual, actual - expected));
    }

    @SuppressWarnings("unused")
    private double deathRate(PopulationContext fctx, double[] day_lx, int nd, int nd2) throws Exception
    {
        return 1 - survivalRate(fctx, day_lx, nd, nd2);
    }

    private double survivalRate(PopulationContext fctx, double[] day_lx, int nd, int nd2) throws Exception
    {
        double rate;

        if (nd2 < day_lx.length)
        {
            if (day_lx[nd2] == 0 && day_lx[nd] == 0)
                rate = 0;
            else
                rate = day_lx[nd2] / day_lx[nd];
        }
        else
        {
            if (nd2 / fctx.DAYS_PER_YEAR < Population.MAX_AGE)
                throw new Exception("unexpected nd2");

            /* использовать коэффициент смертности последнего года */
            final int extra = 10;
            if (day_lx[nd2 - fctx.DAYS_PER_YEAR - extra] == 0 && day_lx[nd - fctx.DAYS_PER_YEAR - extra] == 0)
                rate = 0;
            else
                rate = day_lx[nd2 - fctx.DAYS_PER_YEAR - extra] / day_lx[nd - fctx.DAYS_PER_YEAR - extra];
        }

        return check_range(rate, 0, 1);
    }

    private double check_range(double v, double v1, double v2) throws Exception
    {
        if (Util.isValid(v) && v >= v1 && v <= v2)
            return v;
        else
            throw new Exception("Out of range");
    }

    /* ================================================================================================================== */

    /*
     * Перенести часть населения из сельского в городское для достижения указанного уровня урбанизации.
     * Мы вычисляем требуемое количество передвижения и переносим это население.
     * Перенос прилагается только к возрастным группам 0-49, сельские группы в возрасте 50+ остаются неизменными.
     * В группах 0-49 перенос распределяется равномерно, пропорционально численности этих групп.  
     */
    public PopulationByLocality urbanize(final PopulationByLocality p,
            PopulationContext fctx,
            final Gender gender,
            final double target_urban_level)
            throws Exception
    {
        PopulationByLocality pto = p.clone();

        if (fctx == null)
            fctx = new PopulationContext();

        /*
         * Целевая и текущая численность городского населения во всех возрастах
         */
        double total_population = p.sum(Locality.TOTAL, gender, 0, MAX_AGE) +
                                  fctx.sumAllAges(Locality.TOTAL, gender);

        double target_urban = target_urban_level * total_population;

        double current_urban = p.sum(Locality.URBAN, gender, 0, MAX_AGE) +
                               fctx.sumAllAges(Locality.URBAN, gender);

        /*
         * Численность населения, которое нужно перенести 
         */
        double move = target_urban - current_urban;
        if (move < 0)
            throw new Exception("Negative rural -> urban movement amount");

        /*
         * Мы переносим население из сельского в городское в возрастных группах 0-49.
         * Группы 50+ остаются неизменными.
         * Переносимое население распределяется равномерно между возрастами 0-49 
         * пропорционально их численности в сельском населении.
         */
        double rural049 = p.sum(Locality.RURAL, gender, 0, 49) + fctx.sumAllAges(Locality.RURAL, gender);

        double factor = move / rural049;

        for (int age = 0; age <= 49; age++)
        {
            double r1 = p.get(Locality.RURAL, gender, age);
            move = r1 * factor;
            pto.sub(Locality.RURAL, gender, age, move);
            pto.add(Locality.URBAN, gender, age, move);

            if (fctx != null && age <= fctx.MAX_YEAR)
            {
                for (int nd = fctx.firstDayForAge(age); nd <= fctx.lastDayForAge(age); nd++)
                {
                    double r2 = fctx.getDay(Locality.RURAL, gender, nd);
                    move = r2 * factor;
                    fctx.subDay(Locality.RURAL, gender, nd, move);
                    fctx.addDay(Locality.URBAN, gender, nd, move);
                }
            }
        }

        pto.makeBoth(Locality.RURAL);
        pto.makeBoth(Locality.URBAN);
        pto.resetUnknownForEveryLocality();
        pto.recalcTotalForEveryLocality();

        pto.validate();

        /*
         * Verify that new level (of transformed population) is correct
         */
        if (Util.differ(target_urban_level, urban_fraction(pto, fctx, gender)))
            throw new Exception("Miscalculated urbanization");

        return pto;
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
        sb.append(COLUMN_DIVIDER);
        sb.append("         RURAL       ");
        sb.append("         RURAL        ");
        sb.append("         RURAL        ");
        sb.append(COLUMN_DIVIDER);
        sb.append("         URBAN       ");
        sb.append("         URBAN        ");
        sb.append("         URBAN        ");
        Util.out(sb.toString());

        sb.setLength(0);
        sb.append("  Age  ");
        for (int k = 0; k < 3; k++)
        {
            sb.append(COLUMN_DIVIDER);
            sb.append("         MALE        ");
            sb.append("         FEMALE       ");
            sb.append("     MALE + FEMALE    ");
        }
        Util.out(sb.toString());

        sb.setLength(0);
        sb.append("=======");
        for (int k = 0; k < 3; k++)
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

        sb.append(COLUMN_DIVIDER);
        show_shortfall(sb, pExpected, pActual, Locality.RURAL, Gender.MALE, age1, age2, "");
        show_shortfall(sb, pExpected, pActual, Locality.RURAL, Gender.FEMALE, age1, age2, " ");
        show_shortfall(sb, pExpected, pActual, Locality.RURAL, Gender.BOTH, age1, age2, " ");

        sb.append(COLUMN_DIVIDER);
        show_shortfall(sb, pExpected, pActual, Locality.URBAN, Gender.MALE, age1, age2, "");
        show_shortfall(sb, pExpected, pActual, Locality.URBAN, Gender.FEMALE, age1, age2, " ");
        show_shortfall(sb, pExpected, pActual, Locality.URBAN, Gender.BOTH, age1, age2, " ");

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
