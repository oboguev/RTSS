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
 * Продвижка населения по таблице смертности не имеющей отдельных частей
 * для городского и сельского населения  
 */
public class ForwardPopulationT
{
    protected static final int MAX_AGE = Population.MAX_AGE;

    protected final double MaleFemaleBirthRatio = 1.06;
    protected double BirthRateTotal;
    
    public ForwardPopulationT setBirthRateTotal(double BirthRateTotal)
    {
        this.BirthRateTotal = BirthRateTotal;
        return this;
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
                                        final CombinedMortalityTable mt,
                                        final double yfraction)
            throws Exception
    {
        /* пустая структура для получения результатов */
        PopulationByLocality pto = PopulationByLocality.newPopulationTotalOnly();

        /* продвижка всего населения (городского и сельского совокупно), сохранить результат в @pto */
        forward(pto, p, Locality.TOTAL, mt, yfraction);

        /* проверить внутреннюю согласованность результата */
        pto.validate();

        return pto;
    }

    public void forward(PopulationByLocality pto,
                        final PopulationByLocality p,
                        final Locality locality,
                        final CombinedMortalityTable mt,
                        final double yfraction)
            throws Exception
    {
        /* продвижка мужского и женского населений по смертности из @p в @pto */
        forward(pto, p, locality, Gender.MALE, mt, yfraction);
        forward(pto, p, locality, Gender.FEMALE, mt, yfraction);

        /* добавить рождения */
        double birthRate;
        switch (locality)
        {
        case TOTAL:     birthRate = BirthRateTotal; break;
        default:        throw new Exception("Invalid locality");
        }
        
        double sum = p.sum(locality, Gender.BOTH, 0, MAX_AGE);
        double births = sum * yfraction * birthRate / 1000;
        double m_births = births * MaleFemaleBirthRatio / (1 + MaleFemaleBirthRatio);
        double f_births = births * 1.0 / (1 + MaleFemaleBirthRatio);
        
        pto.add(locality, Gender.MALE, 0, m_births);
        pto.add(locality, Gender.FEMALE, 0, f_births);

        /* вычислить графу "оба пола" из отдельных граф для мужчин и женщин */
        pto.makeBoth(locality);
    }

    public void forward(PopulationByLocality pto,
                        final PopulationByLocality p,
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
}