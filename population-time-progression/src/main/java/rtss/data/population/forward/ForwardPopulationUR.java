package rtss.data.population.forward;

import java.util.HashMap;
import java.util.Map;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.MortalityInfo;
import rtss.data.population.Population;
import rtss.data.population.PopulationByLocality;
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

    public void forward(PopulationByLocality pto,
            final PopulationByLocality p,
            PopulationContext fctx,
            final Locality locality,
            final CombinedMortalityTable mt,
            final double yfraction)
            throws Exception
    {
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
                // TODO: подвергнуь смертности
                throw new Exception("use fctx != null");
            }
        }

        /* вычислить графу "оба пола" из отдельных граф для мужчин и женщин */
        pto.makeBoth(locality);
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
        observed_births += births;
        double m_births = births * MaleFemaleBirthRatio / (1 + MaleFemaleBirthRatio);
        double f_births = births * 1.0 / (1 + MaleFemaleBirthRatio);

        int ndays = (int) Math.round(yfraction * fctx.DAYS_PER_YEAR);
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
        fctx.addTotalBirths(locality, gender, total_births);

        /*
         * распределить рождения по числу дней
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
            fctx.addDay(locality, gender, nd, day_births[nd]);

        double deaths_from_births = total_births - Util.sum(day_births);
        observed_deaths += deaths_from_births;

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

            pto.add(locality, gender, age, staying);
            pto.add(locality, gender, Math.min(MAX_AGE, age + 1), moving - deaths);
        }

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
                v *= survivalRate(fctx, day_lx, nd, nd2);
                p2[nd2] = v;
            }
            else
            {
                int age = fctx.day2age(nd2);
                nd2 = age * fctx.DAYS_PER_YEAR;
                v *= survivalRate(fctx, day_lx, nd, nd2);
                pto.add(locality, gender, Math.min(age, MAX_AGE), v);
            }

            sum_deaths += v_initial - v;
        }

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
    
    private double survivalRate(PopulationContext fctx, double[] day_lx, int nd, int nd2) throws Exception
    {
        if (nd2 < day_lx.length)
        {
            return day_lx[nd2] / day_lx[nd];
        }
        else
        {
            if (nd2 / fctx.DAYS_PER_YEAR  < Population.MAX_AGE)
                throw new Exception("unexpected nd2");
            
            /* использовать коэффициент смертности последнего года */
            int extra = 10;
            return day_lx[nd2 - fctx.DAYS_PER_YEAR - extra] / day_lx[nd - fctx.DAYS_PER_YEAR - extra];
        }
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
