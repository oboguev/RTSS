package my;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import data.mortality.CombinedMortalityTable;
import data.mortality.MortalityInfo;
import data.population.Population;
import data.population.PopulationByLocality;
import data.selectors.Gender;
import data.selectors.Locality;

public class Forward_1926_1937
{
    private static final int MAX_AGE = Population.MAX_AGE;

    private CombinedMortalityTable mt1926 = new CombinedMortalityTable("mortality_tables/USSR/1926-1927");
    private CombinedMortalityTable mt1938 = new CombinedMortalityTable("mortality_tables/USSR/1938-1939");
    private PopulationByLocality p1926 = PopulationByLocality.load("population_data/USSR/1926");
    private PopulationByLocality p1937_original = PopulationByLocality.load("population_data/USSR/1937");
    private PopulationByLocality p1937 = new Adjust_1937().adjust(p1937_original);

    private Map<Integer, Double> urban_male_fraction_yyyy;
    private Map<Integer, Double> urban_female_fraction_yyyy;

    private double BirthRateRural;
    private double BirthRateUrban;
    private final double MaleFemaleBirthRatio = 1.06;

    public Forward_1926_1937() throws Exception
    {
    }

    public void forward(boolean interpolateMortalityTable) throws Exception
    {
        /*
         * Вычислить рождаемость городского и сельского населения в 1926 году 
         */
        calcBirthRates();
        
        /*
         * Вычислить оценку доли городского населения для каждого года между 1926 и 1936
         * посредством интерполяции между переписями декабря 1926 и января 1937 гг. 
         * 
         * перепись 1926 года была 1926-12-17
         * перепись 1937 года была 1937-01-06, почти в конце 1936 
         */
        double urban_female_fraction_1926 = urban_fraction(p1926, Gender.FEMALE);
        double urban_female_fraction_1936 = urban_fraction(p1937, Gender.FEMALE);

        double urban_male_fraction_1926 = urban_fraction(p1926, Gender.MALE);
        double urban_male_fraction_1936 = urban_fraction(p1937, Gender.MALE);

        urban_male_fraction_yyyy = interpolate_linear(1926, urban_male_fraction_1926, 1936,
                                                      urban_male_fraction_1936);
        urban_female_fraction_yyyy = interpolate_linear(1926, urban_female_fraction_1926, 1936,
                                                        urban_female_fraction_1936);

        /*
         * Продвижка населения для целых лет с декабря 1926 по декабрь 1936 
         */
        CombinedMortalityTable mt = mt1926; 
        PopulationByLocality p = p1926;
        int year = 1926;
        double yfraction = 1.0;
        for (;;)
        {
            year++;
            
            if (interpolateMortalityTable)
                mt = interpolateMortalityTable(year);
            
            p = forward(p, mt, yfraction);

            /*
             * Перераспределить население между городским и сельским, отражая урбанизацию 
             */
            p = urbanize(p, Gender.MALE, urban_male_fraction_yyyy.get(year));
            p = urbanize(p, Gender.FEMALE, urban_female_fraction_yyyy.get(year));

            if (year == 1936)
                break;
        }

        /*
         * Продвижка населения для части года (с 17 декабря 1926 по 6 января 1937)
         */
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Date d1936 = df.parse("1936-12-17");
        Date d1937 = df.parse("1937-01-06");
        long ndays = Duration.between(d1936.toInstant(), d1937.toInstant()).toDays();
        yfraction = ndays / 365.0;
        p = forward(p, mt, yfraction);

        show_results(p);
    }

    /*****************************************************************************************/

    /*
     * Оценить долю городского населения во всём населении  
     */
    private double urban_fraction(PopulationByLocality p, Gender gender) throws Exception
    {
        double total = p.sum(Locality.TOTAL, gender, 0, MAX_AGE);
        double urban = p.sum(Locality.URBAN, gender, 0, MAX_AGE);
        return urban / total;
    }

