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
 * Продвижка населения по таблице смертности имеющей отдельные части
 * для городского и сельского населения
 */
public class ForwardPopulationUR
{
    protected static final int MAX_AGE = Population.MAX_AGE;
    protected final double MaleFemaleBirthRatio = 1.06;

    protected double BirthRateRural;
    protected double BirthRateUrban;

    /*
     * Оценить долю городского населения во всём населении (для указанного пола) 
     */
    public double urban_fraction(PopulationByLocality p, PopulationForwardingContext fctx, Gender gender) throws Exception
    {
        double urban = p.sum(Locality.URBAN, gender, 0, MAX_AGE);
        double total = p.sum(Locality.TOTAL, gender, 0, MAX_AGE);

        if (fctx != null)
        {
            urban += fctx.sum(Locality.URBAN, gender, 0, fctx.MAX_DAY);
            total += fctx.sum(Locality.TOTAL, gender, 0, fctx.MAX_DAY);
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
        /* пустая структура для получения результатов */
        PopulationByLocality pto = PopulationByLocality.newPopulationByLocality();

        /* продвижка сельского и городского населений, сохранить результат в @pto */
        forward(pto, p, fctx, Locality.RURAL, mt, yfraction);
        forward(pto, p, fctx, Locality.URBAN, mt, yfraction);

        /* вычислить совокупное население обеих зон суммированием городского и сельского */
        pto.recalcTotalLocalityFromUrbanRural();

        /* проверить внутреннюю согласованность результата */
        pto.validate();

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
            add_births(fctx, p, locality, mt, birthRate, yfraction);
        }
        else
        {
            double sum = p.sum(locality, Gender.BOTH, 0, MAX_AGE);
            double births = sum * yfraction * birthRate / 1000;
            double m_births = births * MaleFemaleBirthRatio / (1 + MaleFemaleBirthRatio);
            double f_births = births * 1.0 / (1 + MaleFemaleBirthRatio);

            pto.add(locality, Gender.MALE, 0, m_births);
            pto.add(locality, Gender.FEMALE, 0, f_births);
        }

        /* вычислить графу "оба пола" из отдельных граф для мужчин и женщин */
        pto.makeBoth(locality);
    }

    private void add_births(PopulationForwardingContext fctx,
            final PopulationByLocality p,
            final Locality locality,
            final CombinedMortalityTable mt,
            final double birthRate,
            final double yfraction) throws Exception
    {
        double sum = p.sum(locality, Gender.BOTH, 0, MAX_AGE) + fctx.sumAges(locality, Gender.BOTH, 0, fctx.MAX_YEAR);
        double births = sum  * yfraction * birthRate / 1000;
        double m_births = births * MaleFemaleBirthRatio / (1 + MaleFemaleBirthRatio);
        double f_births = births * 1.0 / (1 + MaleFemaleBirthRatio);

        int ndays = (int) Math.round(yfraction * fctx.DAYS_PER_YEAR);
        ndays = Math.max(1,  ndays);

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
        /*
         * распределить рождения по числу дней
         */
        double[] day_births = new double[ndays];
        for (int nd = 0; nd < ndays; nd++)
            day_births[nd] = total_births / ndays;
        
        /*
         * подвергнуть рождения смертности
         * lx[nd] содержит число выживших на день жизни nd согласно таблице смертности 
         */
        double[] day_lx = fctx.get_daily_lx(mt, locality, gender);
        for (int nd = 0; nd < ndays; nd++)
            day_births[nd] *= day_lx[nd] / day_lx[0];
        
        /*
         * добавить результат в контекст
         */
        for (int nd = 0; nd < ndays; nd++)
            fctx.add(locality, gender, nd, day_births[nd]);
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

            pto.add(locality, gender, age, staying);
            pto.add(locality, gender, Math.min(MAX_AGE, age + 1), moving - deaths);
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
        
        for (int nd = 0; nd < p.length; nd++)
        {
            int nd2 = nd + ndays;
            
            double v = p[nd];

            if (nd2 < p2.length)
            {
                v *= day_lx[nd2] / day_lx[nd];
                p2[nd2] = v;
            }
            else
            {
                int age = fctx.day2age(nd2);
                nd2 = age * fctx.DAYS_PER_YEAR;
                v *= day_lx[nd2] / day_lx[nd];
                pto.add(locality, gender, age, v);
            }
        }
        
        fctx.fromArray(locality, gender, p2);
    }
    
    /*
     * Перенести часть населения из сельского в городское для достижения указанного уровня урбанизации.
     * Мы вычисляем требуемое количество передвижения и переносим это население.
     * Перенос прилагается только к возрастным группам 0-49, сельские группы в возрасте 50+ остаются неизменными.
     * В группах 0-49 перенос распределяется равномерно, пропорционально численности этих групп.  
     */
    public PopulationByLocality urbanize(final PopulationByLocality p,
            PopulationForwardingContext fctx,
            final Gender gender,
            final double target_urban_level)
            throws Exception
    {
        PopulationByLocality pto = p.clone();

        if (fctx == null)
            fctx = new PopulationForwardingContext();

        /*
         * Целевая и текущая численность городского населения во всех возрастах
         */
        double total_population = p.sum(Locality.TOTAL, gender, 0, MAX_AGE) +
                                  fctx.sumAges(Locality.TOTAL, gender, 0, fctx.MAX_YEAR);

        double target_urban = target_urban_level * total_population;

        double current_urban = p.sum(Locality.URBAN, gender, 0, MAX_AGE) +
                               fctx.sumAges(Locality.URBAN, gender, 0, fctx.MAX_YEAR);

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
        double rural049 = p.sum(Locality.RURAL, gender, 0, 49) +
                          fctx.sumAges(Locality.RURAL, gender, 0, fctx.MAX_YEAR);

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
                    double r2 = fctx.get(Locality.RURAL, gender, nd);
                    move = r2 * factor;
                    fctx.sub(Locality.RURAL, gender, nd, move);
                    fctx.add(Locality.URBAN, gender, nd, move);
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
}