    /*
     * Линейная интерполяция между двумя точками
     */
    private Map<Integer, Double> interpolate_linear(int y1, double v1, int y2, double v2)
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
     * При продвижке на целый год, @yfraction = 1.0.
     * При продвижке на часть года, @yfraction < 1.0.
     */
    public PopulationByLocality forward(final PopulationByLocality p,
                                        final CombinedMortalityTable mt,
                                        final double yfraction)
            throws Exception
    {
        /* пустая структура для получения результатов */
        PopulationByLocality pto = PopulationByLocality.newPopulationByLocality();

        /* продвижка седльского и городского населений, сохранить результат в @pto */
        forward(pto, p, Locality.RURAL, mt, yfraction);
        forward(pto, p, Locality.URBAN, mt, yfraction);

        /* вычислить совокупное население обеих зон суммированием городского и сельского */
        pto.recalcTotal();

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
        case RURAL:     birthRate = BirthRateRural; break;
        case URBAN:     birthRate = BirthRateUrban; break;
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

    /*
     * Перенести часть населения из сельского в городское для достижения указанного уровня урбанизации.
     * Мы вычисляем требуемое количество передвижения и переносим это население.
     * Перенос прилагается только к возрастным группам 0-49, сельские группы в возрасте 50+ остаются неизменными.
     * В группах 0-49 перенос распределяется равномерно, пропорционально численности этих групп.  
     */
    public PopulationByLocality urbanize(final PopulationByLocality p,
                                         final Gender gender,
                                         final double target_urban_level)
            throws Exception
    {
        PopulationByLocality pto = p.clone();

        /*
         * Целевая и текущая численность городского населения во всех возрастах
         */
        double target_urban = target_urban_level * p.sum(Locality.TOTAL, gender, 0, MAX_AGE);
        double current_urban = p.sum(Locality.URBAN, gender, 0, MAX_AGE);

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
        double rural049 = p.sum(Locality.RURAL, gender, 0, 49);
        double factor = move / rural049;
        for (int age = 0; age <= 49; age++)
        {
            double r = p.get(Locality.RURAL, gender, age);
            double u = p.get(Locality.URBAN, gender, age);
            move = r * factor;
            pto.set(Locality.RURAL, gender, age, r - move);
            pto.set(Locality.URBAN, gender, age, u + move);
        }

        pto.makeBoth(Locality.RURAL);
        pto.makeBoth(Locality.URBAN);
        pto.resetUnknown();
        pto.resetTotal();

        pto.validate();

        /*
         * Verify that new level (of transformed population) is correct
         */
        if (differ(target_urban_level, urban_fraction(pto, gender)))
            throw new Exception("Miscalculated urbanization");

        return pto;
    }

    /*****************************************************************************************/

    private boolean differ(double a, double b)
    {
        return differ(a, b, 0.00001);
    }

    private boolean differ(double a, double b, double diff)
    {
        return Math.abs(a - b) / Math.max(Math.abs(a), Math.abs(b)) > diff;
    }

    /*****************************************************************************************/

    private void show_results(PopulationByLocality p) throws Exception
    {
        /*
         * Распечатать суммарные итоги
         */
        Util.out(String
                .format("Population expected to survive from the end of 1926 till January 1937 and be of age 10+ in January 1937: %,d ",
                        Math.round(p.sum(Locality.TOTAL, Gender.BOTH, 10, MAX_AGE))));

        Util.out(String.format("Actual January 1937 population ages 10 years and older: %,d",
                               Math.round(p1937.sum(Locality.TOTAL, Gender.BOTH, 10, MAX_AGE))));
        Util.out("");
        Util.out(String.format("Actual January 1937 population all ages: %,d",
                               Math.round(p1937.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE))));

        Util.out("");
        Util.out(String.format("Expected population ages 0-9 in January 1937: %,d",
                               Math.round(p.sum(Locality.TOTAL, Gender.BOTH, 0, 9))));

        Util.out(String.format("Actual population ages 0-9 in January 1937: %,d",
                               Math.round(p1937.sum(Locality.TOTAL, Gender.BOTH, 0, 9))));

        /*
         * Display overall shortall
         */
        String divider = "*************************************************************************************";
        Util.out("");
        Util.out(divider);
        Util.out("");
        Util.out("Overall population shortfall:  (%%-age is relative to expected population)");
        Util.out("");
        show_shortfall_header();
        show_shortfall(p, 0, MAX_AGE);
        show_shortfall(p, 0, 9);
        show_shortfall(p, 10, MAX_AGE);

        /*
         * Display shortall by age groups
         */
        Util.out("");
        Util.out(divider);
        Util.out("");
        Util.out("Population shortfall by age groups:");
        Util.out("");
        show_shortfall_header();
        for (int age = 0; age + 5 <= MAX_AGE; age += 5)
        {
            show_shortfall(p, age, age + 4);
        }
        show_shortfall(p, MAX_AGE, MAX_AGE);
    }

    private void show_shortfall_header()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("      ");
        sb.append("          TOTAL       ");
        sb.append("         TOTAL        ");
        sb.append("          RURAL       ");
        sb.append("         RURAL        ");
        sb.append("          URBAN       ");
        sb.append("         URBAN        ");
        Util.out(sb.toString());

        sb.setLength(0);
        sb.append("  Age ");
        for (int k = 0; k < 3; k++)
        {
            sb.append("          MALE        ");
            sb.append("         FEMALE       ");
        }
        Util.out(sb.toString());

        sb.setLength(0);
        sb.append("=======");
        for (int k = 0; k < 6; k++)
            sb.append("  ====================");
        Util.out(sb.toString());
    }

    private void show_shortfall(PopulationByLocality p, int age1, int age2) throws Exception
    {
        String s = String.format("%d-%d", age1, age2);
        if (age1 == age2 & age1 == MAX_AGE)
            s = String.format("%d+", age1);
        s = String.format("%6s", s);
        StringBuilder sb = new StringBuilder(s);

        show_shortfall(sb, p, Locality.TOTAL, Gender.MALE, age1, age2);
        show_shortfall(sb, p, Locality.TOTAL, Gender.FEMALE, age1, age2);
        show_shortfall(sb, p, Locality.RURAL, Gender.MALE, age1, age2);
        show_shortfall(sb, p, Locality.RURAL, Gender.FEMALE, age1, age2);
        show_shortfall(sb, p, Locality.URBAN, Gender.MALE, age1, age2);
        show_shortfall(sb, p, Locality.URBAN, Gender.FEMALE, age1, age2);

        Util.out(sb.toString());
    }

    private void show_shortfall(StringBuilder sb,
                                PopulationByLocality p,
                                Locality locality,
                                Gender gender,
                                int age1, int age2)
            throws Exception
    {
        double expected = p.sum(locality, gender, age1, age2);
        double actual = p1937.sum(locality, gender, age1, age2);
        double deficit = expected - actual;
        double deficit_pct = 100 * deficit / expected;
        String s = String.format("%,10d (%7.2f%%)", Math.round(deficit), deficit_pct);
        sb.append(" ");
        sb.append(s);
    }

    /*****************************************************************************************/
    
    private void calcBirthRates() throws Exception
    {
        /*
         * ЦСУ СССР, "Естественное движение населения Союза ССР в 1926 г.", т. 1, вып. 2, М. 1929 (стр. 39):
         * рождаемость во всём СССР = 44.0
         * в сельских местностях СССР = 46.1
         */
        final double BirthRateTotal = 44.0;
        BirthRateRural = 46.1;
        final double ruralPopulation = p1926.sum(Locality.RURAL, Gender.BOTH, 0, MAX_AGE);
        final double urbanPopulation = p1926.sum(Locality.URBAN, Gender.BOTH, 0, MAX_AGE);
        BirthRateUrban = (BirthRateTotal * (ruralPopulation + urbanPopulation) - BirthRateRural * ruralPopulation) / urbanPopulation;
        
        /*
         * Результат вычисления: 
         *    городское = 34.4   сельское = 46.1
         *    
         * ЦСУ СССР, "Статистический справочник СССР за 1928", М. 1929 (стр. 76-77) приводит для Европейской части СССР на 1927 год   
         *    городское = 32.1   сельское = 45.5
         */
    }
    
    private CombinedMortalityTable interpolateMortalityTable(int year) throws Exception
    {
        double weight = ((double)year - 1926) / (1938 - 1926);
        return CombinedMortalityTable.interpolate(mt1926, mt1938, weight);
    }
}
